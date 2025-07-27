-- 数据库表结构初始化脚本
-- 适用于H2数据库

-- 服务器配置表
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
);

-- 部署历史表
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
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_server_config_name ON server_configurations(name);
CREATE INDEX IF NOT EXISTS idx_server_config_host ON server_configurations(host);
CREATE INDEX IF NOT EXISTS idx_server_config_enabled ON server_configurations(enabled);

CREATE INDEX IF NOT EXISTS idx_deployment_task_id ON deployment_history(task_id);
CREATE INDEX IF NOT EXISTS idx_deployment_server_name ON deployment_history(server_name);
CREATE INDEX IF NOT EXISTS idx_deployment_server_host ON deployment_history(server_host);
CREATE INDEX IF NOT EXISTS idx_deployment_success ON deployment_history(success);
CREATE INDEX IF NOT EXISTS idx_deployment_start_time ON deployment_history(start_time);

-- 插入示例数据
INSERT INTO server_configurations (name, host, port, username, password, upload_directory, installation_directory, backup_directory, enabled, description)
VALUES
('开发服务器', 'dev.example.com', 22, 'deploy', 'password123', '/tmp/upload', '/opt/app', '/opt/backup', true, '开发环境服务器');

INSERT INTO server_configurations (name, host, port, username, password, upload_directory, installation_directory, backup_directory, enabled, description)
VALUES
('测试服务器', 'test.example.com', 22, 'deploy', 'password123', '/tmp/upload', '/opt/app', '/opt/backup', true, '测试环境服务器');

INSERT INTO server_configurations (name, host, port, username, password, upload_directory, installation_directory, backup_directory, enabled, description)
VALUES
('生产服务器', 'prod.example.com', 22, 'deploy', 'password123', '/tmp/upload', '/opt/app', '/opt/backup', false, '生产环境服务器（默认禁用）');
