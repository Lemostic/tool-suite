package io.github.lemostic.toolsuite.modules.devops.logviewer.service;

import com.jcraft.jsch.*;
import io.github.lemostic.toolsuite.modules.devops.logviewer.model.LogDirectory;
import io.github.lemostic.toolsuite.modules.devops.logviewer.model.LogFile;
import io.github.lemostic.toolsuite.modules.devops.logviewer.model.ServerConfig;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

/**
 * 日志查看器服务类
 * 负责 SSH 连接和日志文件读取
 */
@Slf4j
public class LogViewerService {
    
    private Session currentSession;
    private ServerConfig currentServer;
    
    private final StringProperty statusMessage = new SimpleStringProperty("就绪");
    private final BooleanProperty connected = new SimpleBooleanProperty(false);
    
    public StringProperty statusMessageProperty() {
        return statusMessage;
    }
    
    public BooleanProperty connectedProperty() {
        return connected;
    }
    
    /**
     * 连接到服务器
     */
    public CompletableFuture<Boolean> connectAsync(ServerConfig serverConfig) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Platform.runLater(() -> statusMessage.set("正在连接 " + serverConfig.getHost() + "..."));
                
                // 断开现有连接
                disconnect();
                
                JSch jsch = new JSch();
                
                // 如果使用私钥认证
                if (!serverConfig.isUsePasswordAuth() && serverConfig.getPrivateKeyPath() != null) {
                    if (serverConfig.getPrivateKeyPassphrase() != null && !serverConfig.getPrivateKeyPassphrase().isEmpty()) {
                        jsch.addIdentity(serverConfig.getPrivateKeyPath(), serverConfig.getPrivateKeyPassphrase());
                    } else {
                        jsch.addIdentity(serverConfig.getPrivateKeyPath());
                    }
                }
                
                Session session = jsch.getSession(
                    serverConfig.getUsername(),
                    serverConfig.getHost(),
                    serverConfig.getPort()
                );
                
                // 配置连接属性
                Properties config = new Properties();
                config.put("StrictHostKeyChecking", "no");
                config.put("UserKnownHostsFile", "/dev/null");
                session.setConfig(config);
                
                // 设置密码
                if (serverConfig.isUsePasswordAuth()) {
                    session.setPassword(serverConfig.getPassword());
                }
                
                // 连接
                session.connect(30000); // 30秒超时
                
                currentSession = session;
                currentServer = serverConfig;
                
                Platform.runLater(() -> {
                    connected.set(true);
                    statusMessage.set("已连接到 " + serverConfig.getDisplayName());
                });
                
