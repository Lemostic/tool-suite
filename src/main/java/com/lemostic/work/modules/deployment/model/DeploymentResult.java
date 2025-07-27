package com.lemostic.work.modules.deployment.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;

/**
 * 部署结果模型
 */
@Data
public class DeploymentResult {
    
    /**
     * 任务ID
     */
    private String taskId;
    
    /**
     * 目标服务器配置
     */
    private ServerConfiguration serverConfiguration;
    
    /**
     * 部署是否成功
     */
    private boolean success;
    
    /**
     * 错误消息（如果失败）
     */
    private String errorMessage;
    
    /**
     * 详细日志信息
     */
    private List<String> logMessages = new ArrayList<>();
    
    /**
     * 开始时间
     */
    private LocalDateTime startTime;
    
    /**
     * 结束时间
     */
    private LocalDateTime endTime;
    
    /**
     * 上传的文件大小（字节）
     */
    private long uploadedFileSize;
    
    /**
     * 备份文件路径（如果创建了备份）
     */
    private String backupFilePath;
    
    /**
     * 部署的文件列表
     */
    private List<String> deployedFiles = new ArrayList<>();

    // Constructors
    public DeploymentResult() {}

    public DeploymentResult(String taskId, ServerConfiguration serverConfiguration, boolean success,
                           String errorMessage, List<String> logMessages, LocalDateTime startTime,
                           LocalDateTime endTime, long uploadedFileSize, String backupFilePath,
                           List<String> deployedFiles) {
        this.taskId = taskId;
        this.serverConfiguration = serverConfiguration;
        this.success = success;
        this.errorMessage = errorMessage;
        this.logMessages = logMessages;
        this.startTime = startTime;
        this.endTime = endTime;
        this.uploadedFileSize = uploadedFileSize;
        this.backupFilePath = backupFilePath;
        this.deployedFiles = deployedFiles;
    }



    /**
     * 添加日志消息
     */
    public void addLogMessage(String message) {
        logMessages.add(LocalDateTime.now() + ": " + message);
    }

    /**
     * 添加部署的文件
     */
    public void addDeployedFile(String filePath) {
        deployedFiles.add(filePath);
    }

    /**
     * 获取执行时长（毫秒）
     */
    public long getDurationMillis() {
        if (startTime != null && endTime != null) {
            return java.time.Duration.between(startTime, endTime).toMillis();
        }
        return 0;
    }

    /**
     * 获取格式化的执行时长
     */
    public String getFormattedDuration() {
        long millis = getDurationMillis();
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;

        if (minutes > 0) {
            return String.format("%d分%d秒", minutes, seconds);
        } else {
            return String.format("%d秒", seconds);
        }
    }
}
