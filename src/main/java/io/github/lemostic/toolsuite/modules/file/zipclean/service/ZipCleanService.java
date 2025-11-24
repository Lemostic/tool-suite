package io.github.lemostic.toolsuite.modules.file.zipclean.service;

import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * 压缩包清理服务
 */
public class ZipCleanService {
    
    private static final Logger logger = LoggerFactory.getLogger(ZipCleanService.class);
    
    private final DoubleProperty progress = new SimpleDoubleProperty(0);
    private final StringProperty statusMessage = new SimpleStringProperty("");
    
    /**
     * 清理规则
     */
    public static class CleanRule {
        private String pattern;
        private boolean isRegex;
        private boolean enabled;
        
        public CleanRule(String pattern, boolean isRegex, boolean enabled) {
            this.pattern = pattern;
            this.isRegex = isRegex;
            this.enabled = enabled;
        }
        
        public String getPattern() {
            return pattern;
        }
        
        public boolean isRegex() {
            return isRegex;
        }
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public boolean matches(String path) {
            if (!enabled) {
                return false;
            }
            
            if (isRegex) {
                try {
                    return Pattern.matches(pattern, path);
                } catch (Exception e) {
                    logger.warn("正则表达式匹配失败: {}", pattern, e);
                    return false;
                }
            } else {
                // 简单模式匹配
                return path.contains(pattern);
            }
        }
    }
    
    /**
     * 清理结果
     */
    public static class CleanResult {
        private int totalFiles;
        private int deletedFiles;
        private int keptFiles;
        private long originalSize;
        private long cleanedSize;
        private List<String> deletedPaths;
        private boolean success;
        private String message;
        
        public CleanResult() {
            this.deletedPaths = new ArrayList<>();
        }
        
        // Getters and setters
        public int getTotalFiles() { return totalFiles; }
        public void setTotalFiles(int totalFiles) { this.totalFiles = totalFiles; }
        
        public int getDeletedFiles() { return deletedFiles; }
        public void setDeletedFiles(int deletedFiles) { this.deletedFiles = deletedFiles; }
        
        public int getKeptFiles() { return keptFiles; }
        public void setKeptFiles(int keptFiles) { this.keptFiles = keptFiles; }
        
        public long getOriginalSize() { return originalSize; }
        public void setOriginalSize(long originalSize) { this.originalSize = originalSize; }
        
        public long getCleanedSize() { return cleanedSize; }
        public void setCleanedSize(long cleanedSize) { this.cleanedSize = cleanedSize; }
        
        public List<String> getDeletedPaths() { return deletedPaths; }
        public void addDeletedPath(String path) { this.deletedPaths.add(path); }
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
    
    public DoubleProperty progressProperty() {
        return progress;
    }
    
    public StringProperty statusMessageProperty() {
        return statusMessage;
    }
    
    /**
     * 清理压缩包
     * @param zipFile 源压缩包
     * @param rules 清理规则列表
     * @param outputFile 输出文件/目录路径
     * @param extractAfterClean 清理后是否解压
     * @param deleteOriginal 是否删除原文件
     */
    public CleanResult cleanZip(File zipFile, List<CleanRule> rules, File outputFile, 
                                boolean extractAfterClean, boolean deleteOriginal) throws IOException {
        CleanResult result = new CleanResult();
        
        if (!zipFile.exists()) {
            result.setSuccess(false);
            result.setMessage("源文件不存在");
            return result;
        }
        
        result.setOriginalSize(zipFile.length());
        
        // 临时目录
        Path tempDir = Files.createTempDirectory("zipclean_");
        
        try {
            updateStatus("正在解压文件...", 0.1);
            
            // 解压到临时目录
            unzip(zipFile, tempDir.toFile());
            
            updateStatus("正在分析文件...", 0.3);
            
            // 统计和删除文件
            List<Path> allFiles = listAllFiles(tempDir);
            result.setTotalFiles(allFiles.size());
            
            int processed = 0;
            for (Path file : allFiles) {
                String relativePath = tempDir.relativize(file).toString().replace("\\", "/");
                
                boolean shouldDelete = false;
                for (CleanRule rule : rules) {
                    if (rule.matches(relativePath)) {
                        shouldDelete = true;
                        break;
                    }
                }
                
                if (shouldDelete) {
                    Files.deleteIfExists(file);
                    result.addDeletedPath(relativePath);
                    result.setDeletedFiles(result.getDeletedFiles() + 1);
                } else {
                    result.setKeptFiles(result.getKeptFiles() + 1);
                }
                
                processed++;
                updateStatus("正在处理文件... (" + processed + "/" + allFiles.size() + ")", 
                    0.3 + (processed * 0.4 / allFiles.size()));
            }
            
            // 删除空目录
            deleteEmptyDirectories(tempDir);
            
            if (extractAfterClean) {
                // 直接将临时目录移动到目标位置
                updateStatus("正在生成文件夹...", 0.8);
                
                if (outputFile.exists()) {
                    deleteDirectory(outputFile.toPath());
                }
                
                // 将临时目录内容移动到输出目录
                Files.move(tempDir, outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                
                // 计算文件夹大小
                result.setCleanedSize(calculateDirectorySize(outputFile.toPath()));
                result.setSuccess(true);
                result.setMessage("清理完成（已解压）");
                
                updateStatus("清理完成（已解压到文件夹）！", 1.0);
                
                // 标记为不删除临时目录（因为已经移动）
                tempDir = null;
            } else {
                // 重新打包为压缩文件
                updateStatus("正在打包文件...", 0.8);
                
                zip(tempDir.toFile(), outputFile);
                
                result.setCleanedSize(outputFile.length());
                result.setSuccess(true);
                result.setMessage("清理完成");
                
                updateStatus("清理完成！", 1.0);
            }
            
            // 删除原文件（如果需要）
            if (deleteOriginal && zipFile.exists()) {
                boolean deleted = zipFile.delete();
                if (deleted) {
                    logger.info("已删除原文件: {}", zipFile.getAbsolutePath());
                } else {
                    logger.warn("无法删除原文件: {}", zipFile.getAbsolutePath());
                }
            }
            
        } finally {
            // 清理临时目录（如果未被移动）
            if (tempDir != null) {
                deleteDirectory(tempDir);
            }
        }
        
        return result;
    }
    
    /**
     * 预览将要删除的文件
     */
    public List<String> previewDeletion(File zipFile, List<CleanRule> rules) throws IOException {
        List<String> toDelete = new ArrayList<>();
        
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    String path = entry.getName();
                    
                    for (CleanRule rule : rules) {
                        if (rule.matches(path)) {
                            toDelete.add(path);
                            break;
                        }
                    }
                }
                zis.closeEntry();
            }
        }
        
