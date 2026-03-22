package io.github.lemostic.toolsuite.modules.devops.deploy.dao;

import java.util.List;

/**
 * 服务器配置DAO
 */
public class ServerConfigDao extends BaseDao<ServerConfig> {
    
    public ServerConfigDao() {
        super(ServerConfig.class);
    }
    
    /**
     * 根据名称查找服务器配置
     */
    public ServerConfig findByName(String name) {
        List<ServerConfig> configs = findByProperty("name", name);
        return configs.isEmpty() ? null : configs.get(0);
    }
    
    /**
     * 查找启用的服务器配置
     */
    public List<ServerConfig> findEnabledConfigs() {
        return executeQuery("FROM ServerConfig WHERE enabled = true ORDER BY name");
    }
    
    /**
     * 根据主机地址查找服务器配置
     */
    public List<ServerConfig> findByHost(String host) {
        return findByProperty("host", host);
    }
    
    /**
     * 检查服务器名称是否已存在
     */
    public boolean isNameExists(String name, Long excludeId) {
        String hql = excludeId != null 
            ? "SELECT COUNT(*) FROM ServerConfig WHERE name = :name AND id != :excludeId"
            : "SELECT COUNT(*) FROM ServerConfig WHERE name = :name";
        
        org.hibernate.query.Query<Long> query = getSession().createQuery(hql, Long.class);
        query.setParameter("name", name);
        if (excludeId != null) {
            query.setParameter("excludeId", excludeId);
        }
        
        return query.uniqueResult() > 0;
    }
}