package io.github.lemostic.toolsuite.modules.devops.jenkinsdownloader.service;

import io.github.lemostic.toolsuite.modules.devops.jenkinsdownloader.model.FileInfo;
import io.github.lemostic.toolsuite.modules.devops.jenkinsdownloader.model.JenkinsConfig;
import io.github.lemostic.toolsuite.modules.devops.jenkinsdownloader.model.MatchType;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Jenkins文件下载服务
 * 处理Jenkins API调用、文件列表获取和下载功能
 */
public class JenkinsDownloadService {
    
    private static final Logger logger = LoggerFactory.getLogger(JenkinsDownloadService.class);
    
    private final DoubleProperty progress = new SimpleDoubleProperty(0);
    private final StringProperty statusMessage = new SimpleStringProperty("就绪");
    
    // 连接超时（毫秒）
    private static final int CONNECT_TIMEOUT = 30000;
    private static final int READ_TIMEOUT = 60000;
    
    public DoubleProperty progressProperty() {
        return progress;
    }
    
    public StringProperty statusMessageProperty() {
        return statusMessage;
    }
    
    /**
     * 解析Jenkins工作区URL
     * 从用户输入的URL中提取必要信息
     */
    public ParsedUrl parseWorkspaceUrl(String url) {
        ParsedUrl result = new ParsedUrl();
        
        try {
            // 移除末尾的斜杠
            url = url.trim();
            while (url.endsWith("/")) {
                url = url.substring(0, url.length() - 1);
            }
            
            // 检查是否是Jenkins工作区URL
            // 格式: http://jenkins/job/jobname/ws/path 或 http://jenkins/job/jobname/ws/
            if (url.contains("/job/") && url.contains("/ws")) {
                int jobIndex = url.indexOf("/job/");
                int wsIndex = url.indexOf("/ws");
                
                result.setBaseUrl(url.substring(0, jobIndex));
                result.setJobPath(url.substring(jobIndex + 1, wsIndex)); // job/jobname
                
                if (wsIndex + 3 < url.length()) {
                    result.setWorkspacePath(url.substring(wsIndex + 3)); // 去掉/ws后的路径
                } else {
                    result.setWorkspacePath("");
                }
                
                result.setValid(true);
            } else {
                result.setValid(false);
                result.setErrorMessage("URL格式不正确，请确保是Jenkins工作区链接（包含/job/和/ws）");
            }
        } catch (Exception e) {
            result.setValid(false);
            result.setErrorMessage("解析URL失败: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 获取Jenkins工作区文件列表
     * 使用Jenkins API获取目录内容
     */
    public List<FileInfo> fetchFileList(JenkinsConfig config, String workspaceUrl) throws Exception {
        List<FileInfo> files = new ArrayList<>();
        
        ParsedUrl parsed = parseWorkspaceUrl(workspaceUrl);
        if (!parsed.isValid()) {
            throw new IllegalArgumentException(parsed.getErrorMessage());
        }
        
        // 构建API URL - 使用Jenkins的api/json端点
        String apiUrl = buildApiUrl(config, parsed);
        
        updateStatus("正在连接Jenkins服务器...", 0.1);
        
        HttpURLConnection conn = null;
        try {
            URL url = new URL(apiUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(CONNECT_TIMEOUT);
            conn.setReadTimeout(READ_TIMEOUT);
            
            // 设置认证头
            String authHeader = config.getAuthHeader();
            if (authHeader != null) {
                conn.setRequestProperty("Authorization", authHeader);
            }
            
            conn.setRequestProperty("Accept", "application/json");
            
            int responseCode = conn.getResponseCode();
            
            if (responseCode == 401 || responseCode == 403) {
                throw new SecurityException("认证失败，请检查用户名和API Token是否正确");
            }
            
            if (responseCode == 404) {
                throw new FileNotFoundException("工作区路径不存在，请检查URL是否正确");
            }
            
            if (responseCode != 200) {
                throw new IOException("服务器返回错误: HTTP " + responseCode);
            }
            
            updateStatus("正在解析文件列表...", 0.3);
            
            // 读取响应
            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }
            
            // 解析JSON响应
            files = parseFileListFromJson(response.toString(), config, parsed);
            
            updateStatus(String.format("找到 %d 个文件", files.size()), 1.0);
            
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        
        return files;
    }
    
    /**
     * 构建API URL
     */
    private String buildApiUrl(JenkinsConfig config, ParsedUrl parsed) {
        StringBuilder sb = new StringBuilder();
        sb.append(config.getNormalizedBaseUrl());
        sb.append("/").append(parsed.getJobPath());
        sb.append("/ws");
        
        if (!parsed.getWorkspacePath().isEmpty()) {
            sb.append(parsed.getWorkspacePath());
        }
        
        // 添加api/json获取目录列表
        sb.append("/api/json?depth=1");
        
        return sb.toString();
    }
    
    /**
     * 从JSON响应中解析文件列表
     * 使用简单的字符串解析来处理Jenkins API响应
     */
    private List<FileInfo> parseFileListFromJson(String json, JenkinsConfig config, ParsedUrl parsed) {
        List<FileInfo> files = new ArrayList<>();
        
        try {
            // 查找 "fileName" 字段
            Pattern fileNamePattern = Pattern.compile("\"fileName\"\\s*:\\s*\"([^\"]+)\"");
            java.util.regex.Matcher matcher = fileNamePattern.matcher(json);
            
            while (matcher.find()) {
                String fileName = matcher.group(1);
                
                // 跳过目录（简单判断：没有扩展名的可能是目录）
                if (!fileName.contains(".")) {
                    continue;
                }
                
                // 构建文件URL
                String fileUrl = buildFileUrl(config, parsed, fileName);
                
                FileInfo fileInfo = new FileInfo();
                fileInfo.setFileName(fileName);
                fileInfo.setRelativePath(parsed.getWorkspacePath() + "/" + fileName);
                fileInfo.setDownloadUrl(fileUrl);
                fileInfo.setFileSize("未知");
                fileInfo.setSizeInBytes(0);
                fileInfo.setSelected(true);
                
                files.add(fileInfo);
            }
            
            // 如果没有找到文件，尝试备用方法：直接构造文件列表
            if (files.isEmpty()) {
                logger.info("API未返回文件列表，尝试备用方法");
            }
            
        } catch (Exception e) {
            logger.error("解析文件列表失败", e);
        }
        
        return files;
    }
    
    /**
     * 构建单个文件的下载URL
     */
    private String buildFileUrl(JenkinsConfig config, ParsedUrl parsed, String fileName) {
        StringBuilder sb = new StringBuilder();
        sb.append(config.getNormalizedBaseUrl());
        sb.append("/").append(parsed.getJobPath());
        sb.append("/ws");
        
        if (!parsed.getWorkspacePath().isEmpty()) {
            sb.append(parsed.getWorkspacePath());
        }
        sb.append("/");
        
        // URL编码文件名
        try {
            sb.append(URLEncoder.encode(fileName, StandardCharsets.UTF_8.toString()).replace("+", "%20"));
        } catch (Exception e) {
            sb.append(fileName);
        }
        
        return sb.toString();
    }
    
    /**
     * 根据匹配规则过滤文件
     */
    public List<FileInfo> filterFiles(List<FileInfo> files, String pattern, MatchType matchType) {
        List<FileInfo> filtered = new ArrayList<>();
        
        if (pattern == null || pattern.trim().isEmpty()) {
            return files;
        }
        
        String trimmedPattern = pattern.trim();
        Pattern regex = null;
        
        // 预编译正则表达式（如果需要）
        if (matchType == MatchType.REGEX) {
            try {
                regex = Pattern.compile(trimmedPattern);
            } catch (PatternSyntaxException e) {
                logger.error("正则表达式语法错误: {}", trimmedPattern);
                return files;
            }
        }
        
        for (FileInfo file : files) {
            String fileName = file.getFileName();
            boolean matches = false;
            
            switch (matchType) {
                case PREFIX:
                    matches = fileName.startsWith(trimmedPattern);
                    break;
                case SUFFIX:
                    matches = fileName.endsWith(trimmedPattern);
                    break;
                case CONTAINS:
                    matches = fileName.contains(trimmedPattern);
                    break;
                case REGEX:
                    matches = regex.matcher(fileName).matches();
                    break;
                case EXACT:
                    matches = fileName.equals(trimmedPattern);
                    break;
            }
            
            if (matches) {
                filtered.add(file);
            }
        }
        
        return filtered;
    }
    
    /**
     * 下载单个文件
     */
    public boolean downloadFile(FileInfo fileInfo, JenkinsConfig config, Path targetDir) {
        HttpURLConnection conn = null;
        
        try {
            Platform.runLater(() -> fileInfo.setStatus("下载中..."));
            
            URL url = new URL(fileInfo.getDownloadUrl());
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(CONNECT_TIMEOUT);
            conn.setReadTimeout(READ_TIMEOUT);
            
            String authHeader = config.getAuthHeader();
            if (authHeader != null) {
                conn.setRequestProperty("Authorization", authHeader);
            }
            
            int responseCode = conn.getResponseCode();
            
            if (responseCode != 200) {
                Platform.runLater(() -> fileInfo.setStatus("失败: HTTP " + responseCode));
                return false;
            }
            
            // 获取文件大小
            long fileSize = conn.getContentLengthLong();
            
            // 创建目标文件路径
            Path targetFile = targetDir.resolve(fileInfo.getFileName());
            
            // 下载文件
            try (InputStream in = conn.getInputStream()) {
                Files.copy(in, targetFile, StandardCopyOption.REPLACE_EXISTING);
            }
            
            // 验证文件大小
            long downloadedSize = Files.size(targetFile);
            if (fileSize > 0 && downloadedSize != fileSize) {
                Platform.runLater(() -> fileInfo.setStatus("失败: 文件不完整"));
                return false;
            }
            
            Platform.runLater(() -> fileInfo.setStatus("完成"));
            return true;
            
        } catch (Exception e) {
            logger.error("下载文件失败: {}", fileInfo.getFileName(), e);
            Platform.runLater(() -> fileInfo.setStatus("失败: " + e.getMessage()));
            return false;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
    
    /**
     * 批量下载文件
     */
    public DownloadResult downloadFiles(List<FileInfo> files, JenkinsConfig config, Path targetDir) {
        DownloadResult result = new DownloadResult();
        
        int total = files.size();
        int success = 0;
        int failed = 0;
        
        // 确保目标目录存在
        try {
            Files.createDirectories(targetDir);
        } catch (IOException e) {
            result.setSuccess(false);
            result.setMessage("无法创建目标目录: " + e.getMessage());
            return result;
        }
        
        for (int i = 0; i < files.size(); i++) {
            FileInfo file = files.get(i);
            
            final int progress = i + 1;
            updateStatus(String.format("正在下载 (%d/%d): %s", progress, total, file.getFileName()), 
                        (double) progress / total);
            
            if (downloadFile(file, config, targetDir)) {
                success++;
            } else {
                failed++;
            }
        }
        
        result.setSuccess(failed == 0);
        result.setTotal(total);
        result.setSuccessCount(success);
        result.setFailedCount(failed);
        result.setMessage(String.format("下载完成: 成功 %d, 失败 %d", success, failed));
        
        updateStatus(result.getMessage(), 1.0);
        
        return result;
    }
    
    private void updateStatus(String message, double progress) {
        Platform.runLater(() -> {
            this.statusMessage.set(message);
            this.progress.set(progress);
        });
        logger.info("{} - {}", message, String.format("%.0f%%", progress * 100));
    }
    
    /**
     * 解析后的URL信息
     */
    public static class ParsedUrl {
        private String baseUrl;
        private String jobPath;
        private String workspacePath;
        private boolean valid;
        private String errorMessage;
        
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        
        public String getJobPath() { return jobPath; }
        public void setJobPath(String jobPath) { this.jobPath = jobPath; }
        
        public String getWorkspacePath() { return workspacePath; }
        public void setWorkspacePath(String workspacePath) { this.workspacePath = workspacePath; }
        
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }
    
    /**
     * 下载结果
     */
    public static class DownloadResult {
        private boolean success;
        private int total;
        private int successCount;
        private int failedCount;
        private String message;
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public int getTotal() { return total; }
        public void setTotal(int total) { this.total = total; }
        
        public int getSuccessCount() { return successCount; }
        public void setSuccessCount(int successCount) { this.successCount = successCount; }
        
        public int getFailedCount() { return failedCount; }
        public void setFailedCount(int failedCount) { this.failedCount = failedCount; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}
