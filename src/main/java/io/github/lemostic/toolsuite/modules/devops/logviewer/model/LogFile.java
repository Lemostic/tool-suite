package io.github.lemostic.toolsuite.modules.devops.logviewer.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 日志文件信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogFile implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /** 文件名 */
    private String name;
    
    /** 完整路径 */
    private String fullPath;
    
    /** 文件大小（字节） */
    private long size;
    
    /** 最后修改时间 */
    private LocalDateTime lastModified;
    
    /** 是否为目录 */
    private boolean directory;
    
    /**
     * 获取格式化的文件大小
     */
    public String getFormattedSize() {
        if (directory) {
            return "<DIR>";
        }
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.1f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", size / (1024.0 * 1024));
        } else {
            return String.format("%.1f GB", size / (1024.0 * 1024 * 1024));
        }
    }
}
