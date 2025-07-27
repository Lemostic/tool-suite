package com.lemostic.work.modules.deployment.util;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

/**
 * 日志工具类
 */
public class LoggingUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(LoggingUtil.class);
    private static final String LOG_BASE_DIR = "logs";
    private static final String DEPLOYMENT_MODULE_DIR = "deployment";
    private static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter LOG_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * 日志级别枚举
     */
    public enum LogLevel {
        INFO, WARN, ERROR, DEBUG
    }
    
    /**
     * 初始化日志目录
     */
    public static void initLogDirectories() {
        try {
            Path logDir = Paths.get(LOG_BASE_DIR, DEPLOYMENT_MODULE_DIR);
            if (!Files.exists(logDir)) {
                Files.createDirectories(logDir);
                logger.info("创建日志目录: {}", logDir.toAbsolutePath());
            }
        } catch (IOException e) {
            logger.error("创建日志目录失败", e);
        }
    }
    
    /**
     * 写入部署日志
     */
    public static void writeDeploymentLog(String taskId, LogLevel level, String message) {
        writeDeploymentLog(taskId, level, message, null);
    }
    
    /**
     * 写入部署日志（带回调）
     */
    public static void writeDeploymentLog(String taskId, LogLevel level, String message, Consumer<String> logCallback) {
        try {
            String timestamp = LocalDateTime.now().format(LOG_TIME_FORMAT);
            String logEntry = String.format("[%s] [%s] %s%n", timestamp, level.name(), message);
            
            // 写入文件
            Path logFile = getLogFilePath(taskId);
            Files.write(logFile, logEntry.getBytes(), 
                       StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            
            // 控制台输出
            switch (level) {
                case INFO:
                    logger.info("[{}] {}", taskId, message);
                    break;
                case WARN:
                    logger.warn("[{}] {}", taskId, message);
                    break;
                case ERROR:
                    logger.error("[{}] {}", taskId, message);
                    break;
                case DEBUG:
                    logger.debug("[{}] {}", taskId, message);
                    break;
            }
            
            // 回调通知
            if (logCallback != null) {
                logCallback.accept(logEntry.trim());
            }
            
        } catch (IOException e) {
            logger.error("写入部署日志失败", e);
        }
    }
    
    /**
     * 获取日志文件路径
     */
    private static Path getLogFilePath(String taskId) {
        String dateStr = LocalDateTime.now().format(FILE_DATE_FORMAT);
        String fileName = String.format("deployment_%s_%s.log", taskId, dateStr);
        return Paths.get(LOG_BASE_DIR, DEPLOYMENT_MODULE_DIR, fileName);
    }
    
    /**
     * 读取指定任务的日志内容
     */
    public static String readDeploymentLog(String taskId) {
        try {
            Path logFile = getLogFilePath(taskId);
            if (Files.exists(logFile)) {
                return FileUtil.readUtf8String(logFile.toFile());
            }
        } catch (Exception e) {
            logger.error("读取部署日志失败", e);
        }
        return "";
    }
    
    /**
     * 清理过期日志文件（保留最近30天）
     */
    public static void cleanupOldLogs() {
        try {
            Path logDir = Paths.get(LOG_BASE_DIR, DEPLOYMENT_MODULE_DIR);
            if (!Files.exists(logDir)) {
                return;
            }
            
            long cutoffTime = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000); // 30天前
            
            Files.list(logDir)
                 .filter(path -> path.toString().endsWith(".log"))
                 .filter(path -> {
                     try {
                         return Files.getLastModifiedTime(path).toMillis() < cutoffTime;
                     } catch (IOException e) {
                         return false;
                     }
                 })
                 .forEach(path -> {
                     try {
                         Files.delete(path);
                         logger.info("删除过期日志文件: {}", path.getFileName());
                     } catch (IOException e) {
                         logger.warn("删除日志文件失败: {}", path.getFileName(), e);
                     }
                 });
                 
        } catch (IOException e) {
            logger.error("清理过期日志失败", e);
        }
    }
    
    /**
     * 获取日志目录路径
     */
    public static String getLogDirectory() {
        return Paths.get(LOG_BASE_DIR, DEPLOYMENT_MODULE_DIR).toAbsolutePath().toString();
    }
}
