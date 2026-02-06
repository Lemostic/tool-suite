package io.github.lemostic.toolsuite.modules.devops.deploy.repository;

import io.github.lemostic.toolsuite.modules.devops.deploy.entity.ServerConfigEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import java.util.List;
import java.util.Optional;

/**
 * 服务器配置 Repository
 */
public class ServerConfigRepository extends AbstractBaseRepository<ServerConfigEntity, Long> {
    
    public ServerConfigRepository(EntityManager entityManager) {
        super(entityManager, ServerConfigEntity.class);
    }
    
    @Override
    protected boolean isNew(ServerConfigEntity entity) {
        return entity.getId() == null;
    }
    
    /**
     * 根据名称查询
     */
    public Optional<ServerConfigEntity> findByName(String name) {
        TypedQuery<ServerConfigEntity> query = entityManager.createQuery(
            "SELECT s FROM ServerConfigEntity s WHERE s.name = :name",
            ServerConfigEntity.class
        );
        query.setParameter("name", name);
        
        try {
            return Optional.of(query.getSingleResult());
        } catch (Exception e) {
            return Optional.empty();
        }
    }
    
    /**
     * 查询所有启用的服务器
     */
    public List<ServerConfigEntity> findAllEnabled() {
        TypedQuery<ServerConfigEntity> query = entityManager.createQuery(
            "SELECT s FROM ServerConfigEntity s WHERE s.enabled = true ORDER BY s.name",
            ServerConfigEntity.class
        );
        return query.getResultList();
    }
    
    /**
     * 根据主机地址查询
     */
    public List<ServerConfigEntity> findByHost(String host) {
        TypedQuery<ServerConfigEntity> query = entityManager.createQuery(
            "SELECT s FROM ServerConfigEntity s WHERE s.host = :host",
            ServerConfigEntity.class
        );
        query.setParameter("host", host);
        return query.getResultList();
    }
    
    /**
     * 检查名称是否已存在（排除指定ID）
     */
    public boolean existsByNameAndNotId(String name, Long excludeId) {
        TypedQuery<Long> query = entityManager.createQuery(
            "SELECT COUNT(s) FROM ServerConfigEntity s WHERE s.name = :name AND s.id <> :excludeId",
            Long.class
        );
        query.setParameter("name", name);
        query.setParameter("excludeId", excludeId);
        return query.getSingleResult() > 0;
    }
}
