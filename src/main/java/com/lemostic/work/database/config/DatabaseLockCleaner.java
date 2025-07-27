package com.lemostic.work.database.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 数据库锁清理工具
 * 用于清理H2数据库的锁文件
 */
public class DatabaseLockCleaner {
    
    private static final Logger logger = LoggerFactory.getLogger(DatabaseLockCleaner.class);
    private static final String DATABASE_DIR = "data";
    private static final String DATABASE_NAME = "workbench";
    
    /**
     * 清理数据库锁文件
     */
    public static void cleanupLockFiles() {
        try {
            logger.info("开始清理数据库锁文件...");

            String dbPath = DATABASE_DIR + File.separator + DATABASE_NAME;

            // H2数据库可能的锁文件和临时文件
            String[] lockFileExtensions = {
                ".lock.db",
                ".trace.db",
                ".temp.db",
                ".tmp"
            };

            boolean hasDeleted = false;
            for (String extension : lockFileExtensions) {
                Path lockFile = Paths.get(dbPath + extension);
                if (Files.exists(lockFile)) {
                    try {
                        Files.delete(lockFile);
                        logger.info("已删除数据库文件: {}", lockFile);
                        hasDeleted = true;
                    } catch (Exception e) {
                        logger.warn("无法删除文件: {}", lockFile, e);
                        // 尝试强制删除
                        try {
                            File file = lockFile.toFile();
                            if (file.exists()) {
                                file.setWritable(true);
                                if (file.delete()) {
                                    logger.info("强制删除成功: {}", lockFile);
                                    hasDeleted = true;
                                }
                            }
                        } catch (Exception ex) {
                            logger.warn("强制删除也失败: {}", lockFile, ex);
                        }
                    }
                }
            }

            if (!hasDeleted) {
                logger.info("没有发现需要清理的锁文件");
            }

        } catch (Exception e) {
            logger.error("清理数据库锁文件时出错", e);
        }
    }
    
    /**
     * 检查数据库是否被锁定
     */
    public static boolean isDatabaseLocked() {
        try {
            String dbPath = DATABASE_DIR + File.separator + DATABASE_NAME;
            Path lockFile = Paths.get(dbPath + ".lock.db");
            return Files.exists(lockFile);
        } catch (Exception e) {
            logger.error("检查数据库锁状态时出错", e);
            return false;
        }
    }
    
    /**
     * 强制清理数据库锁（谨慎使用）
     */
    public static void forceCleanup() {
        logger.warn("正在强制清理数据库锁文件...");
        
        try {
            String dbPath = DATABASE_DIR + File.separator + DATABASE_NAME;
            
            // 删除所有可能的锁文件和临时文件
            String[] patterns = {
                ".lock.db",
                ".trace.db", 
                ".temp.db",
                ".tmp"
            };
            
            File dataDir = new File(DATABASE_DIR);
            if (dataDir.exists() && dataDir.isDirectory()) {
                File[] files = dataDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        String fileName = file.getName();
                        for (String pattern : patterns) {
                            if (fileName.endsWith(pattern)) {
                                try {
                                    if (file.delete()) {
                                        logger.info("强制删除文件: {}", file.getAbsolutePath());
                                    } else {
                                        logger.warn("无法删除文件: {}", file.getAbsolutePath());
                                    }
                                } catch (Exception e) {
                                    logger.warn("删除文件时出错: {}", file.getAbsolutePath(), e);
                                }
                                break;
                            }
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            logger.error("强制清理数据库锁时出错", e);
        }
    }
}
