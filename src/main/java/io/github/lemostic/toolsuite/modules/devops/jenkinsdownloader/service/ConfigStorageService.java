package io.github.lemostic.toolsuite.modules.devops.jenkinsdownloader.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.github.lemostic.toolsuite.modules.devops.jenkinsdownloader.model.JenkinsConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 配置存储服务
 * 用于保存和加载Jenkins配置信息
 */
public class ConfigStorageService {
    
    private static final Logger logger = LoggerFactory.getLogger(ConfigStorageService.class);
    
    private final ObjectMapper objectMapper;
    private final Path configFilePath;
    
    public ConfigStorageService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        
        // 配置文件路径: 用户目录/.toolsuite/jenkins-configs.json
        String userHome = System.getProperty("user.home");
        this.configFilePath = Paths.get(userHome, ".toolsuite", "jenkins-configs.json");
        
        // 确保目录存在
        try {
            Files.createDirectories(configFilePath.getParent());
        } catch (IOException e) {
            logger.error("无法创建配置目录", e);
        }
    }
    
    /**
     * 加载所有配置
     */
    public List<JenkinsConfig> loadConfigs() {
        File configFile = configFilePath.toFile();
        
        if (!configFile.exists()) {
            logger.info("配置文件不存在，返回空列表");
            return new ArrayList<>();
        }
        
        try {
            return objectMapper.readValue(configFile, new TypeReference<List<JenkinsConfig>>() {});
        } catch (IOException e) {
            logger.error("加载配置失败", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 保存所有配置
     */
    public boolean saveConfigs(List<JenkinsConfig> configs) {
        try {
            objectMapper.writeValue(configFilePath.toFile(), configs);
            logger.info("保存了 {} 个配置", configs.size());
            return true;
        } catch (IOException e) {
            logger.error("保存配置失败", e);
            return false;
        }
    }
    
    /**
     * 添加或更新配置
     */
    public boolean saveConfig(JenkinsConfig config) {
        List<JenkinsConfig> configs = loadConfigs();
        
        // 查找是否已存在相同配置名称的配置
        boolean found = false;
        for (int i = 0; i < configs.size(); i++) {
            if (configs.get(i).getConfigName().equals(config.getConfigName())) {
                configs.set(i, config);
                found = true;
                break;
            }
        }
        
        // 如果不存在则添加
        if (!found) {
            configs.add(config);
        }
        
        return saveConfigs(configs);
    }
    
    /**
     * 删除配置
     */
    public boolean deleteConfig(String configName) {
        List<JenkinsConfig> configs = loadConfigs();
        
        boolean removed = configs.removeIf(c -> c.getConfigName().equals(configName));
        
        if (removed) {
            return saveConfigs(configs);
        }
        
        return false;
    }
    
    /**
     * 根据名称查找配置
     */
    public JenkinsConfig findConfig(String configName) {
        List<JenkinsConfig> configs = loadConfigs();
        
        return configs.stream()
                .filter(c -> c.getConfigName().equals(configName))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * 获取配置文件路径
     */
    public Path getConfigFilePath() {
        return configFilePath;
    }
}
