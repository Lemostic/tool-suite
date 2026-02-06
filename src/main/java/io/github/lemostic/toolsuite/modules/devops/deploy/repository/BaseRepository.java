package io.github.lemostic.toolsuite.modules.devops.deploy.repository;

import java.util.List;
import java.util.Optional;

/**
 * 基础 Repository 接口
 * 定义通用的 CRUD 操作
 * 
 * @param <T> 实体类型
 * @param <ID> 主键类型
 */
public interface BaseRepository<T, ID> {
    
    /**
     * 保存实体（新增或更新）
     */
    T save(T entity);
    
    /**
     * 根据ID查询
     */
    Optional<T> findById(ID id);
    
    /**
     * 查询所有
     */
    List<T> findAll();
    
    /**
     * 根据ID删除
     */
    void deleteById(ID id);
    
    /**
     * 删除实体
     */
    void delete(T entity);
    
    /**
     * 统计数量
     */
    long count();
    
    /**
     * 是否存在
     */
    boolean existsById(ID id);
}
