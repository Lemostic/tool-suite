package io.github.lemostic.toolsuite.modules.devops.deploy.dao;

import java.util.List;

/**
 * 部署历史DAO
 */
public class DeploymentHistoryDao extends BaseDao<DeploymentHistory> {
    
    public DeploymentHistoryDao() {
        super(DeploymentHistory.class);
    }
    
    /**
     * 根据服务器ID查找部署历史
     */
    public List<DeploymentHistory> findByServerId(Long serverId) {
        return findByProperty("serverId", serverId);
    }
    
    /**
     * 查找最近的部署历史
     */
    public List<DeploymentHistory> findRecent(int limit) {
        return executeQuery("FROM DeploymentHistory ORDER BY createdAt DESC", limit);
    }
    
    /**
     * 根据状态查找部署历史
     */
    public List<DeploymentHistory> findByStatus(DeploymentHistory.DeploymentStatus status) {
        return findByProperty("status", status);
    }
    
    /**
     * 查找特定服务器的最近部署历史
     */
    public List<DeploymentHistory> findRecentByServer(Long serverId, int limit) {
        String hql = "FROM DeploymentHistory WHERE serverId = :serverId ORDER BY createdAt DESC";
        return executeQuery(hql + (limit > 0 ? " LIMIT " + limit : ""), serverId);
    }
    
    /**
     * 统计各状态的部署数量
     */
    public List<Object[]> getStatusStatistics() {
        String sql = "SELECT status, COUNT(*) FROM deployment_history GROUP BY status";
        return executeNativeQuery(sql);
    }
    
    /**
     * 获取最新的部署记录
     */
    public DeploymentHistory findLatest() {
        List<DeploymentHistory> histories = executeQuery(
            "FROM DeploymentHistory ORDER BY createdAt DESC", 1);
        return histories.isEmpty() ? null : histories.get(0);
    }
    
    /**
     * 根据包名查找部署历史
     */
    public List<DeploymentHistory> findByPackageName(String packageName) {
        return findByProperty("packageName", packageName);
    }
}