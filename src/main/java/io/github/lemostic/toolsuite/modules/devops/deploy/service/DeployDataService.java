package io.github.lemostic.toolsuite.modules.devops.deploy.service;

import io.github.lemostic.toolsuite.modules.devops.deploy.entity.DeploymentHistoryEntity;
import io.github.lemostic.toolsuite.modules.devops.deploy.entity.ServerConfigEntity;
import io.github.lemostic.toolsuite.modules.devops.deploy.model.BackupOptions;
import io.github.lemostic.toolsuite.modules.devops.deploy.model.ServerConfig;
import io.github.lemostic.toolsuite.modules.devops.deploy.model.UploadOptions;
import io.github.lemostic.toolsuite.modules.devops.deploy.repository.DeploymentHistoryRepository;
import io.github.lemostic.toolsuite.modules.devops.deploy.repository.RepositoryFactory;
import io.github.lemostic.toolsuite.modules.devops.deploy.repository.ServerConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 部署数据服务
 * 封装业务逻辑，协调 Repository 操作
 */
public class DeployDataService {
    
    private static final Logger logger = LoggerFactory.getLogger(DeployDataService.class);
    
    private final ServerConfigRepository serverConfigRepository;
    private final DeploymentHistoryRepository deploymentHistoryRepository;
    
    public DeployDataService() {
        RepositoryFactory factory = RepositoryFactory.getInstance();
        this.serverConfigRepository = factory.getServerConfigRepository();
        this.deploymentHistoryRepository = factory.getDeploymentHistoryRepository();
    }
    
    // ==================== 服务器配置服务 ====================
    
    /**
     * 加载所有服务器配置
     */
    public List<ServerConfig> loadAllServers() {
        List<ServerConfigEntity> entities = serverConfigRepository.findAll();
        return entities.stream()
            .map(this::convertToModel)
            .collect(Collectors.toList());
    }
    
    /**
     * 加载所有启用的服务器
     */
    public List<ServerConfig> loadEnabledServers() {
        List<ServerConfigEntity> entities = serverConfigRepository.findAllEnabled();
        return entities.stream()
            .map(this::convertToModel)
            .collect(Collectors.toList());
    }
    
    /**
     * 根据ID查询服务器
     */
    public ServerConfig findServerById(Long id) {
        return serverConfigRepository.findById(id)
            .map(this::convertToModel)
            .orElse(null);
    }
    
    /**
     * 保存服务器配置
     */
    public ServerConfig saveServer(ServerConfig config) {
        ServerConfigEntity entity = convertToEntity(config);
        ServerConfigEntity saved = serverConfigRepository.save(entity);
        
        // 更新模型的ID
        config.setId(saved.getId());
        
        logger.info("保存服务器配置成功: {}", config.getName());
        return config;
    }
    
    /**
     * 删除服务器配置
     */
    public void deleteServer(Long id) {
        serverConfigRepository.deleteById(id);
        logger.info("删除服务器配置成功: {}", id);
    }
    
    /**
     * 检查服务器名称是否已存在
     */
    public boolean isServerNameExists(String name, Long excludeId) {
        return serverConfigRepository.existsByNameAndNotId(name, excludeId);
    }
    
    // ==================== 部署历史服务 ====================
    
    /**
     * 保存部署历史
     */
    public void saveDeploymentHistory(DeploymentHistoryEntity history) {
        deploymentHistoryRepository.save(history);
    }
    
    /**
     * 加载最近的部署历史
     */
    public List<DeploymentHistoryEntity> loadRecentHistory(int limit) {
        return deploymentHistoryRepository.findRecent(limit);
    }
    
    /**
     * 根据服务器ID查询历史
     */
    public List<DeploymentHistoryEntity> loadHistoryByServer(Long serverId) {
        return deploymentHistoryRepository.findByServerId(serverId);
    }
    
    /**
     * 创建部署历史记录
     */
    public DeploymentHistoryEntity createHistory(Long serverId, String serverName, 
                                                  String packageName,
                                                  DeploymentHistoryEntity.DeploymentStatus status) {
        DeploymentHistoryEntity history = new DeploymentHistoryEntity();
        history.setId(java.util.UUID.randomUUID().toString());
        history.setServerId(serverId);
        history.setServerName(serverName);
        history.setPackageName(packageName);
        history.setStatus(status);
        
        return deploymentHistoryRepository.save(history);
    }
    
    /**
     * 更新部署状态
     */
    public void updateHistoryStatus(String historyId, 
                                     DeploymentHistoryEntity.DeploymentStatus status,
                                     String errorMessage) {
        deploymentHistoryRepository.findById(historyId).ifPresent(history -> {
            history.setStatus(status);
            history.setErrorMessage(errorMessage);
            deploymentHistoryRepository.save(history);
        });
    }
    
    // ==================== 实体与模型转换 ====================
    
    private ServerConfig convertToModel(ServerConfigEntity entity) {
        ServerConfig model = new ServerConfig();
        model.setId(entity.getId());
        model.setName(entity.getName());
        model.setHost(entity.getHost());
        model.setPort(entity.getPort());
        model.setUsername(entity.getUsername());
        model.setPassword(entity.getPassword());
        model.setAppDirectory(entity.getAppDirectory());
        model.setBackupDirectory(entity.getBackupDirectory());
        model.setBinDirectory(entity.getBinDirectory());
        model.setStopScript(entity.getStopScript());
        model.setStartScript(entity.getStartScript());
        model.setDescription(entity.getDescription());
        model.setEnabled(entity.getEnabled());
        model.setCreatedAt(entity.getCreatedAt());
        model.setUpdatedAt(entity.getUpdatedAt());
        return model;
    }
    
    private ServerConfigEntity convertToEntity(ServerConfig model) {
        ServerConfigEntity entity = new ServerConfigEntity();
        entity.setId(model.getId());
        entity.setName(model.getName());
        entity.setHost(model.getHost());
        entity.setPort(model.getPort());
        entity.setUsername(model.getUsername());
        entity.setPassword(model.getPassword());
        entity.setAppDirectory(model.getAppDirectory());
        entity.setBackupDirectory(model.getBackupDirectory());
        entity.setBinDirectory(model.getBinDirectory());
        entity.setStopScript(model.getStopScript());
        entity.setStartScript(model.getStartScript());
        entity.setDescription(model.getDescription());
        entity.setEnabled(model.getEnabled());
        entity.setCreatedAt(model.getCreatedAt());
        entity.setUpdatedAt(model.getUpdatedAt());
        return entity;
    }
}
