package com.lemostic.work.database.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 服务器配置实体类 - MyBatis-Plus版本
 * 遵循MVVM架构中的Model层设计
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("server_configurations")
public class ServerConfigurationEntity extends BaseEntity {

    /**
     * 配置名称 - 唯一标识
     */
    private String name;

    /**
     * 服务器主机地址
     */
    private String host;

    /**
     * SSH端口号
     */
    private Integer port = 22;

    /**
     * 登录用户名
     */
    private String username;

    /**
     * 登录密码
     */
    private String password;

    /**
     * 私钥文件路径
     */
    private String privateKeyPath;

    /**
     * 私钥密码
     */
    private String privateKeyPassphrase;

    /**
     * 上传目录
     */
    private String uploadDirectory;

    /**
     * 安装目录
     */
    private String installationDirectory;

    /**
     * 备份目录
     */
    private String backupDirectory;

    /**
     * 是否启用
     */
    private Boolean enabled = true;

    /**
     * 配置描述
     */
    private String description;

    // Getter and Setter methods
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPrivateKeyPath() {
        return privateKeyPath;
    }

    public void setPrivateKeyPath(String privateKeyPath) {
        this.privateKeyPath = privateKeyPath;
    }

    public String getPrivateKeyPassphrase() {
        return privateKeyPassphrase;
    }

    public void setPrivateKeyPassphrase(String privateKeyPassphrase) {
        this.privateKeyPassphrase = privateKeyPassphrase;
    }

    public String getUploadDirectory() {
        return uploadDirectory;
    }

    public void setUploadDirectory(String uploadDirectory) {
        this.uploadDirectory = uploadDirectory;
    }

    public String getInstallationDirectory() {
        return installationDirectory;
    }

    public void setInstallationDirectory(String installationDirectory) {
        this.installationDirectory = installationDirectory;
    }

    public String getBackupDirectory() {
        return backupDirectory;
    }

    public void setBackupDirectory(String backupDirectory) {
        this.backupDirectory = backupDirectory;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
