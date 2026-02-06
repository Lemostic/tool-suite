package io.github.lemostic.toolsuite.modules.devops.deploy.service;

import com.jcraft.jsch.*;
import io.github.lemostic.toolsuite.modules.devops.deploy.model.ServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.concurrent.*;

public class SshConnectionPool {
    private static final Logger logger = LoggerFactory.getLogger(SshConnectionPool.class);
    private static final int MAX_POOL_SIZE = 10;
    private static final long CONNECTION_TIMEOUT = 30000; // 30秒
    
    private final ConcurrentHashMap<String, Session> sessionPool = new ConcurrentHashMap<>();
    private final JSch jsch = new JSch();
    
    public Session getSession(ServerConfig server) throws JSchException {
        String key = server.getHost() + ":" + server.getPort() + "@" + server.getUsername();
        
        Session session = sessionPool.get(key);
        if (session != null && session.isConnected()) {
            return session;
        }
        
        session = jsch.getSession(server.getUsername(), server.getHost(), server.getPort());
        session.setPassword(server.getPassword());
        session.setConfig("StrictHostKeyChecking", "no");
        session.setTimeout((int) CONNECTION_TIMEOUT);
        session.connect();
        
        sessionPool.put(key, session);
        logger.info("创建SSH连接到: {}@{}:{}", server.getUsername(), server.getHost(), server.getPort());
        
        return session;
    }
    
    public void closeSession(ServerConfig server) {
        String key = server.getHost() + ":" + server.getPort() + "@" + server.getUsername();
        Session session = sessionPool.remove(key);
        if (session != null && session.isConnected()) {
            session.disconnect();
            logger.info("关闭SSH连接: {}", key);
        }
    }
    
    public void closeAll() {
        sessionPool.forEach((key, session) -> {
            if (session.isConnected()) {
                session.disconnect();
            }
        });
        sessionPool.clear();
        logger.info("关闭所有SSH连接");
    }
}
