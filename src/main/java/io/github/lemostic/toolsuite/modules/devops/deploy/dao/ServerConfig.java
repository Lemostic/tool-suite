package io.github.lemostic.toolsuite.modules.devops.deploy.dao;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 服务器配置实体类
 */
@Data
@Entity
@Table(name = "server_config")
public class ServerConfig {

    // Getters and Setters
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false)
    private String host;
    
    @Column(nullable = false)
    private Integer port = 22;
    
    @Column(nullable = false)
    private String username;
    
    @Column(nullable = false)
    private String password;
    
    @Column(name = "app_directory", nullable = false)
    private String appDirectory;
    
    @Column(name = "backup_directory")
    private String backupDirectory;
    
    @Column(name = "bin_directory")
    private String binDirectory;
    
    @Column(name = "stop_script")
    private String stopScript;
    
    @Column(name = "start_script")
    private String startScript;
    
    private String description;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    private Boolean enabled = true;
    
    // Constructors
    public ServerConfig() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
        this.updatedAt = LocalDateTime.now();
    }
    
    @Override
    public String toString() {
        return "ServerConfig{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", host='" + host + '\'' +
                ", port=" + port +
                '}';
    }
}