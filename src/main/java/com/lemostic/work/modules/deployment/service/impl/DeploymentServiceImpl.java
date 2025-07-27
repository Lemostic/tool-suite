package com.lemostic.work.modules.deployment.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;

import com.lemostic.work.database.entity.ServerConfigurationEntity;
import com.lemostic.work.database.entity.DeploymentHistoryEntity;
import com.lemostic.work.database.service.IServerConfigurationService;
import com.lemostic.work.database.service.IDeploymentHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.jcraft.jsch.Session;
import com.lemostic.work.modules.deployment.model.DeploymentResult;
import com.lemostic.work.modules.deployment.model.DeploymentTask;
import com.lemostic.work.modules.deployment.model.ServerConfiguration;
import com.lemostic.work.modules.deployment.service.DeploymentService;
import com.lemostic.work.modules.deployment.util.LoggingUtil;
import com.lemostic.work.modules.deployment.util.SSHUtil;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

/**
 * 部署服务实现类
 */
@Service
public class DeploymentServiceImpl implements DeploymentService {

    private static final Logger logger = LoggerFactory.getLogger(DeploymentServiceImpl.class);

    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final ConcurrentHashMap<String, Future<?>> runningTasks = new ConcurrentHashMap<>();

    private Consumer<Double> progressListener;
    private Consumer<String> statusListener;

    // 数据库服务 - 通过Spring上下文获取
    @Autowired
    private IServerConfigurationService serverConfigurationService;
    @Autowired
    private IDeploymentHistoryService deploymentHistoryService;
    
    public DeploymentServiceImpl() {
        // 初始化日志目录
        LoggingUtil.initLogDirectories();

        // 清理过期日志
        LoggingUtil.cleanupOldLogs();

        logger.info("部署服务初始化完成");
    }



    // 临时方法：将实体转换为模型
    private ServerConfiguration entityToModel(ServerConfigurationEntity entity) {
        if (entity == null) return null;

        ServerConfiguration model = new ServerConfiguration();
        model.setName(entity.getName());
        model.setHost(entity.getHost());
        model.setPort(entity.getPort());
        model.setUsername(entity.getUsername());
        model.setPassword(entity.getPassword());
        model.setPrivateKeyPath(entity.getPrivateKeyPath());
        model.setPrivateKeyPassphrase(entity.getPrivateKeyPassphrase());
        model.setUploadDirectory(entity.getUploadDirectory());
        model.setInstallationDirectory(entity.getInstallationDirectory());
        model.setBackupDirectory(entity.getBackupDirectory());
        model.setEnabled(entity.getEnabled());
        model.setDescription(entity.getDescription());
        return model;
    }

    // 临时方法：将模型转换为实体
    private ServerConfigurationEntity modelToEntity(ServerConfiguration model) {
        if (model == null) return null;

        ServerConfigurationEntity entity = new ServerConfigurationEntity();
        entity.setName(model.getName());
        entity.setHost(model.getHost());
        entity.setPort(model.getPort());
        entity.setUsername(model.getUsername());
        entity.setPassword(model.getPassword());
        entity.setPrivateKeyPath(model.getPrivateKeyPath());
        entity.setPrivateKeyPassphrase(model.getPrivateKeyPassphrase());
        entity.setUploadDirectory(model.getUploadDirectory());
        entity.setInstallationDirectory(model.getInstallationDirectory());
        entity.setBackupDirectory(model.getBackupDirectory());
        entity.setEnabled(model.isEnabled());
        entity.setDescription(model.getDescription());
        return entity;
    }

