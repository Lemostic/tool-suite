package io.github.lemostic.toolsuite.modules.devops.deploy.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 基础 Repository 抽象实现
 * 封装通用的 JPA 操作
 * 
 * @param <T> 实体类型
 * @param <ID> 主键类型
 */
public abstract class AbstractBaseRepository<T, ID> implements BaseRepository<T, ID> {
    
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    
    protected final EntityManager entityManager;
    protected final Class<T> entityClass;
    
    public AbstractBaseRepository(EntityManager entityManager, Class<T> entityClass) {
        this.entityManager = entityManager;
        this.entityClass = entityClass;
    }
    
    /**
     * 在事务中执行操作
     */
    protected void executeInTransaction(Consumer<EntityManager> action) {
        EntityTransaction tx = entityManager.getTransaction();
        try {
            tx.begin();
            action.accept(entityManager);
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            logger.error("事务执行失败", e);
            throw new RepositoryException("数据库操作失败", e);
        }
    }
    
    /**
     * 在事务中执行并返回结果
     */
    protected <R> R executeInTransaction(Function<EntityManager, R> action) {
        EntityTransaction tx = entityManager.getTransaction();
        try {
            tx.begin();
            R result = action.apply(entityManager);
            tx.commit();
            return result;
        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            logger.error("事务执行失败", e);
            throw new RepositoryException("数据库操作失败", e);
        }
    }
    
    @Override
    public T save(T entity) {
        return executeInTransaction(em -> {
            if (isNew(entity)) {
                em.persist(entity);
                return entity;
            } else {
                return em.merge(entity);
            }
        });
    }
    
    @Override
    public Optional<T> findById(ID id) {
        return Optional.ofNullable(entityManager.find(entityClass, id));
    }
    
    @Override
    public List<T> findAll() {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> query = cb.createQuery(entityClass);
        Root<T> root = query.from(entityClass);
        query.select(root);
        
        TypedQuery<T> typedQuery = entityManager.createQuery(query);
        return typedQuery.getResultList();
    }
    
    @Override
    public void deleteById(ID id) {
        findById(id).ifPresent(this::delete);
    }
    
    @Override
    public void delete(T entity) {
        executeInTransaction(em -> {
            if (!em.contains(entity)) {
                em.remove(em.merge(entity));
            } else {
                em.remove(entity);
            }
        });
    }
    
    @Override
    public long count() {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<T> root = query.from(entityClass);
        query.select(cb.count(root));
        
        return entityManager.createQuery(query).getSingleResult();
    }
    
    @Override
    public boolean existsById(ID id) {
        return findById(id).isPresent();
    }
    
    /**
     * 判断实体是否为新实体
     * 子类可以覆盖此方法
     */
    protected abstract boolean isNew(T entity);
}
