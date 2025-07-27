package com.lemostic.work.modules.deployment.model;

import lombok.Getter;
import lombok.Setter;

/**
 * 服务器配置模型
 */
@Setter
@Getter
public class ServerConfiguration {

    // Getter methods
    // Setter methods
    /**
     * 配置名称
     */
    private String name;
    
    /**
     * 服务器主机地址
     */
    private String host;
    
    /**
     * SSH端口
     */
    private int port = 22;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 密码
     */
    private String password;
    
    /**
     * 私钥文件路径（可选，与密码二选一）
     */
    private String privateKeyPath;
    
    /**
     * 私钥密码（可选）
     */
    private String privateKeyPassphrase;
    
    /**
     * 远程包上传目录
     */
    private String uploadDirectory;
    
    /**
     * 远程安装目录
     */
    private String installationDirectory;
    
    /**
     * 远程备份目录
     */
    private String backupDirectory;
    
    /**
     * 是否启用此配置
     */
    private boolean enabled = true;
    
    /**
     * 配置描述
     */
    private String description;

    // Constructors
    public ServerConfiguration() {}

    public ServerConfiguration(String name, String host, int port, String username, String password,
                             String privateKeyPath, String privateKeyPassphrase, String uploadDirectory,
                             String installationDirectory, String backupDirectory, boolean enabled, String description) {
        this.name = name;
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.privateKeyPath = privateKeyPath;
        this.privateKeyPassphrase = privateKeyPassphrase;
        this.uploadDirectory = uploadDirectory;
        this.installationDirectory = installationDirectory;
        this.backupDirectory = backupDirectory;
        this.enabled = enabled;
        this.description = description;
    }

    @Override
    public String toString() {
        return name + " (" + host + ")";
    }

    /**
     * 验证配置是否完整
     */
    public boolean isValid() {
        return name != null && !name.trim().isEmpty() &&
               host != null && !host.trim().isEmpty() &&
               username != null && !username.trim().isEmpty() &&
               (password != null && !password.trim().isEmpty() ||
                privateKeyPath != null && !privateKeyPath.trim().isEmpty()) &&
               uploadDirectory != null && !uploadDirectory.trim().isEmpty() &&
               installationDirectory != null && !installationDirectory.trim().isEmpty() &&
               backupDirectory != null && !backupDirectory.trim().isEmpty();
    }
}