    // 将部署历史实体转换为部署结果模型
    private DeploymentResult entityToDeploymentResult(DeploymentHistoryEntity entity) {
        if (entity == null) return null;

        DeploymentResult result = new DeploymentResult();
        result.setTaskId(entity.getTaskId());

        // 创建服务器配置对象
        ServerConfiguration serverConfig = new ServerConfiguration();
        serverConfig.setName(entity.getServerName());
        serverConfig.setHost(entity.getServerHost());
        result.setServerConfiguration(serverConfig);

        result.setSuccess(entity.getSuccess());
        result.setErrorMessage(entity.getErrorMessage());
        result.setStartTime(entity.getStartTime());
        result.setEndTime(entity.getEndTime());
        result.setBackupFilePath(entity.getBackupFilePath());

        // 解析部署文件列表（从JSON字符串）
        if (StrUtil.isNotBlank(entity.getDeployedFiles())) {
            try {
                List<String> deployedFiles = JSONUtil.toList(entity.getDeployedFiles(), String.class);
                result.setDeployedFiles(deployedFiles);
            } catch (Exception e) {
                logger.warn("解析部署文件列表失败: {}", entity.getDeployedFiles(), e);
                result.setDeployedFiles(new ArrayList<String>());
            }
        } else {
            result.setDeployedFiles(new ArrayList<String>());
        }

        return result;
    }
    
    @Override
    public boolean testConnection(ServerConfiguration config, Consumer<String> messageCallback) {
        try {
            if (messageCallback != null) {
                messageCallback.accept("正在连接到服务器: " + config.getHost());
            }

            Session session = SSHUtil.createSession(config);
            session.connect(10000); // 10秒超时

            if (session.isConnected()) {
                if (messageCallback != null) {
                    messageCallback.accept("连接成功!");
                }

                session.disconnect();
                return true;
            } else {
                if (messageCallback != null) {
                    messageCallback.accept("连接失败");
                }
                return false;
            }

        } catch (Exception e) {
            logger.error("测试连接失败", e);
            if (messageCallback != null) {
                messageCallback.accept("连接失败: " + e.getMessage());
            }
            return false;
        }
    }


    
    @Override
    public void executeDeployment(DeploymentTask task, Consumer<DeploymentResult> resultCallback, 
                                 Consumer<String> progressCallback) {
        
        String taskId = task.getTaskId();
        if (StrUtil.isBlank(taskId)) {
            taskId = IdUtil.simpleUUID();
            task.setTaskId(taskId);
        }
        
        final String finalTaskId = taskId;
        
        Future<?> future = executorService.submit(() -> {
            task.setStatus(DeploymentTask.TaskStatus.RUNNING);
            task.setStartTime(LocalDateTime.now());
            
            LoggingUtil.writeDeploymentLog(finalTaskId, LoggingUtil.LogLevel.INFO, 
                                         "开始执行部署任务: " + task.getDescription(), progressCallback);
            
            for (ServerConfiguration serverConfig : task.getTargetServers()) {
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }
                
                DeploymentResult result = deployToServer(task, serverConfig, progressCallback);
                
                // 保存结果到历史记录
                saveDeploymentResult(result, task);
                
                // 通知结果
                if (resultCallback != null) {
                    Platform.runLater(() -> resultCallback.accept(result));
                }
            }
            
            task.setStatus(DeploymentTask.TaskStatus.COMPLETED);
            task.setEndTime(LocalDateTime.now());
            
            LoggingUtil.writeDeploymentLog(finalTaskId, LoggingUtil.LogLevel.INFO, 
                                         "部署任务完成", progressCallback);
            
            runningTasks.remove(finalTaskId);
        });
        
