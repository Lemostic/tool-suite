package com.lemostic.work.database.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.File;

/**
 * MyBatis-Plus配置类
 * 负责数据库连接和MyBatis-Plus相关配置
 * 简化版本，不依赖Spring Boot
 */
public class MyBatisPlusConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(MyBatisPlusConfig.class);
    private static final String DATABASE_DIR = "data";
    private static final String DATABASE_NAME = "workbench";
    
    private static MyBatisPlusConfig instance;
    private HikariDataSource dataSource;

    private MyBatisPlusConfig() {
        initializeDataSource();

        // 添加JVM关闭钩子，确保数据库连接正常关闭
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }

    public static synchronized MyBatisPlusConfig getInstance() {
        if (instance == null) {
            instance = new MyBatisPlusConfig();
        }
        return instance;
    }

    /**
     * 数据源配置
     */
    private void initializeDataSource() {
        // 确保数据库目录存在
        File dataDir = new File(DATABASE_DIR);
        if (!dataDir.exists()) {
            boolean created = dataDir.mkdirs();
            if (created) {
                logger.info("创建数据库目录: {}", dataDir.getAbsolutePath());
            }
        }

        // 清理可能存在的锁文件
        DatabaseLockCleaner.cleanupLockFiles();

        // 等待一下确保文件系统操作完成
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        HikariConfig config = new HikariConfig();
        
        // H2数据库配置 - 文件模式
        String dbPath = DATABASE_DIR + File.separator + DATABASE_NAME;
        config.setJdbcUrl("jdbc:h2:file:./" + dbPath +
            ";DB_CLOSE_DELAY=-1" +
            ";MODE=MySQL" +
            ";AUTO_SERVER=TRUE");  // 允许多个连接访问同一个数据库
        config.setDriverClassName("org.h2.Driver");
        config.setUsername("sa");
        config.setPassword("");
        
        // 连接池配置 - 减少连接数避免锁冲突
        config.setMaximumPoolSize(3);      // 最大3个连接
        config.setMinimumIdle(1);          // 最小1个空闲连接
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(300000);     // 5分钟空闲超时
        config.setMaxLifetime(1800000);    // 30分钟最大生命周期
        config.setLeakDetectionThreshold(60000); // 1分钟泄漏检测
        config.setPoolName("WorkbenchHikariCP");
        config.setConnectionTestQuery("SELECT 1");
        config.setAutoCommit(true);
        
        logger.info("数据源配置完成: {}", config.getJdbcUrl());
        this.dataSource = new HikariDataSource(config);
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    /**
     * 检查数据库是否可用
     */
    public boolean isAvailable() {
        return dataSource != null && !dataSource.isClosed();
    }

    /**
     * 关闭数据库连接池
     */
    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            try {
                logger.info("正在关闭数据库连接池...");
                dataSource.close();
                logger.info("数据库连接池已关闭");
            } catch (Exception e) {
                logger.error("关闭数据库连接池时出错", e);
            }
        }
    }
}
