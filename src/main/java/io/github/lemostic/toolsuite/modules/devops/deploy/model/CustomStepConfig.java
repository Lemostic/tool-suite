package io.github.lemostic.toolsuite.modules.devops.deploy.model;

import java.util.Map;

/**
 * 自定义部署步骤配置
 */
public class CustomStepConfig {
    private String id;
    private String name;
    private String description;
    private StepType type;
    private Map<String, Object> parameters;
    private boolean enabled;
    private int order;
    
    public enum StepType {
        SSH_COMMAND("执行SSH命令"),
        FILE_TRANSFER("文件传输"),
        SCRIPT_EXECUTION("脚本执行"),
        HEALTH_CHECK("健康检查"),
        CUSTOM_SCRIPT("自定义脚本");
        
        private final String displayName;
        
        StepType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public StepType getType() {
        return type;
    }
    
    public void setType(StepType type) {
        this.type = type;
    }
    
    public Map<String, Object> getParameters() {
        return parameters;
    }
    
    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public int getOrder() {
        return order;
    }
    
    public void setOrder(int order) {
        this.order = order;
    }
}