        runningTasks.put(finalTaskId, future);
    }
    
    private DeploymentResult deployToServer(DeploymentTask task, ServerConfiguration serverConfig, 
                                          Consumer<String> progressCallback) {
        DeploymentResult result = new DeploymentResult();
        result.setTaskId(task.getTaskId());
        result.setServerConfiguration(serverConfig);
        result.setStartTime(LocalDateTime.now());
        
        Session session = null;
        
        try {
            // 连接到服务器
            result.addLogMessage("连接到服务器: " + serverConfig.getHost());
            session = SSHUtil.createSession(serverConfig);
            session.connect(30000);
            
            if (!session.isConnected()) {
                throw new RuntimeException("无法连接到服务器");
            }

            // 创建必要的目录
            result.addLogMessage("检查并创建必要的目录...");
            createRequiredDirectories(session, serverConfig, progressCallback);

            // 创建备份
            if (task.isCreateBackup()) {
                result.addLogMessage("创建备份...");
                String backupPath = SSHUtil.createBackup(session, 
                                                       serverConfig.getInstallationDirectory(),
                                                       serverConfig.getBackupDirectory(),
                                                       progressCallback);
                result.setBackupFilePath(backupPath);
            }
            
            // 上传包文件
            File localPackage = new File(task.getLocalPackagePath());
            String remotePackagePath = serverConfig.getUploadDirectory() + "/" + localPackage.getName();
            
            result.addLogMessage("上传包文件: " + localPackage.getName());
            SSHUtil.uploadFile(session, task.getLocalPackagePath(), remotePackagePath, progressCallback);
            result.setUploadedFileSize(localPackage.length());
            
            // 解压包文件
            result.addLogMessage("解压包文件...");
            String extractDir = serverConfig.getUploadDirectory() + "/extracted_" + System.currentTimeMillis();

            // 根据文件扩展名选择解压命令
            String fileName = localPackage.getName().toLowerCase();
            String extractCommand;

            if (fileName.endsWith(".zip")) {
                extractCommand = String.format("mkdir -p %s && cd %s && unzip -q %s",
                                             extractDir, extractDir, remotePackagePath);
            } else if (fileName.endsWith(".tar.gz") || fileName.endsWith(".tgz")) {
                extractCommand = String.format("mkdir -p %s && cd %s && tar -xzf %s",
                                             extractDir, extractDir, remotePackagePath);
            } else if (fileName.endsWith(".tar")) {
                extractCommand = String.format("mkdir -p %s && cd %s && tar -xf %s",
                                             extractDir, extractDir, remotePackagePath);
            } else {
                // 默认尝试tar.gz
                extractCommand = String.format("mkdir -p %s && cd %s && tar -xzf %s",
                                             extractDir, extractDir, remotePackagePath);
            }

            SSHUtil.executeCommand(session, extractCommand, progressCallback);
            
            // 移动文件到安装目录
            for (String fileToMove : task.getFilesToMove()) {
                result.addLogMessage("移动文件: " + fileToMove);
                String sourceFile = extractDir + "/" + fileToMove;
                String targetFile = serverConfig.getInstallationDirectory() + "/" + fileToMove;

                // 确保目标文件的父目录存在
                String targetDir = targetFile.substring(0, targetFile.lastIndexOf('/'));
                if (!targetDir.equals(serverConfig.getInstallationDirectory())) {
                    String createDirCommand = String.format("mkdir -p %s", targetDir);
                    SSHUtil.executeCommand(session, createDirCommand, progressCallback);
                }

                // 复制文件/目录
                String moveCommand = String.format("cp -r %s %s", sourceFile, targetFile);
                SSHUtil.executeCommand(session, moveCommand, progressCallback);
                result.addDeployedFile(targetFile);
            }
            
            // 清理临时文件
            if (task.isCleanupAfterDeployment()) {
                result.addLogMessage("清理临时文件...");
                String cleanupCommand = String.format("rm -rf %s %s", extractDir, remotePackagePath);
                SSHUtil.executeCommand(session, cleanupCommand, progressCallback);
            }
            
            result.setSuccess(true);
            result.addLogMessage("部署成功完成");
            
        } catch (Exception e) {
            logger.error("部署到服务器失败: " + serverConfig.getHost(), e);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            result.addLogMessage("部署失败: " + e.getMessage());
            
            LoggingUtil.writeDeploymentLog(task.getTaskId(), LoggingUtil.LogLevel.ERROR, 
                                         "部署到服务器失败: " + serverConfig.getHost() + " - " + e.getMessage(), 
                                         progressCallback);
        } finally {
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
            result.setEndTime(LocalDateTime.now());
        }
        
        return result;
    }

    /**
     * 创建部署所需的目录
     */
    private void createRequiredDirectories(Session session, ServerConfiguration serverConfig,
                                         Consumer<String> progressCallback) throws Exception {
        // 创建上传目录
        if (!SSHUtil.checkRemoteDirectoryExists(session, serverConfig.getUploadDirectory(), progressCallback)) {
            SSHUtil.createRemoteDirectory(session, serverConfig.getUploadDirectory(), progressCallback);
        }

        // 创建安装目录
        if (!SSHUtil.checkRemoteDirectoryExists(session, serverConfig.getInstallationDirectory(), progressCallback)) {
            SSHUtil.createRemoteDirectory(session, serverConfig.getInstallationDirectory(), progressCallback);
        }

        // 创建备份目录
        if (!SSHUtil.checkRemoteDirectoryExists(session, serverConfig.getBackupDirectory(), progressCallback)) {
            SSHUtil.createRemoteDirectory(session, serverConfig.getBackupDirectory(), progressCallback);
        }

        // 确保目录权限正确（分别设置每个目录）
        try {
            String[] directories = {
                serverConfig.getUploadDirectory(),
                serverConfig.getInstallationDirectory(),
                serverConfig.getBackupDirectory()
            };

            for (String dir : directories) {
                try {
                    String chmodCommand = String.format("chmod 755 %s", dir);
                    SSHUtil.executeCommand(session, chmodCommand, null);
                } catch (Exception e) {
                    // 单个目录权限设置失败不影响其他目录
                    if (progressCallback != null) {
                        progressCallback.accept("警告: 无法设置目录权限 " + dir + " - " + e.getMessage());
                    }
                }
            }

            if (progressCallback != null) {
                progressCallback.accept("目录权限设置完成");
            }
        } catch (Exception e) {
            if (progressCallback != null) {
                progressCallback.accept("警告: 目录权限设置过程中出现问题 - " + e.getMessage());
            }
        }
    }

    @Override
    public void cancelDeployment(String taskId) {
        Future<?> future = runningTasks.get(taskId);
        if (future != null) {
            future.cancel(true);
            runningTasks.remove(taskId);
            LoggingUtil.writeDeploymentLog(taskId, LoggingUtil.LogLevel.INFO, "部署任务已取消", null);
        }
    }
    
    @Override
    public List<ServerConfiguration> getAllServerConfigurations() {

        try {
            List<ServerConfigurationEntity> entities = serverConfigurationService.list();
            List<ServerConfiguration> result = new ArrayList<ServerConfiguration>();
            for (ServerConfigurationEntity entity : entities) {
                result.add(entityToModel(entity));
            }
            logger.info("获取到 {} 个服务器配置", result.size());
            return result;
        } catch (Exception e) {
            logger.error("获取所有服务器配置失败", e);
            return new ArrayList<ServerConfiguration>();
        }
    }

    @Override
    public void saveServerConfiguration(ServerConfiguration config) {
        if (serverConfigurationService == null) {
            logger.warn("数据库服务不可用，无法保存服务器配置: {}", config.getName());
            return;
        }

        try {
            // 检查是否已存在同名配置
            ServerConfigurationEntity existingEntity = serverConfigurationService.findByName(config.getName());

            if (existingEntity != null) {
                // 更新现有配置
                existingEntity.setHost(config.getHost());
                existingEntity.setPort(config.getPort());
                existingEntity.setUsername(config.getUsername());
                existingEntity.setPassword(config.getPassword());
                existingEntity.setPrivateKeyPath(config.getPrivateKeyPath());
                existingEntity.setPrivateKeyPassphrase(config.getPrivateKeyPassphrase());
                existingEntity.setUploadDirectory(config.getUploadDirectory());
                existingEntity.setInstallationDirectory(config.getInstallationDirectory());
                existingEntity.setBackupDirectory(config.getBackupDirectory());
                existingEntity.setEnabled(config.isEnabled());
                existingEntity.setDescription(config.getDescription());

                serverConfigurationService.updateById(existingEntity);
                logger.info("更新服务器配置成功: {}", config.getName());
            } else {
                // 创建新配置
                ServerConfigurationEntity entity = modelToEntity(config);
                serverConfigurationService.save(entity);
                logger.info("保存新服务器配置成功: {}", config.getName());
            }
        } catch (Exception e) {
            logger.error("保存服务器配置失败: {}", config.getName(), e);
            throw new RuntimeException("保存服务器配置失败", e);
        }
    }

    @Override
    public void deleteServerConfiguration(String configName) {
        if (serverConfigurationService == null) {
            logger.warn("数据库服务不可用，无法删除服务器配置: {}", configName);
            return;
        }

        try {
            boolean success = serverConfigurationService.deleteByName(configName);
            if (success) {
                logger.info("删除服务器配置成功: {}", configName);
            } else {
                logger.warn("删除服务器配置失败，配置不存在: {}", configName);
            }
        } catch (Exception e) {
            logger.error("删除服务器配置失败: {}", configName, e);
            throw new RuntimeException("删除服务器配置失败", e);
        }
    }

    @Override
    public List<DeploymentResult> getDeploymentHistory() {
        if (deploymentHistoryService == null) {
            logger.warn("DeploymentHistoryService不可用，返回空历史列表");
            return new ArrayList<DeploymentResult>();
        }

        try {
            List<DeploymentHistoryEntity> entities = deploymentHistoryService.findRecentDeployments(100);
            List<DeploymentResult> results = new ArrayList<DeploymentResult>();

            for (DeploymentHistoryEntity entity : entities) {
                DeploymentResult result = entityToDeploymentResult(entity);
                results.add(result);
            }

            return results;
        } catch (Exception e) {
            logger.error("获取部署历史失败", e);
            return new ArrayList<DeploymentResult>();
        }
    }

    @Override
    public void cleanupDeploymentHistory(int daysToKeep) {
        if (deploymentHistoryService == null) {
            logger.warn("DeploymentHistoryService不可用，无法清理部署历史");
            return;
        }

        try {
            int deletedCount = deploymentHistoryService.cleanupOldDeployments(daysToKeep);
            logger.info("清理部署历史完成，删除了{}条记录", deletedCount);
        } catch (Exception e) {
            logger.error("清理部署历史失败", e);
        }
    }
    
    @Override
    public void setOnProgressUpdate(Consumer<Double> listener) {
        this.progressListener = listener;
    }
    
    @Override
    public void setOnStatusChange(Consumer<String> listener) {
        this.statusListener = listener;
    }
    
    @Override
    public void stopAllDeployments() {
        runningTasks.values().forEach(future -> future.cancel(true));
        runningTasks.clear();
    }

    /**
     * 保存部署结果到数据库
     */
    private void saveDeploymentResult(DeploymentResult result, DeploymentTask task) {
        try {
            // 获取包文件信息
            String packageName = "";
            Long packageSize = 0L;

            if (task.getLocalPackagePath() != null) {
                File packageFile = new File(task.getLocalPackagePath());
                if (packageFile.exists()) {
                    packageName = packageFile.getName();
                    packageSize = packageFile.length();
                }
            }

            // 创建历史实体
            DeploymentHistoryEntity entity = new DeploymentHistoryEntity();
            entity.setTaskId(result.getTaskId());

            // 安全地获取服务器信息
            if (result.getServerConfiguration() != null) {
                entity.setServerName(result.getServerConfiguration().getName());
                entity.setServerHost(result.getServerConfiguration().getHost());
            } else {
                entity.setServerName("Unknown");
                entity.setServerHost("Unknown");
            }

            entity.setPackageName(packageName);
            entity.setPackageSize(packageSize);
            entity.setSuccess(result.isSuccess());
            entity.setErrorMessage(result.getErrorMessage());
            entity.setStartTime(result.getStartTime());
            entity.setEndTime(result.getEndTime());

            if (result.getStartTime() != null && result.getEndTime() != null) {
                Duration duration = Duration.between(result.getStartTime(), result.getEndTime());
                entity.setDurationMillis(duration.toMillis());
            }

            entity.setBackupFilePath(result.getBackupFilePath());
            entity.setDeployedFiles(JSONUtil.toJsonStr(result.getDeployedFiles()));
            entity.setTaskDescription(task.getDescription());

            // 保存到数据库
            if (deploymentHistoryService != null) {
                boolean saved = deploymentHistoryService.save(entity);
                if (saved) {
                    logger.debug("保存部署结果到数据库成功: {}", result.getTaskId());
                } else {
                    logger.warn("保存部署结果到数据库失败: {}", result.getTaskId());
                }
            } else {
                logger.warn("DeploymentHistoryService不可用，无法保存部署结果: {}", result.getTaskId());
            }

        } catch (Exception e) {
            logger.error("保存部署结果到数据库失败", e);
            // 不抛出异常，避免影响部署流程
        }
    }


}
