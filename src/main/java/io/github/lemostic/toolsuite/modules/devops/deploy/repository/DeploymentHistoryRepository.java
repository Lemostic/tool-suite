package io.github.lemostic.toolsuite.modules.devops.deploy.repository;

import io.github.lemostic.toolsuite.modules.devops.deploy.entity.DeploymentHistoryEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 部署历史 Repository
 */
public class DeploymentHistoryRepository extends AbstractBaseRepository<DeploymentHistoryEntity, String> {
    
    public DeploymentHistoryRepository(EntityManager entityManager) {
        super(entityManager, DeploymentHistoryEntity.class);
    }
    
    @Override
    protected boolean isNew(DeploymentHistoryEntity entity) {
        return entity.getId() == null;
    }
    
    /**
     * 根据服务器ID查询历史记录
     */
    public List<DeploymentHistoryEntity> findByServerId(Long serverId) {
        TypedQuery<DeploymentHistoryEntity> query = entityManager.createQuery(
            "SELECT h FROM DeploymentHistoryEntity h WHERE h.serverId = :serverId ORDER BY h.createdAt DESC",
            DeploymentHistoryEntity.class
        );
        query.setParameter("serverId", serverId);
        return query.getResultList();
    }
    
    /**
     * 查询最近的部署历史
     */
    public List<DeploymentHistoryEntity> findRecent(int limit) {
        TypedQuery<DeploymentHistoryEntity> query = entityManager.createQuery(
            "SELECT h FROM DeploymentHistoryEntity h ORDER BY h.createdAt DESC",
            DeploymentHistoryEntity.class
        );
        query.setMaxResults(limit);
        return query.getResultList();
    }
    
    /**
     * 根据状态查询
     */
    public List<DeploymentHistoryEntity> findByStatus(DeploymentHistoryEntity.DeploymentStatus status) {
        TypedQuery<DeploymentHistoryEntity> query = entityManager.createQuery(
            "SELECT h FROM DeploymentHistoryEntity h WHERE h.status = :status ORDER BY h.createdAt DESC",
            DeploymentHistoryEntity.class
        );
        query.setParameter("status", status);
        return query.getResultList();
    }
    
    /**
     * 查询时间范围内的部署记录
     */
    public List<DeploymentHistoryEntity> findByTimeRange(LocalDateTime start, LocalDateTime end) {
        TypedQuery<DeploymentHistoryEntity> query = entityManager.createQuery(
            "SELECT h FROM DeploymentHistoryEntity h WHERE h.createdAt BETWEEN :start AND :end ORDER BY h.createdAt DESC",
            DeploymentHistoryEntity.class
        );
        query.setParameter("start", start);
        query.setParameter("end", end);
        return query.getResultList();
    }
    
    /**
     * 统计指定状态的数量
     */
    public long countByStatus(DeploymentHistoryEntity.DeploymentStatus status) {
        TypedQuery<Long> query = entityManager.createQuery(
            "SELECT COUNT(h) FROM DeploymentHistoryEntity h WHERE h.status = :status",
            Long.class
        );
        query.setParameter("status", status);
        return query.getSingleResult();
    }
    
    /**
     * 删除指定时间之前的记录
     */
    public int deleteBefore(LocalDateTime before) {
        return entityManager.createQuery(
            "DELETE FROM DeploymentHistoryEntity h WHERE h.createdAt < :before"
        )
        .setParameter("before", before)
        .executeUpdate();
    }
}
