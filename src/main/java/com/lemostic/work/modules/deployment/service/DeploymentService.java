package com.lemostic.work.modules.deployment.service;

import com.lemostic.work.modules.deployment.model.DeploymentResult;
import com.lemostic.work.modules.deployment.model.DeploymentTask;
import com.lemostic.work.modules.deployment.model.ServerConfiguration;

import java.util.List;
import java.util.function.Consumer;

/**
 * 部署服务接口
 */
public interface DeploymentService {
    
    /**
     * 测试服务器连接
     */
    boolean testConnection(ServerConfiguration config, Consumer<String> messageCallback);
    
    /**
     * 执行部署任务
     */
    void executeDeployment(DeploymentTask task, Consumer<DeploymentResult> resultCallback, 
                          Consumer<String> progressCallback);
    
    /**
     * 取消部署任务
     */
    void cancelDeployment(String taskId);
    
    /**
     * 获取所有服务器配置
     */
    List<ServerConfiguration> getAllServerConfigurations();
    
    /**
     * 保存服务器配置
     */
    void saveServerConfiguration(ServerConfiguration config);
    
    /**
     * 删除服务器配置
     */
    void deleteServerConfiguration(String configName);
    
    /**
     * 获取部署历史
     */
    List<DeploymentResult> getDeploymentHistory();
    
    /**
     * 清理部署历史
     */
    void cleanupDeploymentHistory(int daysToKeep);
    
    /**
     * 设置进度更新回调
     */
    void setOnProgressUpdate(Consumer<Double> listener);
    
    /**
     * 设置状态变化回调
     */
    void setOnStatusChange(Consumer<String> listener);
    
    /**
     * 停止所有部署任务
     */
    void stopAllDeployments();
}