                logger.info("成功连接到服务器: {}", serverConfig.getHost());
                return true;
                
            } catch (Exception e) {
                logger.error("连接服务器失败: {}", e.getMessage(), e);
                Platform.runLater(() -> {
                    connected.set(false);
                    statusMessage.set("连接失败: " + e.getMessage());
                });
                throw new RuntimeException("连接失败: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * 断开连接
     */
    public void disconnect() {
        if (currentSession != null && currentSession.isConnected()) {
            currentSession.disconnect();
            logger.info("已断开与服务器 {} 的连接", currentServer != null ? currentServer.getHost() : "unknown");
        }
        currentSession = null;
        currentServer = null;
        connected.set(false);
        statusMessage.set("已断开连接");
    }
    
    /**
     * 获取日志目录下的文件列表
     * 支持多种文件匹配模式，以逗号分隔
     */
    public CompletableFuture<List<LogFile>> listLogFilesAsync(LogDirectory logDirectory) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (currentSession == null || !currentSession.isConnected()) {
                    throw new RuntimeException("未连接到服务器");
                }
                
                Platform.runLater(() -> statusMessage.set("正在读取目录: " + logDirectory.getPath()));
                
                // 解析文件匹配模式（支持逗号分隔的多种模式）
                String filePattern = logDirectory.getFilePattern();
                String[] patterns = filePattern.split(",");
                
                List<LogFile> allFiles = new ArrayList<>();
                
                for (String pattern : patterns) {
                    pattern = pattern.trim();
                    if (pattern.isEmpty()) continue;
                    
                    Channel channel = currentSession.openChannel("exec");
                    String command = String.format(
                        "find %s -maxdepth 1 -type f -name '%s' -printf '%%T@|%%s|%%p\\n'",
                        escapePath(logDirectory.getPath()),
                        escapePath(pattern)
                    );
                    ((ChannelExec) channel).setCommand(command);
                    
                    channel.setInputStream(null);
                    ((ChannelExec) channel).setErrStream(System.err);
                    
                    InputStream in = channel.getInputStream();
                    channel.connect();
                    
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                    String line;
                    
                    while ((line = reader.readLine()) != null) {
                        LogFile logFile = parseLogFileLine(line);
                        if (logFile != null && !containsFile(allFiles, logFile.getFullPath())) {
                            allFiles.add(logFile);
                        }
                    }
                    
                    channel.disconnect();
                }
                
                // 按文件名排序
                allFiles.sort((f1, f2) -> f1.getName().compareTo(f2.getName()));
                
                Platform.runLater(() -> statusMessage.set("找到 " + allFiles.size() + " 个日志文件"));
                return allFiles;
                
            } catch (Exception e) {
                logger.error("读取日志文件列表失败: {}", e.getMessage(), e);
                Platform.runLater(() -> statusMessage.set("读取失败: " + e.getMessage()));
                throw new RuntimeException("读取日志文件列表失败: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * 检查文件列表中是否已包含指定路径的文件
     */
    private boolean containsFile(List<LogFile> files, String fullPath) {
        return files.stream().anyMatch(f -> f.getFullPath().equals(fullPath));
    }
    
    /**
     * 读取日志文件内容
     * @param logFile 日志文件
     * @param lines 读取行数（正数表示从头开始，负数表示从尾部开始）
     */
    public CompletableFuture<String> readLogFileAsync(LogFile logFile, int lines) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (currentSession == null || !currentSession.isConnected()) {
                    throw new RuntimeException("未连接到服务器");
                }
                
                Platform.runLater(() -> statusMessage.set("正在读取: " + logFile.getName()));
                
                Channel channel = currentSession.openChannel("exec");
                String command;
                
                if (lines > 0) {
                    // 从头读取指定行数
                    command = String.format("head -n %d %s", lines, escapePath(logFile.getFullPath()));
                } else {
                    // 从尾部读取指定行数
                    command = String.format("tail -n %d %s", Math.abs(lines), escapePath(logFile.getFullPath()));
                }
                
                ((ChannelExec) channel).setCommand(command);
                channel.setInputStream(null);
                ((ChannelExec) channel).setErrStream(System.err);
                
                InputStream in = channel.getInputStream();
                channel.connect();
                
                StringBuilder content = new StringBuilder();
                BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                String line;
                
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
                
                channel.disconnect();
                
                Platform.runLater(() -> statusMessage.set("已读取 " + logFile.getName()));
                return content.toString();
                
            } catch (Exception e) {
                logger.error("读取日志文件失败: {}", e.getMessage(), e);
                Platform.runLater(() -> statusMessage.set("读取失败: " + e.getMessage()));
                throw new RuntimeException("读取日志文件失败: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * 解析文件列表输出行
     * 格式: 修改时间戳|文件大小|文件路径
     */
    private LogFile parseLogFileLine(String line) {
        try {
            String[] parts = line.split("\\|", 3);
            if (parts.length < 3) {
                return null;
            }
            
            long timestamp = (long) Double.parseDouble(parts[0]);
            long size = Long.parseLong(parts[1]);
            String path = parts[2];
            
            // 提取文件名
            String name = path.substring(path.lastIndexOf('/') + 1);
            
            LocalDateTime lastModified = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochSecond(timestamp),
                ZoneId.systemDefault()
            );
            
            return LogFile.builder()
                .name(name)
                .fullPath(path)
                .size(size)
                .lastModified(lastModified)
                .directory(false)
                .build();
                
        } catch (Exception e) {
            logger.warn("解析文件行失败: {}", line, e);
            return null;
        }
    }
    
    /**
     * 转义路径中的特殊字符
     */
    private String escapePath(String path) {
        return path.replace("'", "'\\''");
    }
    
    /**
     * 获取当前连接的服务器
     */
    public ServerConfig getCurrentServer() {
        return currentServer;
    }
    
    /**
     * 检查是否已连接
     */
    public boolean isConnected() {
        return currentSession != null && currentSession.isConnected();
    }
}
