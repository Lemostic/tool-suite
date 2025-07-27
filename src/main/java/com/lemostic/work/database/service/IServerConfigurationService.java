package com.lemostic.work.database.service;

import com.lemostic.work.database.entity.ServerConfigurationEntity;

import java.util.List;

/**
 * 服务器配置服务接口
 * 遵循MVVM架构中的业务服务层设计
 * 简化版本，不依赖MyBatis-Plus的IService
 */
public interface IServerConfigurationService {

    /**
     * 获取所有配置
     * @return 服务器配置列表
     */
    List<ServerConfigurationEntity> list();

    /**
     * 保存配置
     * @param entity 服务器配置实体
     * @return 是否保存成功
     */
    boolean save(ServerConfigurationEntity entity);

    /**
     * 根据ID更新配置
     * @param entity 服务器配置实体
     * @return 是否更新成功
     */
    boolean updateById(ServerConfigurationEntity entity);

    /**
     * 根据名称查找配置
     * @param name 配置名称
     * @return 服务器配置实体
     */
    ServerConfigurationEntity findByName(String name);
    
    /**
     * 根据启用状态查找配置
     * @param enabled 是否启用
     * @return 服务器配置列表
     */
    List<ServerConfigurationEntity> findByEnabled(Boolean enabled);
    
    /**
     * 根据主机地址查找配置
     * @param host 主机地址
     * @return 服务器配置列表
     */
    List<ServerConfigurationEntity> findByHost(String host);
    
    /**
     * 检查名称是否已存在
     * @param name 配置名称
     * @return 是否存在
     */
    boolean existsByName(String name);
    
    /**
     * 根据名称删除配置（逻辑删除）
     * @param name 配置名称
     * @return 是否删除成功
     */
    boolean deleteByName(String name);
    
    /**
     * 获取所有启用的配置（用于下拉选择）
     * @return 启用的服务器配置列表
     */
    List<ServerConfigurationEntity> findEnabledForSelection();
    
    /**
     * 启用/禁用配置
     * @param id 配置ID
     * @param enabled 是否启用
     * @return 是否操作成功
     */
    boolean updateEnabled(Long id, Boolean enabled);
    
    /**
     * 测试服务器连接
     * @param entity 服务器配置
     * @return 连接测试结果
     */
    boolean testConnection(ServerConfigurationEntity entity);
}
