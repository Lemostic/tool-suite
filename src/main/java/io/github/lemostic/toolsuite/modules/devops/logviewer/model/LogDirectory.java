package io.github.lemostic.toolsuite.modules.devops.logviewer.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 日志目录配置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogDirectory implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /** 目录ID */
    private String id;
    
    /** 目录名称（用于Tab显示） */
    private String name;
    
    /** 目录路径 */
    private String path;
    
    /** 文件过滤模式（支持通配符，如 *.log） */
    @Builder.Default
    private String filePattern = "*.log";
    
    /** 排序顺序 */
    @Builder.Default
    private int order = 0;
    
    /**
     * 获取显示名称
     */
    public String getDisplayName() {
        return name != null && !name.isEmpty() ? name : path;
    }
}
