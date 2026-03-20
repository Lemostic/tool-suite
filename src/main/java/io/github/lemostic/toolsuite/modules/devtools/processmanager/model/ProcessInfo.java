package io.github.lemostic.toolsuite.modules.devtools.processmanager.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 进程信息模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessInfo {
    
    /** 进程ID */
    private int pid;
    
    /** 进程名称 */
    private String name;
    
    /** 进程完整路径 */
    private String path;
    
    /** 进程命令行 */
    private String commandLine;
    
    /** 占用的端口列表 */
    private List<Integer> ports;
    
    /** 内存使用 (KB) */
    private long memoryKb;
    
    /** CPU使用率 */
    private double cpuPercent;
    
    /** 进程状态 */
    private String status;
    
    /** 是否被选中 */
    private boolean selected;
    
    /**
     * 获取格式化的内存大小
     */
    public String getFormattedMemory() {
        if (memoryKb < 1024) {
            return memoryKb + " KB";
        } else if (memoryKb < 1024 * 1024) {
            return String.format("%.1f MB", memoryKb / 1024.0);
        } else {
            return String.format("%.1f GB", memoryKb / (1024.0 * 1024));
        }
    }
    
    /**
     * 获取端口字符串
     */
    public String getPortsString() {
        if (ports == null || ports.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ports.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(ports.get(i));
        }
        return sb.toString();
    }
    
    /**
     * 检查是否占用指定端口
     */
    public boolean hasPort(int port) {
        return ports != null && ports.contains(port);
    }
}
