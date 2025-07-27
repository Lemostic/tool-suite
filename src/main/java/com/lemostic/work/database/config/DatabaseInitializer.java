package com.lemostic.work.database.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.Statement;
import java.util.stream.Collectors;

/**
 * 数据库初始化器
 * 负责创建表结构和初始化数据
 */
public class DatabaseInitializer {
    
    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);
    private static boolean initialized = false;
    
    /**
     * 初始化数据库
     */
    public static synchronized void initialize(DataSource dataSource) {
        if (initialized) {
            logger.debug("数据库已经初始化");
            return;
        }
        
        try {
            logger.info("开始初始化数据库...");

            // 分步执行：先创建表，再创建索引
            createTables(dataSource);
            createIndexes(dataSource);
            insertSampleData(dataSource);

            initialized = true;
            logger.info("数据库初始化完成");

        } catch (Exception e) {
            logger.error("数据库初始化失败", e);
            throw new RuntimeException("数据库初始化失败", e);
        }
    }
    
    /**
     * 执行SQL脚本
     */
    private static void executeSqlScript(DataSource dataSource, String scriptPath) {
        try (Connection connection = dataSource.getConnection()) {

            // 读取SQL脚本
            String sql = readSqlScript(scriptPath);
            if (sql == null || sql.trim().isEmpty()) {
                logger.warn("SQL脚本为空: {}", scriptPath);
                return;
            }

            // 按行分割，然后重新组合完整的SQL语句
            String[] lines = sql.split("\\r?\\n");
            StringBuilder currentStatement = new StringBuilder();

            try (Statement statement = connection.createStatement()) {
                for (String line : lines) {
                    String trimmedLine = line.trim();

                    // 跳过空行和注释行
                    if (trimmedLine.isEmpty() || trimmedLine.startsWith("--")) {
                        continue;
                    }

                    // 添加到当前语句
                    currentStatement.append(trimmedLine).append(" ");

                    // 如果行以分号结尾，表示一个完整的SQL语句
                    if (trimmedLine.endsWith(";")) {
                        String completeSql = currentStatement.toString().trim();
                        // 移除末尾的分号
                        if (completeSql.endsWith(";")) {
                            completeSql = completeSql.substring(0, completeSql.length() - 1);
                        }

                        if (!completeSql.isEmpty()) {
                            logger.debug("执行SQL: {}", completeSql);
                            try {
                                statement.execute(completeSql);
                                logger.debug("SQL执行成功");
                            } catch (Exception e) {
                                logger.warn("SQL执行失败，继续执行下一条: {}", completeSql, e);
                            }
                        }

                        // 重置当前语句
                        currentStatement.setLength(0);
                    }
                }

                // 处理最后一个可能没有分号的语句
                if (currentStatement.length() > 0) {
                    String completeSql = currentStatement.toString().trim();
                    if (!completeSql.isEmpty()) {
                        logger.debug("执行最后的SQL: {}", completeSql);
                        try {
                            statement.execute(completeSql);
                            logger.debug("SQL执行成功");
                        } catch (Exception e) {
                            logger.warn("SQL执行失败: {}", completeSql, e);
                        }
                    }
                }
            }

            logger.info("SQL脚本执行完成: {}", scriptPath);

        } catch (Exception e) {
            logger.error("执行SQL脚本失败: {}", scriptPath, e);
            throw new RuntimeException("执行SQL脚本失败: " + scriptPath, e);
        }
    }
    
    /**
     * 读取SQL脚本文件
     */
    private static String readSqlScript(String scriptPath) {
        try (InputStream inputStream = DatabaseInitializer.class.getResourceAsStream(scriptPath)) {
            if (inputStream == null) {
                logger.error("找不到SQL脚本文件: {}", scriptPath);
                return null;
            }
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
            
        } catch (Exception e) {
            logger.error("读取SQL脚本文件失败: {}", scriptPath, e);
            return null;
        }
    }
    
    /**
     * 检查是否已初始化
     */
    public static boolean isInitialized() {
        return initialized;
    }
    
    /**
     * 创建表结构
     */
    private static void createTables(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            logger.info("开始创建表结构...");

            // 服务器配置表
            String createServerConfigTable = """
                CREATE TABLE IF NOT EXISTS server_configurations (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(100) NOT NULL UNIQUE,
                    host VARCHAR(255) NOT NULL,
                    port INT NOT NULL DEFAULT 22,
                    username VARCHAR(100) NOT NULL,
                    password VARCHAR(255),
                    private_key_path VARCHAR(500),
                    private_key_passphrase VARCHAR(255),
                    upload_directory VARCHAR(500) NOT NULL,
                    installation_directory VARCHAR(500) NOT NULL,
                    backup_directory VARCHAR(500) NOT NULL,
                    enabled BOOLEAN NOT NULL DEFAULT TRUE,
                    description VARCHAR(1000),
                    created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    version INT NOT NULL DEFAULT 0,
                    deleted INT NOT NULL DEFAULT 0
                )
                """;

            statement.execute(createServerConfigTable);
            logger.info("服务器配置表创建成功");

            // 部署历史表
            String createDeploymentHistoryTable = """
                CREATE TABLE IF NOT EXISTS deployment_history (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    task_id VARCHAR(100) NOT NULL,
                    server_name VARCHAR(100) NOT NULL,
                    server_host VARCHAR(255) NOT NULL,
                    package_name VARCHAR(255) NOT NULL,
                    package_size BIGINT NOT NULL,
                    success BOOLEAN NOT NULL,
                    error_message TEXT,
                    start_time TIMESTAMP NOT NULL,
                    end_time TIMESTAMP,
                    duration_millis BIGINT,
                    backup_file_path VARCHAR(500),
                    deployed_files TEXT,
                    task_description VARCHAR(500),
                    created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    version INT NOT NULL DEFAULT 0,
                    deleted INT NOT NULL DEFAULT 0
                )
                """;

            statement.execute(createDeploymentHistoryTable);
            logger.info("部署历史表创建成功");

        } catch (Exception e) {
            logger.error("创建表结构失败", e);
            throw new RuntimeException("创建表结构失败", e);
        }
    }

    /**
     * 创建索引
     */
    private static void createIndexes(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            logger.info("开始创建索引...");

            // 服务器配置表索引
            String[] serverConfigIndexes = {
                "CREATE INDEX IF NOT EXISTS idx_server_config_name ON server_configurations(name)",
                "CREATE INDEX IF NOT EXISTS idx_server_config_host ON server_configurations(host)",
                "CREATE INDEX IF NOT EXISTS idx_server_config_enabled ON server_configurations(enabled)"
            };

            for (String indexSql : serverConfigIndexes) {
                try {
                    statement.execute(indexSql);
                    logger.debug("索引创建成功: {}", indexSql);
                } catch (Exception e) {
                    logger.warn("索引创建失败，继续执行: {}", indexSql, e);
                }
            }

            // 部署历史表索引
            String[] deploymentHistoryIndexes = {
                "CREATE INDEX IF NOT EXISTS idx_deployment_task_id ON deployment_history(task_id)",
                "CREATE INDEX IF NOT EXISTS idx_deployment_server_name ON deployment_history(server_name)",
                "CREATE INDEX IF NOT EXISTS idx_deployment_server_host ON deployment_history(server_host)",
                "CREATE INDEX IF NOT EXISTS idx_deployment_success ON deployment_history(success)",
                "CREATE INDEX IF NOT EXISTS idx_deployment_start_time ON deployment_history(start_time)"
            };

            for (String indexSql : deploymentHistoryIndexes) {
                try {
                    statement.execute(indexSql);
                    logger.debug("索引创建成功: {}", indexSql);
                } catch (Exception e) {
                    logger.warn("索引创建失败，继续执行: {}", indexSql, e);
                }
            }

            logger.info("索引创建完成");

        } catch (Exception e) {
            logger.error("创建索引失败", e);
            throw new RuntimeException("创建索引失败", e);
        }
    }

    /**
     * 插入示例数据
     */
    private static void insertSampleData(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            logger.info("开始插入示例数据...");

            // 检查是否已有数据
            var rs = statement.executeQuery("SELECT COUNT(*) FROM server_configurations");
            if (rs.next() && rs.getInt(1) > 0) {
                logger.info("表中已有数据，跳过示例数据插入");
                return;
            }

            // 插入示例数据
            String[] sampleData = {
                "INSERT INTO server_configurations (name, host, port, username, password, upload_directory, installation_directory, backup_directory, enabled, description) VALUES ('开发服务器', 'dev.example.com', 22, 'deploy', 'password123', '/tmp/upload', '/opt/app', '/opt/backup', true, '开发环境服务器')",
                "INSERT INTO server_configurations (name, host, port, username, password, upload_directory, installation_directory, backup_directory, enabled, description) VALUES ('测试服务器', 'test.example.com', 22, 'deploy', 'password123', '/tmp/upload', '/opt/app', '/opt/backup', true, '测试环境服务器')",
                "INSERT INTO server_configurations (name, host, port, username, password, upload_directory, installation_directory, backup_directory, enabled, description) VALUES ('生产服务器', 'prod.example.com', 22, 'deploy', 'password123', '/tmp/upload', '/opt/app', '/opt/backup', false, '生产环境服务器（默认禁用）')"
            };

            for (String insertSql : sampleData) {
                try {
                    statement.execute(insertSql);
                    logger.debug("示例数据插入成功");
                } catch (Exception e) {
                    logger.warn("示例数据插入失败，继续执行: {}", e.getMessage());
                }
            }

            logger.info("示例数据插入完成");

        } catch (Exception e) {
            logger.error("插入示例数据失败", e);
            // 示例数据插入失败不应该阻止应用启动
            logger.warn("示例数据插入失败，但应用将继续运行");
        }
    }

    /**
     * 重置初始化状态（用于测试）
     */
    public static void reset() {
        initialized = false;
    }
}
