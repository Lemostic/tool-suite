package com.lemostic.work.modules.deployment.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 部署任务模型
 */
public class DeploymentTask {
    
    /**
     * 任务ID
     */
    private String taskId;
    
    /**
     * 本地包文件路径
     */
    private String localPackagePath;
    
    /**
     * 目标服务器配置列表
     */
    private List<ServerConfiguration> targetServers;
    
    /**
     * 需要移动的目录/文件列表（相对于解压后的包根目录）
     */
    private List<String> filesToMove;
    
    /**
     * 任务状态
     */
    private TaskStatus status = TaskStatus.PENDING;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime = LocalDateTime.now();
    
    /**
     * 开始时间
     */
    private LocalDateTime startTime;
    
    /**
     * 结束时间
     */
    private LocalDateTime endTime;
    
    /**
     * 任务描述
     */
    private String description;
    
    /**
     * 是否在部署前创建备份
     */
    private boolean createBackup = true;
    
    /**
     * 是否在部署后删除上传的包文件
     */
    private boolean cleanupAfterDeployment = true;

    // Constructors
    public DeploymentTask() {}

    public DeploymentTask(String taskId, String localPackagePath, List<ServerConfiguration> targetServers,
                         List<String> filesToMove, TaskStatus status, LocalDateTime createTime,
                         LocalDateTime startTime, LocalDateTime endTime, String description,
                         boolean createBackup, boolean cleanupAfterDeployment) {
        this.taskId = taskId;
        this.localPackagePath = localPackagePath;
        this.targetServers = targetServers;
        this.filesToMove = filesToMove;
        this.status = status;
        this.createTime = createTime;
        this.startTime = startTime;
        this.endTime = endTime;
        this.description = description;
        this.createBackup = createBackup;
        this.cleanupAfterDeployment = cleanupAfterDeployment;
    }

    // Getter methods
    public String getTaskId() { return taskId; }
    public String getLocalPackagePath() { return localPackagePath; }
    public List<ServerConfiguration> getTargetServers() { return targetServers; }
    public List<String> getFilesToMove() { return filesToMove; }
    public TaskStatus getStatus() { return status; }
    public LocalDateTime getCreateTime() { return createTime; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public String getDescription() { return description; }
    public boolean isCreateBackup() { return createBackup; }
    public boolean isCleanupAfterDeployment() { return cleanupAfterDeployment; }

    // Setter methods
    public void setTaskId(String taskId) { this.taskId = taskId; }
    public void setLocalPackagePath(String localPackagePath) { this.localPackagePath = localPackagePath; }
    public void setTargetServers(List<ServerConfiguration> targetServers) { this.targetServers = targetServers; }
    public void setFilesToMove(List<String> filesToMove) { this.filesToMove = filesToMove; }
    public void setStatus(TaskStatus status) { this.status = status; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public void setDescription(String description) { this.description = description; }
    public void setCreateBackup(boolean createBackup) { this.createBackup = createBackup; }
    public void setCleanupAfterDeployment(boolean cleanupAfterDeployment) { this.cleanupAfterDeployment = cleanupAfterDeployment; }

    /**
     * 任务状态枚举
     */
    public enum TaskStatus {
        PENDING("待执行"),
        RUNNING("执行中"),
        COMPLETED("已完成"),
        FAILED("失败"),
        CANCELLED("已取消");

        private final String displayName;

        TaskStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }
}
