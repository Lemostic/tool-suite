package com.lemostic.work.config;

import com.lemostic.work.database.config.DatabaseInitializer;
import com.lemostic.work.database.config.MyBatisPlusConfig;
import com.lemostic.work.database.mapper.DeploymentHistoryMapper;
import com.lemostic.work.database.mapper.ServerConfigurationMapper;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.mapper.MapperFactoryBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Spring应用配置类
 * 配置数据源和启用组件扫描
 */
@Configuration
@ComponentScan(basePackages = {
    "com.lemostic.work.database.service",
    "com.lemostic.work.database.mapper",
    "com.lemostic.work.modules.deployment.service",
    "com.lemostic.work.modules.deployment"
})
public class ApplicationConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(ApplicationConfig.class);
    
    /**
     * 数据源Bean
     */
    @Bean
    public DataSource dataSource() {
        MyBatisPlusConfig config = MyBatisPlusConfig.getInstance();
        DataSource dataSource = config.getDataSource();

        // 初始化数据库表结构
        try {
            DatabaseInitializer.initialize(dataSource);
        } catch (Exception e) {
            logger.error("数据库初始化失败，但应用将继续运行", e);
        }

        return dataSource;
    }
    
    /**
     * SqlSessionFactory配置
     */
    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);

        // 基本配置
        org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
        configuration.setMapUnderscoreToCamelCase(true); // 下划线转驼峰
        configuration.setCacheEnabled(false); // 禁用二级缓存
        configuration.setCallSettersOnNulls(true); // 空值也调用setter

        factoryBean.setConfiguration(configuration);

        logger.info("SqlSessionFactory配置完成");
        return factoryBean.getObject();
    }

    /**
     * ServerConfigurationMapper Bean
     */
    @Bean
    public ServerConfigurationMapper serverConfigurationMapper(SqlSessionFactory sqlSessionFactory) throws Exception {
        MapperFactoryBean<ServerConfigurationMapper> factoryBean = new MapperFactoryBean<>(ServerConfigurationMapper.class);
        factoryBean.setSqlSessionFactory(sqlSessionFactory);
        return factoryBean.getObject();
    }

    /**
     * DeploymentHistoryMapper Bean
     */
    @Bean
    public DeploymentHistoryMapper deploymentHistoryMapper(SqlSessionFactory sqlSessionFactory) throws Exception {
        MapperFactoryBean<DeploymentHistoryMapper> factoryBean = new MapperFactoryBean<>(DeploymentHistoryMapper.class);
        factoryBean.setSqlSessionFactory(sqlSessionFactory);
        return factoryBean.getObject();
    }
}
