package io.github.lemostic.toolsuite.modules.devops.logviewer.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 服务器配置信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServerConfig implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /** 服务器ID */
    private String id;
    
    /** 服务器名称 */
    private String name;
    
    /** 服务器主机地址 */
    private String host;
    
    /** SSH端口 */
    private int port;
    
    /** 用户名 */
    private String username;
    
    /** 密码（加密存储） */
    private String password;
    
    /** 私钥路径（可选） */
    private String privateKeyPath;
    
    /** 私钥密码（可选） */
    private String privateKeyPassphrase;
    
    /** 日志目录列表 */
    @Builder.Default
    private List<LogDirectory> logDirectories = new ArrayList<>();
    
    /** 是否使用密码认证 */
    @Builder.Default
    private boolean usePasswordAuth = true;
    
    /** 备注 */
    private String remark;
    
    /**
     * 获取显示名称
     */
    public String getDisplayName() {
        return name + " (" + host + ":" + port + ")";
    }
}
