package io.github.lemostic.toolsuite.modules.devops.deploy.repository;

import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Predicate;
import io.github.lemostic.toolsuite.modules.devops.deploy.entity.DeploymentHistoryEntity;
import jakarta.persistence.EntityManager;

import java.time.LocalDateTime;
import java.util.List;

import static io.github.lemostic.toolsuite.modules.devops.deploy.entity.QDeploymentHistoryEntity.deploymentHistoryEntity;

/**
 * 部署历史 Repository
 * 使用 QueryDSL 实现类型安全的查询
 */
public class DeploymentHistoryRepository extends QueryDslRepository<DeploymentHistoryEntity, String> {
    
    public DeploymentHistoryRepository(EntityManager entityManager) {
        super(entityManager, DeploymentHistoryEntity.class);
    }
    
    @Override
    protected EntityPath<DeploymentHistoryEntity> getEntityPath() {
        return deploymentHistoryEntity;
    }
    
    @Override
    protected boolean isNew(DeploymentHistoryEntity entity) {
        return entity.getId() == null;
    }
    
    /**
     * 根据服务器ID查询历史记录
     */
    public List<DeploymentHistoryEntity> findByServerId(Long serverId) {
        return findAll(
            deploymentHistoryEntity.serverId.eq(serverId),
            deploymentHistoryEntity.createdAt.desc()
        );
    }
    
    /**
     * 查询最近的部署历史
     */
    public List<DeploymentHistoryEntity> findRecent(int limit) {
        return getQueryFactory()
            .selectFrom(deploymentHistoryEntity)
            .orderBy(deploymentHistoryEntity.createdAt.desc())
            .limit(limit)
            .fetch();
    }
    
    /**
     * 根据状态查询
     */
    public List<DeploymentHistoryEntity> findByStatus(DeploymentHistoryEntity.DeploymentStatus status) {
        return findAll(
            deploymentHistoryEntity.status.eq(status),
            deploymentHistoryEntity.createdAt.desc()
        );
    }
    
    /**
     * 查询时间范围内的部署记录
     */
    public List<DeploymentHistoryEntity> findByTimeRange(LocalDateTime start, LocalDateTime end) {
        Predicate predicate = deploymentHistoryEntity.createdAt.between(start, end);
        return findAll(predicate, deploymentHistoryEntity.createdAt.desc());
    }
    
    /**
     * 统计指定状态的数量
     */
    public long countByStatus(DeploymentHistoryEntity.DeploymentStatus status) {
        return count(deploymentHistoryEntity.status.eq(status));
    }
    
    /**
     * 统计成功率
     */
    public double calculateSuccessRate() {
        long total = count();
        if (total == 0) {
            return 0.0;
        }
        long successCount = countByStatus(DeploymentHistoryEntity.DeploymentStatus.SUCCESS);
        return (double) successCount / total * 100;
    }
    
    /**
     * 删除指定时间之前的记录
     */
    public int deleteBefore(LocalDateTime before) {
        return (int) getQueryFactory()
            .delete(deploymentHistoryEntity)
            .where(deploymentHistoryEntity.createdAt.lt(before))
            .execute();
    }
    
    /**
     * 查询最近的失败记录
     */
    public List<DeploymentHistoryEntity> findRecentFailures(int limit) {
        return getQueryFactory()
            .selectFrom(deploymentHistoryEntity)
            .where(deploymentHistoryEntity.status.eq(DeploymentHistoryEntity.DeploymentStatus.FAILED))
            .orderBy(deploymentHistoryEntity.createdAt.desc())
            .limit(limit)
            .fetch();
    }
}
