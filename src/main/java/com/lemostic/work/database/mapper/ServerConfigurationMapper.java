package com.lemostic.work.database.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemostic.work.database.entity.ServerConfigurationEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 服务器配置Mapper接口
 * 遵循MVVM架构中的数据访问层设计
 * 继承MyBatis-Plus的BaseMapper，提供基础CRUD操作
 */
@Mapper
public interface ServerConfigurationMapper extends BaseMapper<ServerConfigurationEntity> {
    
    /**
     * 根据名称查找配置
     * @param name 配置名称
     * @return 服务器配置实体
     */
    @Select("SELECT * FROM server_configurations WHERE name = #{name} AND deleted = 0")
    ServerConfigurationEntity findByName(@Param("name") String name);
    
    /**
     * 根据启用状态查找配置
     * @param enabled 是否启用
     * @return 服务器配置列表
     */
    @Select("SELECT * FROM server_configurations WHERE enabled = #{enabled} AND deleted = 0 ORDER BY name")
    List<ServerConfigurationEntity> findByEnabled(@Param("enabled") Boolean enabled);
    
    /**
     * 根据主机地址查找配置
     * @param host 主机地址
     * @return 服务器配置列表
     */
    @Select("SELECT * FROM server_configurations WHERE host = #{host} AND deleted = 0 ORDER BY name")
    List<ServerConfigurationEntity> findByHost(@Param("host") String host);
    
    /**
     * 检查名称是否已存在
     * @param name 配置名称
     * @return 存在数量
     */
    @Select("SELECT COUNT(*) FROM server_configurations WHERE name = #{name} AND deleted = 0")
    int countByName(@Param("name") String name);
    
    /**
     * 获取所有启用的配置（用于下拉选择）
     * @return 启用的服务器配置列表
     */
    @Select("SELECT id, name, host, port FROM server_configurations WHERE enabled = 1 AND deleted = 0 ORDER BY name")
    List<ServerConfigurationEntity> findEnabledForSelection();
}
