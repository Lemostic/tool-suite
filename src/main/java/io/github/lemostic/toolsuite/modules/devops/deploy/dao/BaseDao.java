package io.github.lemostic.toolsuite.modules.devops.deploy.dao;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;

import javax.sql.DataSource;
import java.util.List;
import java.util.Properties;

/**
 * Hibernate数据库访问对象基类
 * 提供基本的数据库操作功能
 */
public abstract class BaseDao<T> {
    
    protected static SessionFactory sessionFactory;
    protected Class<T> entityClass;
    
    static {
        initializeSessionFactory();
    }
    
    protected BaseDao(Class<T> entityClass) {
        this.entityClass = entityClass;
    }
    
    /**
     * 初始化Hibernate SessionFactory
     */
    private static void initializeSessionFactory() {
        try {
            Configuration configuration = new Configuration();
            
            // 数据库连接配置
            Properties props = new Properties();
            props.setProperty("hibernate.connection.driver_class", "org.h2.Driver");
            props.setProperty("hibernate.connection.url", "jdbc:h2:~/tool-suite-db;AUTO_SERVER=TRUE");
            props.setProperty("hibernate.connection.username", "sa");
            props.setProperty("hibernate.connection.password", "");
            
            // Hibernate配置
            props.setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
            props.setProperty("hibernate.hbm2ddl.auto", "update");
            props.setProperty("hibernate.show_sql", "false");
            props.setProperty("hibernate.format_sql", "true");
            props.setProperty("hibernate.use_sql_comments", "true");
            
            // 连接池配置
            props.setProperty("hibernate.c3p0.min_size", "5");
            props.setProperty("hibernate.c3p0.max_size", "20");
            props.setProperty("hibernate.c3p0.timeout", "300");
            props.setProperty("hibernate.c3p0.max_statements", "50");
            props.setProperty("hibernate.c3p0.idle_test_period", "3000");
            
            configuration.setProperties(props);
            
            // 添加实体映射
            configuration.addAnnotatedClass(ServerConfig.class);
            configuration.addAnnotatedClass(DeploymentHistory.class);
            
            ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
                    .applySettings(configuration.getProperties())
                    .build();
                    
            sessionFactory = configuration.buildSessionFactory(serviceRegistry);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize Hibernate SessionFactory", e);
        }
    }
    
    /**
     * 获取Session
     */
    protected Session getSession() {
        return sessionFactory.getCurrentSession();
    }
    
    /**
     * 开启事务
     */
    protected Transaction beginTransaction() {
        return getSession().beginTransaction();
    }
    
    /**
     * 保存实体
     */
    public void save(T entity) {
        Transaction tx = null;
        try {
            tx = beginTransaction();
            getSession().save(entity);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw new RuntimeException("Failed to save entity", e);
        }
    }
    
    /**
     * 更新实体
     */
    public void update(T entity) {
        Transaction tx = null;
        try {
            tx = beginTransaction();
            getSession().update(entity);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw new RuntimeException("Failed to update entity", e);
        }
    }
    
    /**
     * 保存或更新实体
     */
    public void saveOrUpdate(T entity) {
        Transaction tx = null;
        try {
            tx = beginTransaction();
            getSession().saveOrUpdate(entity);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw new RuntimeException("Failed to save or update entity", e);
        }
    }
    
    /**
     * 根据ID删除实体
     */
    public void deleteById(Long id) {
        Transaction tx = null;
        try {
            tx = beginTransaction();
            T entity = findById(id);
            if (entity != null) {
                getSession().delete(entity);
            }
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw new RuntimeException("Failed to delete entity", e);
        }
    }
    
    /**
     * 根据ID查找实体
     */
    public T findById(Long id) {
        try {
            return getSession().get(entityClass, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to find entity by id: " + id, e);
        }
    }
    
    /**
     * 查找所有实体
     */
    public List<T> findAll() {
        try {
            return getSession().createQuery("FROM " + entityClass.getSimpleName(), entityClass).list();
        } catch (Exception e) {
            throw new RuntimeException("Failed to find all entities", e);
        }
    }
    
    /**
     * 根据条件查询
     */
    public List<T> findByProperty(String propertyName, Object value) {
        try {
            String hql = "FROM " + entityClass.getSimpleName() + " WHERE " + propertyName + " = :value";
            return getSession().createQuery(hql, entityClass)
                    .setParameter("value", value)
                    .list();
        } catch (Exception e) {
            throw new RuntimeException("Failed to find entities by property", e);
        }
    }
    
    /**
     * 执行HQL查询
     */
    public List<T> executeQuery(String hql, Object... params) {
        try {
            org.hibernate.query.Query<T> query = getSession().createQuery(hql, entityClass);
            for (int i = 0; i < params.length; i++) {
                query.setParameter(i, params[i]);
            }
            return query.list();
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute query: " + hql, e);
        }
    }
    
    /**
     * 执行原生SQL查询
     */
    public List<Object[]> executeNativeQuery(String sql, Object... params) {
        try {
            org.hibernate.query.Query<Object[]> query = getSession().createNativeQuery(sql);
            for (int i = 0; i < params.length; i++) {
                query.setParameter(i + 1, params[i]);
            }
            return query.list();
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute native query: " + sql, e);
        }
    }
    
    /**
     * 关闭SessionFactory
     */
    public static void shutdown() {
        if (sessionFactory != null) {
            sessionFactory.close();
        }
    }
}