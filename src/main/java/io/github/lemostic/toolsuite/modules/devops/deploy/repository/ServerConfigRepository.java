package io.github.lemostic.toolsuite.modules.devops.deploy.repository;

import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Predicate;
import io.github.lemostic.toolsuite.modules.devops.deploy.entity.ServerConfigEntity;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Optional;

import static io.github.lemostic.toolsuite.modules.devops.deploy.entity.QServerConfigEntity.serverConfigEntity;

/**
 * 服务器配置 Repository
 * 使用 QueryDSL 实现类型安全的查询
 */
public class ServerConfigRepository extends QueryDslRepository<ServerConfigEntity, Long> {
    
    public ServerConfigRepository(EntityManager entityManager) {
        super(entityManager, ServerConfigEntity.class);
    }
    
    @Override
    protected EntityPath<ServerConfigEntity> getEntityPath() {
        return serverConfigEntity;
    }
    
    @Override
    protected boolean isNew(ServerConfigEntity entity) {
        return entity.getId() == null;
    }
    
    /**
     * 根据名称查询
     */
    public Optional<ServerConfigEntity> findByName(String name) {
        return findOne(serverConfigEntity.name.eq(name));
    }
    
    /**
     * 查询所有启用的服务器
     */
    public List<ServerConfigEntity> findAllEnabled() {
        return findAll(
            serverConfigEntity.enabled.isTrue(),
            serverConfigEntity.name.asc()
        );
    }
    
    /**
     * 根据主机地址查询
     */
    public List<ServerConfigEntity> findByHost(String host) {
        return findAll(serverConfigEntity.host.eq(host));
    }
    
    /**
     * 检查名称是否已存在（排除指定ID）
     */
    public boolean existsByNameAndNotId(String name, Long excludeId) {
        Predicate predicate = serverConfigEntity.name.eq(name)
            .and(serverConfigEntity.id.ne(excludeId));
        return exists(predicate);
    }
    
    /**
     * 检查主机和端口组合是否已存在
     */
    public boolean existsByHostAndPort(String host, Integer port) {
        return exists(
            serverConfigEntity.host.eq(host)
                .and(serverConfigEntity.port.eq(port))
        );
    }
    
    /**
     * 根据名称模糊查询
     */
    public List<ServerConfigEntity> findByNameLike(String keyword) {
        return findAll(
            serverConfigEntity.name.containsIgnoreCase(keyword),
            serverConfigEntity.name.asc()
        );
    }
    
    /**
     * 统计启用的服务器数量
     */
    public long countEnabled() {
        return count(serverConfigEntity.enabled.isTrue());
    }
}
