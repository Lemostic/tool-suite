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
@Table(name = "deploy_history")
public class DeploymentHistoryEntity {
    
    @Id
    private String id;
    
    @Column(name = "server_id", nullable = false)
    private Long serverId;
    
    @Column(name = "server_name", nullable = false)
    private String serverName;
    
    @Column(name = "package_name")
    private String packageName;
    
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private DeploymentStatus status;
    
    @Column(name = "start_time")
    private LocalDateTime startTime;
    
    @Column(name = "end_time")
    private LocalDateTime endTime;
    
    @Column(name = "error_message", length = 2000)
    private String errorMessage;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    public enum DeploymentStatus {
        PENDING, RUNNING, SUCCESS, FAILED, CANCELLED
    }
}
