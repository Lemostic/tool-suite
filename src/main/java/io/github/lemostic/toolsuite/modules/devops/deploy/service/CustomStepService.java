package io.github.lemostic.toolsuite.modules.devops.deploy.service;

import io.github.lemostic.toolsuite.modules.devops.deploy.model.CustomStepConfig;
import io.github.lemostic.toolsuite.modules.devops.deploy.task.TaskContext;
import io.github.lemostic.toolsuite.modules.devops.deploy.task.TaskResult;
import io.github.lemostic.toolsuite.modules.devops.deploy.task.TaskStep;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 自定义步骤管理服务
 */
public class CustomStepService {
    private static final Logger logger = LoggerFactory.getLogger(CustomStepService.class);
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String configFilePath = System.getProperty("user.home") + 
                                         File.separator + ".toolsuite" + 
                                         File.separator + "custom-steps.json";
    
    private List<CustomStepConfig> customSteps = new ArrayList<>();
    
    public CustomStepService() {
        loadCustomSteps();
    }
    
    /**
     * 加载自定义步骤配置
     */
    public void loadCustomSteps() {
        try {
            File configFile = new File(configFilePath);
            if (configFile.exists()) {
                String json = Files.readString(Paths.get(configFilePath));
                customSteps = objectMapper.readValue(json, new TypeReference<List<CustomStepConfig>>() {});
                logger.info("加载了 {} 个自定义步骤", customSteps.size());
            } else {
                // 创建默认步骤
                createDefaultSteps();
                saveCustomSteps();
            }
        } catch (IOException e) {
            logger.error("加载自定义步骤配置失败", e);
            createDefaultSteps();
        }
    }
    
    /**
     * 保存自定义步骤配置
     */
    public void saveCustomSteps() {
        try {
            // 确保目录存在
            File configFile = new File(configFilePath);
            configFile.getParentFile().mkdirs();
            
            String json = objectMapper.writeValueAsString(customSteps);
            Files.writeString(Paths.get(configFilePath), json);
            logger.info("自定义步骤配置已保存");
        } catch (IOException e) {
            logger.error("保存自定义步骤配置失败", e);
        }
    }
    
    /**
     * 获取所有自定义步骤
     */
    public List<CustomStepConfig> getAllSteps() {
        return new ArrayList<>(customSteps);
    }
    
    /**
     * 获取启用的自定义步骤
     */
    public List<CustomStepConfig> getEnabledSteps() {
        return customSteps.stream()
                .filter(CustomStepConfig::isEnabled)
                .sorted(Comparator.comparingInt(CustomStepConfig::getOrder))
                .collect(Collectors.toList());
    }
    
    /**
     * 添加自定义步骤
     */
    public void addStep(CustomStepConfig step) {
        if (step.getId() == null) {
            step.setId(UUID.randomUUID().toString());
        }
        customSteps.add(step);
        saveCustomSteps();
    }
    
    /**
     * 更新自定义步骤
     */
    public void updateStep(CustomStepConfig step) {
        for (int i = 0; i < customSteps.size(); i++) {
            if (customSteps.get(i).getId().equals(step.getId())) {
                customSteps.set(i, step);
                saveCustomSteps();
                return;
            }
        }
    }
    
    /**
     * 删除自定义步骤
     */
    public void deleteStep(String stepId) {
        customSteps.removeIf(step -> step.getId().equals(stepId));
        saveCustomSteps();
    }
    
