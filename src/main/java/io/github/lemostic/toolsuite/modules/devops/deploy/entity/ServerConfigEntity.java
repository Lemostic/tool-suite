package io.github.lemostic.toolsuite.modules.devops.deploy.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "deploy_server_config")
public class ServerConfigEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
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
    private String binDirectory = "bin";
    
    @Column(name = "stop_script")
    private String stopScript = "stop.sh";
    
    @Column(name = "start_script")
    private String startScript = "start.sh";
    
    @Column(length = 500)
    private String description;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(nullable = false)
    private Boolean enabled = true;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
