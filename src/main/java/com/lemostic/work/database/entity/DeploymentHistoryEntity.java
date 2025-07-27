package com.lemostic.work.database.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 * 部署历史实体类 - MyBatis-Plus版本
 * 遵循MVVM架构中的Model层设计
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("deployment_history")
public class DeploymentHistoryEntity extends BaseEntity {

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 服务器名称
     */
    private String serverName;

    /**
     * 服务器主机地址
     */
    private String serverHost;

    /**
     * 包名称
     */
    private String packageName;

    /**
     * 包大小（字节）
     */
    private Long packageSize;

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 执行时长（毫秒）
     */
    private Long durationMillis;

    /**
     * 备份文件路径
     */
    private String backupFilePath;

    /**
     * 部署文件列表（JSON格式）
     */
    private String deployedFiles;

    /**
     * 任务描述
     */
    private String taskDescription;
}