    /**
     * 创建任务步骤实例
     */
    public List<TaskStep> createTaskSteps(List<CustomStepConfig> configs) {
        return configs.stream()
                .map(this::createTaskStep)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
    
    /**
     * 根据配置创建单个任务步骤
     */
    private TaskStep createTaskStep(CustomStepConfig config) {
        switch (config.getType()) {
            case SSH_COMMAND:
                return new SshCommandStep(config);
            case FILE_TRANSFER:
                return new FileTransferStep(config);
            case SCRIPT_EXECUTION:
                return new ScriptExecutionStep(config);
            case HEALTH_CHECK:
                return new HealthCheckStep(config);
            case CUSTOM_SCRIPT:
                return new CustomScriptStep(config);
            default:
                logger.warn("未知的步骤类型: {}", config.getType());
                return null;
        }
    }
    
    /**
     * 创建默认步骤
     */
    private void createDefaultSteps() {
        customSteps.clear();
        
        // 默认健康检查步骤
        CustomStepConfig healthCheck = new CustomStepConfig();
        healthCheck.setId("health-check-1");
        healthCheck.setName("服务健康检查");
        healthCheck.setDescription("检查服务是否正常运行");
        healthCheck.setType(CustomStepConfig.StepType.HEALTH_CHECK);
        healthCheck.setEnabled(true);
        healthCheck.setOrder(1);
        healthCheck.setParameters(Map.of(
            "checkCommand", "ps aux | grep java",
            "timeoutSeconds", 30
        ));
        
        // 默认磁盘空间检查步骤
        CustomStepConfig diskCheck = new CustomStepConfig();
        diskCheck.setId("disk-check-1");
        diskCheck.setName("磁盘空间检查");
        diskCheck.setDescription("检查磁盘剩余空间");
        diskCheck.setType(CustomStepConfig.StepType.SSH_COMMAND);
        diskCheck.setEnabled(true);
        diskCheck.setOrder(2);
        diskCheck.setParameters(Map.of(
            "command", "df -h",
            "expectedOutput", ".*[8-9][0-9]%.*"
        ));
        
        customSteps.add(healthCheck);
        customSteps.add(diskCheck);
    }
    
    /**
     * SSH命令执行步骤
     */
    private static class SshCommandStep implements TaskStep {
        private final CustomStepConfig config;
        
        public SshCommandStep(CustomStepConfig config) {
            this.config = config;
        }
        
        @Override
        public String getName() {
            return config.getName();
        }
        
        @Override
        public String getDescription() {
            return config.getDescription();
        }
        
        @Override
        public TaskResult execute(TaskContext context, Consumer<String> logConsumer) {
            try {
                String command = (String) config.getParameters().get("command");
                logConsumer.accept("执行SSH命令: " + command);
                
                // 这里应该调用实际的SSH服务
                // SshCommandService sshService = (SshCommandService) context.getAttribute("sshService");
                // String result = sshService.executeCommand(command);
                
                // 模拟执行
                Thread.sleep(2000);
                logConsumer.accept("命令执行完成");
                
                return TaskResult.success("SSH命令执行成功");
            } catch (Exception e) {
                logConsumer.accept("命令执行失败: " + e.getMessage());
                return TaskResult.failure("SSH命令执行失败: " + e.getMessage());
            }
        }
    }
    
    /**
     * 文件传输步骤
     */
    private static class FileTransferStep implements TaskStep {
        private final CustomStepConfig config;
        
        public FileTransferStep(CustomStepConfig config) {
            this.config = config;
        }
        
        @Override
        public String getName() {
            return config.getName();
        }
        
        @Override
        public String getDescription() {
            return config.getDescription();
        }
        
        @Override
        public TaskResult execute(TaskContext context, Consumer<String> logConsumer) {
            try {
                String source = (String) config.getParameters().get("source");
                String target = (String) config.getParameters().get("target");
                
                logConsumer.accept("传输文件: " + source + " -> " + target);
                
                // 模拟文件传输
                Thread.sleep(3000);
                logConsumer.accept("文件传输完成");
                
                return TaskResult.success("文件传输成功");
            } catch (Exception e) {
                logConsumer.accept("文件传输失败: " + e.getMessage());
                return TaskResult.failure("文件传输失败: " + e.getMessage());
            }
        }
    }
    
    /**
     * 脚本执行步骤
     */
    private static class ScriptExecutionStep implements TaskStep {
        private final CustomStepConfig config;
        
        public ScriptExecutionStep(CustomStepConfig config) {
            this.config = config;
        }
        
        @Override
        public String getName() {
            return config.getName();
        }
        
        @Override
        public String getDescription() {
            return config.getDescription();
        }
        
        @Override
        public TaskResult execute(TaskContext context, Consumer<String> logConsumer) {
            try {
                String scriptPath = (String) config.getParameters().get("scriptPath");
                logConsumer.accept("执行脚本: " + scriptPath);
                
                // 模拟脚本执行
                Thread.sleep(2500);
                logConsumer.accept("脚本执行完成");
                
                return TaskResult.success("脚本执行成功");
            } catch (Exception e) {
                logConsumer.accept("脚本执行失败: " + e.getMessage());
                return TaskResult.failure("脚本执行失败: " + e.getMessage());
            }
        }
    }
    
    /**
     * 健康检查步骤
     */
    private static class HealthCheckStep implements TaskStep {
        private final CustomStepConfig config;
        
        public HealthCheckStep(CustomStepConfig config) {
            this.config = config;
        }
        
        @Override
        public String getName() {
            return config.getName();
        }
        
        @Override
        public String getDescription() {
            return config.getDescription();
        }
        
        @Override
        public TaskResult execute(TaskContext context, Consumer<String> logConsumer) {
            try {
                String checkCommand = (String) config.getParameters().get("checkCommand");
                Integer timeout = (Integer) config.getParameters().getOrDefault("timeoutSeconds", 30);
                
                logConsumer.accept("执行健康检查: " + checkCommand);
                
                // 模拟健康检查
                Thread.sleep(Math.min(timeout * 100, 5000));
                
                // 简单的成功率模拟
                boolean success = Math.random() > 0.1; // 90% 成功率
                
                if (success) {
                    logConsumer.accept("健康检查通过");
                    return TaskResult.success("健康检查通过");
                } else {
                    logConsumer.accept("健康检查失败");
                    return TaskResult.failure("健康检查失败");
                }
            } catch (Exception e) {
                logConsumer.accept("健康检查异常: " + e.getMessage());
                return TaskResult.failure("健康检查异常: " + e.getMessage());
            }
        }
    }
    
    /**
     * 自定义脚本步骤
     */
    private static class CustomScriptStep implements TaskStep {
        private final CustomStepConfig config;
        
        public CustomScriptStep(CustomStepConfig config) {
            this.config = config;
        }
        
        @Override
        public String getName() {
            return config.getName();
        }
        
        @Override
        public String getDescription() {
            return config.getDescription();
        }
        
        @Override
        public TaskResult execute(TaskContext context, Consumer<String> logConsumer) {
            try {
                String scriptContent = (String) config.getParameters().get("scriptContent");
                logConsumer.accept("执行自定义脚本");
                logConsumer.accept("脚本内容: " + scriptContent.substring(0, Math.min(100, scriptContent.length())) + "...");
                
                // 模拟自定义脚本执行
                Thread.sleep(4000);
                logConsumer.accept("自定义脚本执行完成");
                
                return TaskResult.success("自定义脚本执行成功");
            } catch (Exception e) {
                logConsumer.accept("自定义脚本执行失败: " + e.getMessage());
                return TaskResult.failure("自定义脚本执行失败: " + e.getMessage());
            }
        }
    }
}