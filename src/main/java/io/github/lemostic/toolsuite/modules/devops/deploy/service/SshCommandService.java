package io.github.lemostic.toolsuite.modules.devops.deploy.service;

import com.jcraft.jsch.*;
import io.github.lemostic.toolsuite.modules.devops.deploy.model.ServerConfigDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.function.Consumer;

public class SshCommandService {
    private static final Logger logger = LoggerFactory.getLogger(SshCommandService.class);
    
    private final SshConnectionPool connectionPool;
    
    public SshCommandService(SshConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
    }
    
    public int executeCommand(ServerConfigDTO server, String command,
                              Consumer<String> outputConsumer,
                              Consumer<String> errorConsumer) throws Exception {
        Session session = connectionPool.getSession(server);
        ChannelExec channel = null;
        
        try {
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);
            channel.setInputStream(null);
            channel.setErrStream(null);
            
            InputStream stdout = channel.getInputStream();
            InputStream stderr = channel.getErrStream();
            
            channel.connect();
            
            // 读取标准输出
            Thread outThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(stdout))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (outputConsumer != null) {
                            outputConsumer.accept(line);
                        }
                    }
                } catch (IOException e) {
                    logger.error("读取标准输出失败", e);
                }
            });
            
            // 读取错误输出
            Thread errThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(stderr))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (errorConsumer != null) {
                            errorConsumer.accept(line);
                        }
                    }
                } catch (IOException e) {
                    logger.error("读取错误输出失败", e);
                }
            });
            
            outThread.start();
            errThread.start();
            
            while (!channel.isClosed()) {
                Thread.sleep(100);
            }
            
            outThread.join(1000);
            errThread.join(1000);
            
            return channel.getExitStatus();
            
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
    }
    
    public boolean testConnection(ServerConfigDTO server) {
        try {
            Session session = connectionPool.getSession(server);
            return session.isConnected();
        } catch (Exception e) {
            logger.error("连接测试失败: {}@{}:{}", 
                server.getUsername(), server.getHost(), server.getPort(), e);
            return false;
        }
    }
}
