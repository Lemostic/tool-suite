package io.github.lemostic.toolsuite.modules.devops.deploy.service;

import com.jcraft.jsch.*;
import com.jcraft.jsch.SftpProgressMonitor;
import io.github.lemostic.toolsuite.modules.devops.deploy.model.ServerConfig;
import io.github.lemostic.toolsuite.modules.devops.deploy.model.UploadOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.function.Consumer;

public class SftpTransferService {
    private static final Logger logger = LoggerFactory.getLogger(SftpTransferService.class);
    
    private final SshConnectionPool connectionPool;
    
    public SftpTransferService(SshConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
    }
    
    public void uploadFile(ServerConfig server, String localPath, String remotePath,
                          Consumer<Long> progressCallback) throws Exception {
        Session session = connectionPool.getSession(server);
        ChannelSftp channel = null;
        
        try {
            channel = (ChannelSftp) session.openChannel("sftp");
            channel.connect();
            
            File localFile = new File(localPath);
            long fileSize = localFile.length();
            long uploaded = 0;
            
            try (InputStream fis = new FileInputStream(localFile)) {
                channel.put(fis, remotePath, new SftpProgressMonitor() {
                    private long count = 0;
                    
                    @Override
                    public void init(int op, String src, String dest, long max) {}
                    
                    @Override
                    public boolean count(long count) {
                        this.count += count;
                        if (progressCallback != null) {
                            progressCallback.accept(this.count);
                        }
                        return true;
                    }
                    
                    @Override
                    public void end() {}
                });
            }
            
            logger.info("文件上传完成: {} -> {}@{}:{}", 
                localPath, server.getUsername(), server.getHost(), remotePath);
                
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
    }
    
    public void downloadFile(ServerConfig server, String remotePath, String localPath) throws Exception {
        Session session = connectionPool.getSession(server);
        ChannelSftp channel = null;
        
        try {
            channel = (ChannelSftp) session.openChannel("sftp");
            channel.connect();
            channel.get(remotePath, localPath);
            
            logger.info("文件下载完成: {}@{}:{} -> {}", 
                server.getUsername(), server.getHost(), remotePath, localPath);
                
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
    }
    
    public void setFilePermissions(ServerConfig server, String remotePath, 
                                   String permissions) throws Exception {
        Session session = connectionPool.getSession(server);
        ChannelSftp channel = null;
        
        try {
            channel = (ChannelSftp) session.openChannel("sftp");
            channel.connect();
            channel.chmod(Integer.parseInt(permissions, 8), remotePath);
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
    }
    
    public void setFileOwner(ServerConfig server, String remotePath, 
                            String owner, String group) throws Exception {
        String command = String.format("chown %s:%s %s", owner, group, remotePath);
        ChannelExec channel = null;
        
        try {
            Session session = connectionPool.getSession(server);
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);
            channel.connect();
            
            while (!channel.isClosed()) {
                Thread.sleep(100);
            }
            
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
    }
    
    public void createRemoteDirectory(ServerConfig server, String remotePath) throws Exception {
        Session session = connectionPool.getSession(server);
        ChannelSftp channel = null;
        
        try {
            channel = (ChannelSftp) session.openChannel("sftp");
            channel.connect();
            
            try {
                channel.mkdir(remotePath);
            } catch (SftpException e) {
                // 目录可能已存在
                if (e.id != ChannelSftp.SSH_FX_FAILURE) {
                    throw e;
                }
            }
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
    }
}
