package com.lemostic.work.database.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 基础实体类 - MyBatis-Plus版本
 * 提供统一的主键、时间戳和版本控制
 */
@Data
public abstract class BaseEntity {

    /**
     * 主键ID - 自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 创建时间 - 自动填充
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    /**
     * 更新时间 - 自动填充
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;

    /**
     * 版本号 - 乐观锁
     */
    @Version
    private Integer version = 0;

    /**
     * 逻辑删除标记
     */
    @TableLogic
    private Integer deleted = 0;

}
