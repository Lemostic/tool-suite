package io.github.lemostic.toolsuite.modules.devops.deploy.dao;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 部署历史实体类
 */
@Entity
@Table(name = "deployment_history")
public class DeploymentHistory {
    
    public enum DeploymentStatus {
        PENDING, RUNNING, SUCCESS, FAILED, ROLLED_BACK
    }
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "server_id", nullable = false)
    private Long serverId;
    
    @Column(name = "server_name", nullable = false)
    private String serverName;
    
    @Column(name = "package_name", nullable = false)
    private String packageName;
    
    @Enumerated(EnumType.STRING)
    private DeploymentStatus status = DeploymentStatus.PENDING;
    
    @Column(name = "start_time")
    private LocalDateTime startTime;
    
    @Column(name = "end_time")
    private LocalDateTime endTime;
    
    @Column(name = "error_message")
    private String errorMessage;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    // Constructors
    public DeploymentHistory() {
        this.createdAt = LocalDateTime.now();
    }
    
    public DeploymentHistory(Long serverId, String serverName, String packageName) {
        this();
        this.serverId = serverId;
        this.serverName = serverName;
        this.packageName = packageName;
        this.startTime = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getServerId() {
        return serverId;
    }
    
    public void setServerId(Long serverId) {
        this.serverId = serverId;
    }
    
    public String getServerName() {
        return serverName;
    }
    
    public void setServerName(String serverName) {
        this.serverName = serverName;
    }
    
    public String getPackageName() {
        return packageName;
    }
    
    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }
    
    public DeploymentStatus getStatus() {
        return status;
    }
    
    public void setStatus(DeploymentStatus status) {
        this.status = status;
    }
    
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }
    
    public LocalDateTime getEndTime() {
        return endTime;
    }
    
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    // 业务方法
    public void markSuccess() {
        this.status = DeploymentStatus.SUCCESS;
        this.endTime = LocalDateTime.now();
    }
    
    public void markFailed(String errorMessage) {
        this.status = DeploymentStatus.FAILED;
        this.endTime = LocalDateTime.now();
        this.errorMessage = errorMessage;
    }
    
    public void markRunning() {
        this.status = DeploymentStatus.RUNNING;
        this.startTime = LocalDateTime.now();
    }
    
    public void markRolledBack() {
        this.status = DeploymentStatus.ROLLED_BACK;
        this.endTime = LocalDateTime.now();
    }
    
    public long getDurationSeconds() {
        if (startTime == null || endTime == null) {
            return 0;
        }
        return java.time.Duration.between(startTime, endTime).getSeconds();
    }
    
    @Override
    public String toString() {
        return "DeploymentHistory{" +
                "id=" + id +
                ", serverName='" + serverName + '\'' +
                ", packageName='" + packageName + '\'' +
                ", status=" + status +
                ", duration=" + getDurationSeconds() + "s" +
                '}';
    }
}