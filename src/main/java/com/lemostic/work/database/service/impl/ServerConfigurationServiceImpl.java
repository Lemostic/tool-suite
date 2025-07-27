package com.lemostic.work.database.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.lemostic.work.database.entity.ServerConfigurationEntity;
import com.lemostic.work.database.mapper.ServerConfigurationMapper;
import com.lemostic.work.database.service.IServerConfigurationService;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;

import java.util.List;

/**
 * 服务器配置服务实现类
 * 遵循MVVM架构中的业务服务层设计
 * 使用Spring依赖注入
 */
@Service
public class ServerConfigurationServiceImpl implements IServerConfigurationService {

    private static final Logger logger = LoggerFactory.getLogger(ServerConfigurationServiceImpl.class);

    @Autowired
    private ServerConfigurationMapper mapper;

    @Override
    public List<ServerConfigurationEntity> list() {
        if (mapper == null) {
            logger.warn("Mapper未初始化，返回空列表");
            return List.of();
        }
        try {
            return mapper.selectList(null);
        } catch (Exception e) {
            logger.error("获取所有服务器配置失败", e);
            return List.of();
        }
    }

    @Override
    public boolean save(ServerConfigurationEntity entity) {
        try {
            int result = mapper.insert(entity);
            return result > 0;
        } catch (Exception e) {
            logger.error("保存服务器配置失败: {}", entity.getName(), e);
            return false;
        }
    }

    @Override
    public boolean updateById(ServerConfigurationEntity entity) {
        try {
            int result = mapper.updateById(entity);
            return result > 0;
        } catch (Exception e) {
            logger.error("更新服务器配置失败: {}", entity.getName(), e);
            return false;
        }
    }

    @Override
    public ServerConfigurationEntity findByName(String name) {
        try {
            return mapper.findByName(name);
        } catch (Exception e) {
            logger.error("根据名称查找服务器配置失败: {}", name, e);
            return null;
        }
    }
    
    @Override
    public List<ServerConfigurationEntity> findByEnabled(Boolean enabled) {
        try {
            return mapper.findByEnabled(enabled);
        } catch (Exception e) {
            logger.error("根据启用状态查找服务器配置失败: {}", enabled, e);
            return List.of();
        }
    }
    
    @Override
    public List<ServerConfigurationEntity> findByHost(String host) {
        try {
            return mapper.findByHost(host);
        } catch (Exception e) {
            logger.error("根据主机地址查找服务器配置失败: {}", host, e);
            return List.of();
        }
    }

    @Override
    public boolean existsByName(String name) {
        try {
            return mapper.countByName(name) > 0;
        } catch (Exception e) {
            logger.error("检查服务器配置名称是否存在失败: {}", name, e);
            return false;
        }
    }
    
    @Override
    public boolean deleteByName(String name) {
        try {
            // 简化实现：直接使用物理删除
            // TODO: 实现逻辑删除
            ServerConfigurationEntity entity = findByName(name);
            if (entity != null) {
                int result = mapper.deleteById(entity.getId());
                boolean success = result > 0;
                if (success) {
                    logger.info("根据名称删除服务器配置成功: {}", name);
                }
                return success;
            }
            return false;
        } catch (Exception e) {
            logger.error("根据名称删除服务器配置失败: {}", name, e);
            return false;
        }
    }

    @Override
    public List<ServerConfigurationEntity> findEnabledForSelection() {
        try {
            return mapper.findEnabledForSelection();
        } catch (Exception e) {
            logger.error("获取启用的服务器配置失败", e);
            return List.of();
        }
    }
    
    @Override
    public boolean updateEnabled(Long id, Boolean enabled) {
        try {
            // 简化实现：先查询再更新
            ServerConfigurationEntity entity = mapper.selectById(id);
            if (entity != null) {
                entity.setEnabled(enabled);
                int result = mapper.updateById(entity);
                boolean success = result > 0;
                if (success) {
                    logger.info("更新服务器配置启用状态成功: id={}, enabled={}", id, enabled);
                }
                return success;
            }
            return false;
        } catch (Exception e) {
            logger.error("更新服务器配置启用状态失败: id={}, enabled={}", id, enabled, e);
            return false;
        }
    }
    
    @Override
    public boolean testConnection(ServerConfigurationEntity entity) {
        Session session = null;
        try {
            logger.info("开始测试服务器连接: {}@{}:{}", entity.getUsername(), entity.getHost(), entity.getPort());

            JSch jsch = new JSch();

            // 如果配置了私钥，则使用私钥认证
            if (entity.getPrivateKeyPath() != null && !entity.getPrivateKeyPath().trim().isEmpty()) {
                File privateKeyFile = new File(entity.getPrivateKeyPath());
                if (privateKeyFile.exists()) {
                    if (entity.getPrivateKeyPassphrase() != null && !entity.getPrivateKeyPassphrase().trim().isEmpty()) {
                        jsch.addIdentity(entity.getPrivateKeyPath(), entity.getPrivateKeyPassphrase());
                    } else {
                        jsch.addIdentity(entity.getPrivateKeyPath());
                    }
                    logger.debug("使用私钥认证: {}", entity.getPrivateKeyPath());
                } else {
                    logger.warn("私钥文件不存在: {}", entity.getPrivateKeyPath());
                    return false;
                }
            }

            // 创建会话
            session = jsch.getSession(entity.getUsername(), entity.getHost(), entity.getPort());

            // 如果没有私钥，使用密码认证
            if (entity.getPrivateKeyPath() == null || entity.getPrivateKeyPath().trim().isEmpty()) {
                if (entity.getPassword() != null && !entity.getPassword().trim().isEmpty()) {
                    session.setPassword(entity.getPassword());
                    logger.debug("使用密码认证");
                } else {
                    logger.warn("既没有私钥也没有密码，无法进行认证");
                    return false;
                }
            }

            // 配置会话
            session.setConfig("StrictHostKeyChecking", "no"); // 跳过主机密钥检查
            session.setTimeout(10000); // 10秒超时

            // 尝试连接
            session.connect();

            if (session.isConnected()) {
                logger.info("服务器连接测试成功: {}@{}:{}", entity.getUsername(), entity.getHost(), entity.getPort());
                return true;
            } else {
                logger.warn("服务器连接测试失败，会话未连接: {}@{}:{}", entity.getUsername(), entity.getHost(), entity.getPort());
                return false;
            }

        } catch (Exception e) {
            logger.error("测试服务器连接失败: {}@{}:{}", entity.getUsername(), entity.getHost(), entity.getPort(), e);
            return false;
        } finally {
            // 确保关闭连接
            if (session != null && session.isConnected()) {
                session.disconnect();
                logger.debug("SSH会话已关闭");
            }
        }
    }
}
