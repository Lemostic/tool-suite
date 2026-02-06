package io.github.lemostic.toolsuite.modules.devops.deploy.repository;

import io.github.lemostic.toolsuite.modules.devops.deploy.entity.DeploymentHistoryEntity;
import io.github.lemostic.toolsuite.modules.devops.deploy.entity.ServerConfigEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Repository 工厂
 * 管理 EntityManager 和 Repository 实例（单例模式）
 */
public class RepositoryFactory {
    
    private static final Logger logger = LoggerFactory.getLogger(RepositoryFactory.class);
    
    private static RepositoryFactory instance;
    
    private EntityManagerFactory entityManagerFactory;
    private EntityManager entityManager;
    
    private ServerConfigRepository serverConfigRepository;
    private DeploymentHistoryRepository deploymentHistoryRepository;
    
    private RepositoryFactory() {
        initialize();
    }
    
    public static synchronized RepositoryFactory getInstance() {
        if (instance == null) {
            instance = new RepositoryFactory();
        }
        return instance;
    }
    
    /**
     * 初始化数据库连接
     */
    private void initialize() {
        try {
            Map<String, String> properties = new HashMap<>();
            
            // 使用应用目录下的数据库文件
            String userHome = System.getProperty("user.home");
            String dbPath = userHome + File.separator + ".toolsuite" + File.separator + "deploy";
            new File(dbPath).mkdirs();
            
            properties.put(AvailableSettings.JAKARTA_JDBC_URL, 
                "jdbc:h2:file:" + dbPath + File.separator + "deploy_db;DB_CLOSE_DELAY=-1");
            properties.put(AvailableSettings.JAKARTA_JDBC_USER, "sa");
            properties.put(AvailableSettings.JAKARTA_JDBC_PASSWORD, "");
            properties.put(AvailableSettings.JAKARTA_JDBC_DRIVER, "org.h2.Driver");
            properties.put(AvailableSettings.HBM2DDL_AUTO, "update");
            properties.put(AvailableSettings.SHOW_SQL, "false");
            properties.put(AvailableSettings.FORMAT_SQL, "true");
            
            entityManagerFactory = new HibernatePersistenceProvider()
                .createEntityManagerFactory("ToolSuitePU", properties);
            
            entityManager = entityManagerFactory.createEntityManager();
            
            // 初始化 Repository
            serverConfigRepository = new ServerConfigRepository(entityManager);
            deploymentHistoryRepository = new DeploymentHistoryRepository(entityManager);
            
            logger.info("Repository 工厂初始化成功");
            
        } catch (Exception e) {
            logger.error("Repository 工厂初始化失败", e);
            throw new RepositoryException("数据库初始化失败", e);
        }
    }
    
    public ServerConfigRepository getServerConfigRepository() {
        return serverConfigRepository;
    }
    
    public DeploymentHistoryRepository getDeploymentHistoryRepository() {
        return deploymentHistoryRepository;
    }
    
    /**
     * 获取 EntityManager（用于自定义查询）
     */
    public EntityManager getEntityManager() {
        return entityManager;
    }
    
    /**
     * 关闭资源
     */
    public void shutdown() {
        if (entityManager != null && entityManager.isOpen()) {
            entityManager.close();
        }
        if (entityManagerFactory != null && entityManagerFactory.isOpen()) {
            entityManagerFactory.close();
        }
        instance = null;
        logger.info("Repository 工厂已关闭");
    }
}