        return toDelete;
    }
    
    private void updateStatus(String message, double progress) {
        Platform.runLater(() -> {
            this.statusMessage.set(message);
            this.progress.set(progress);
        });
        logger.info("{} - {}", message, String.format("%.0f%%", progress * 100));
    }
    
    private void unzip(File zipFile, File destDir) throws IOException {
        byte[] buffer = new byte[8192];
        
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File newFile = new File(destDir, entry.getName());
                
                if (entry.isDirectory()) {
                    newFile.mkdirs();
                } else {
                    newFile.getParentFile().mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                zis.closeEntry();
            }
        }
    }
    
    private void zip(File sourceDir, File zipFile) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            zipDirectory(sourceDir, sourceDir, zos);
        }
    }
    
    private void zipDirectory(File rootDir, File sourceDir, ZipOutputStream zos) throws IOException {
        File[] files = sourceDir.listFiles();
        if (files == null) return;
        
        byte[] buffer = new byte[8192];
        
        for (File file : files) {
            if (file.isDirectory()) {
                zipDirectory(rootDir, file, zos);
            } else {
                String relativePath = rootDir.toPath().relativize(file.toPath()).toString().replace("\\", "/");
                ZipEntry entry = new ZipEntry(relativePath);
                zos.putNextEntry(entry);
                
                try (FileInputStream fis = new FileInputStream(file)) {
                    int len;
                    while ((len = fis.read(buffer)) > 0) {
                        zos.write(buffer, 0, len);
                    }
                }
                zos.closeEntry();
            }
        }
    }
    
    private List<Path> listAllFiles(Path dir) throws IOException {
        List<Path> files = new ArrayList<>();
        Files.walk(dir)
            .filter(Files::isRegularFile)
            .forEach(files::add);
        return files;
    }
    
    private void deleteEmptyDirectories(Path dir) throws IOException {
        Files.walk(dir)
            .sorted(Comparator.reverseOrder())
            .forEach(path -> {
                try {
                    if (Files.isDirectory(path) && !path.equals(dir)) {
                        File[] files = path.toFile().listFiles();
                        if (files == null || files.length == 0) {
                            Files.deleteIfExists(path);
                        }
                    }
                } catch (IOException e) {
                    logger.warn("删除空目录失败: {}", path, e);
                }
            });
    }
    
    private void deleteDirectory(Path dir) {
        if (dir == null || !Files.exists(dir)) {
            return;
        }
        try {
            Files.walk(dir)
                .sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException e) {
                        logger.warn("删除临时文件失败: {}", path, e);
                    }
                });
        } catch (IOException e) {
            logger.error("删除临时目录失败", e);
        }
    }
    
    /**
     * 计算目录大小
     */
    private long calculateDirectorySize(Path dir) throws IOException {
        return Files.walk(dir)
            .filter(Files::isRegularFile)
            .mapToLong(path -> {
                try {
                    return Files.size(path);
                } catch (IOException e) {
                    return 0L;
                }
            })
            .sum();
    }
}
