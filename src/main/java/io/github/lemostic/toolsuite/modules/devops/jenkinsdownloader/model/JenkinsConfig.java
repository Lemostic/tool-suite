package io.github.lemostic.toolsuite.modules.devops.jenkinsdownloader.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.util.Base64;

/**
 * Jenkins配置实体类
 * 用于保存Jenkins服务器连接信息和认证凭据
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JenkinsConfig implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Jenkins服务器基础URL
     * 例如: http://jenkins.example.com:8080
     */
    private String baseUrl;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * API Token或密码
     */
    private String apiToken;
    
    /**
     * 配置名称（用于显示）
     */
    private String configName;
    
    /**
     * 获取Basic Auth认证头
     */
    public String getAuthHeader() {
        if (username == null || apiToken == null) {
            return null;
        }
        String credentials = username + ":" + apiToken;
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes());
    }
    
    /**
     * 验证配置是否完整
     */
    public boolean isValid() {
        return baseUrl != null && !baseUrl.trim().isEmpty()
            && username != null && !username.trim().isEmpty()
            && apiToken != null && !apiToken.trim().isEmpty();
    }
    
    /**
     * 获取标准化的基础URL
     */
    public String getNormalizedBaseUrl() {
        if (baseUrl == null) return "";
        String url = baseUrl.trim();
        // 移除末尾的斜杠
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }
}
