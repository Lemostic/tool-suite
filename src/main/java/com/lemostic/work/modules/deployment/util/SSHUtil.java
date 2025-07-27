package com.lemostic.work.modules.deployment.util;

import cn.hutool.core.util.StrUtil;
import com.jcraft.jsch.*;
import com.lemostic.work.modules.deployment.model.ServerConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.function.Consumer;

/**
 * SSH工具类
 */
public class SSHUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(SSHUtil.class);
    private static final DateTimeFormatter BACKUP_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
    
    /**
     * 创建SSH会话
     */
    public static Session createSession(ServerConfiguration config) throws JSchException {
        JSch jsch = new JSch();
        
        // 如果使用私钥认证
        if (StrUtil.isNotBlank(config.getPrivateKeyPath())) {
            if (StrUtil.isNotBlank(config.getPrivateKeyPassphrase())) {
                jsch.addIdentity(config.getPrivateKeyPath(), config.getPrivateKeyPassphrase());
            } else {
                jsch.addIdentity(config.getPrivateKeyPath());
            }
        }
        
        Session session = jsch.getSession(config.getUsername(), config.getHost(), config.getPort());
        
        // 如果使用密码认证
        if (StrUtil.isNotBlank(config.getPassword())) {
            session.setPassword(config.getPassword());
        }
        
        // 跳过主机密钥检查
        Properties props = new Properties();
        props.put("StrictHostKeyChecking", "no");
        session.setConfig(props);
        
        return session;
    }
    
    /**
     * 上传文件到远程服务器
     */
    public static void uploadFile(Session session, String localFilePath, String remoteFilePath, 
                                 Consumer<String> progressCallback) throws JSchException, SftpException {
        ChannelSftp sftpChannel = null;
        try {
            sftpChannel = (ChannelSftp) session.openChannel("sftp");
            sftpChannel.connect();
            
            // 确保远程目录存在
            String remoteDir = remoteFilePath.substring(0, remoteFilePath.lastIndexOf('/'));
            createRemoteDirectory(sftpChannel, remoteDir);
            
            // 上传文件
            File localFile = new File(localFilePath);
            long fileSize = localFile.length();
            
            if (progressCallback != null) {
                progressCallback.accept("开始上传文件: " + localFile.getName() + " (大小: " + formatFileSize(fileSize) + ")");
            }
            
            sftpChannel.put(localFilePath, remoteFilePath, new SftpProgressMonitor() {
                private long transferred = 0;
                private long lastReportTime = 0;
                
                @Override
                public void init(int op, String src, String dest, long max) {
                    transferred = 0;
                    lastReportTime = System.currentTimeMillis();
                }
                
                @Override
                public boolean count(long count) {
                    transferred += count;
                    long currentTime = System.currentTimeMillis();
                    
                    // 每秒报告一次进度
                    if (currentTime - lastReportTime >= 1000) {
                        if (progressCallback != null) {
                            double progress = (double) transferred / fileSize * 100;
                            progressCallback.accept(String.format("上传进度: %.1f%% (%s/%s)", 
                                                  progress, formatFileSize(transferred), formatFileSize(fileSize)));
                        }
                        lastReportTime = currentTime;
                    }
                    return true;
                }
                
                @Override
                public void end() {
                    if (progressCallback != null) {
                        progressCallback.accept("文件上传完成: " + localFile.getName());
                    }
                }
            });
            
        } finally {
            if (sftpChannel != null && sftpChannel.isConnected()) {
                sftpChannel.disconnect();
            }
        }
    }
    
    /**
     * 执行远程命令
     */
    public static String executeCommand(Session session, String command, Consumer<String> outputCallback) 
            throws JSchException, IOException {
        ChannelExec execChannel = null;
        StringBuilder output = new StringBuilder();
        
        try {
            execChannel = (ChannelExec) session.openChannel("exec");
            execChannel.setCommand(command);
            
            InputStream in = execChannel.getInputStream();
            InputStream err = execChannel.getErrStream();
            
            execChannel.connect();

            byte[] buffer = new byte[1024];
            long startTime = System.currentTimeMillis();
            long timeout = 30000; // 30秒超时

            while (true) {
                // 检查超时
                if (System.currentTimeMillis() - startTime > timeout) {
                    throw new RuntimeException("命令执行超时");
                }

                // 读取标准输出
                while (in.available() > 0) {
                    int len = in.read(buffer, 0, 1024);
                    if (len < 0) break;
                    String line = new String(buffer, 0, len);
                    output.append(line);
                    if (outputCallback != null) {
                        outputCallback.accept(line.trim());
                    }
                }

                // 读取错误输出
                while (err.available() > 0) {
                    int len = err.read(buffer, 0, 1024);
                    if (len < 0) break;
                    String line = new String(buffer, 0, len);
                    output.append(line);
                    if (outputCallback != null) {
                        outputCallback.accept("ERROR: " + line.trim());
                    }
                }

                if (execChannel.isClosed()) {
                    if (in.available() > 0 || err.available() > 0) continue;
                    break;
                }

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            
            int exitCode = execChannel.getExitStatus();
            if (exitCode != 0) {
                throw new RuntimeException("命令执行失败，退出码: " + exitCode);
            }
            
        } finally {
            if (execChannel != null && execChannel.isConnected()) {
                execChannel.disconnect();
            }
        }
        
        return output.toString();
    }
    
    /**
     * 创建备份
     */
    public static String createBackup(Session session, String sourceDir, String backupDir,
                                    Consumer<String> progressCallback) throws JSchException, IOException {
        String timestamp = LocalDateTime.now().format(BACKUP_TIME_FORMAT);
        String backupPath = backupDir + "/backup_" + timestamp + ".tar.gz";

        if (progressCallback != null) {
            progressCallback.accept("开始创建备份: " + backupPath);
        }

        // 确保备份目录存在
        createRemoteDirectory(session, backupDir, progressCallback);

        // 检查源目录是否存在且不为空
        String checkCommand = String.format("test -d %s && [ \"$(ls -A %s 2>/dev/null)\" ] && echo 'HAS_CONTENT' || echo 'EMPTY_OR_NOT_EXISTS'",
                                           sourceDir, sourceDir);
        String checkResult = executeCommand(session, checkCommand, null);

        if (!checkResult.trim().contains("HAS_CONTENT")) {
            if (progressCallback != null) {
                progressCallback.accept("源目录为空或不存在，跳过备份: " + sourceDir);
            }
            return null;
        }

        // 创建压缩备份
        String command = String.format("cd %s && tar -czf %s . 2>/dev/null || echo 'Backup completed with warnings'",
                                      sourceDir, backupPath);
        executeCommand(session, command, progressCallback);

        // 验证备份文件是否创建成功
        String verifyCommand = String.format("test -f %s && echo 'BACKUP_EXISTS' || echo 'BACKUP_FAILED'", backupPath);
        String verifyResult = executeCommand(session, verifyCommand, null);

        if (verifyResult.trim().contains("BACKUP_EXISTS")) {
            if (progressCallback != null) {
                progressCallback.accept("备份创建完成: " + backupPath);
            }
            return backupPath;
        } else {
            if (progressCallback != null) {
                progressCallback.accept("警告: 备份文件创建失败");
            }
            return null;
        }
    }
    
    /**
     * 创建远程目录（公共方法）
     */
    public static void createRemoteDirectory(Session session, String remoteDir, Consumer<String> progressCallback)
            throws JSchException, IOException {
        if (progressCallback != null) {
            progressCallback.accept("检查并创建目录: " + remoteDir);
        }

        String command = String.format("mkdir -p %s", remoteDir);
        executeCommand(session, command, progressCallback);

        if (progressCallback != null) {
            progressCallback.accept("目录创建完成: " + remoteDir);
        }
    }

    /**
     * 创建远程目录（SFTP方式）
     */
    private static void createRemoteDirectory(ChannelSftp sftpChannel, String remoteDir) throws SftpException {
        String[] dirs = remoteDir.split("/");
        String currentDir = "";

        for (String dir : dirs) {
            if (dir.isEmpty()) continue;
            currentDir += "/" + dir;

            try {
                sftpChannel.stat(currentDir);
            } catch (SftpException e) {
                // 目录不存在，创建它
                sftpChannel.mkdir(currentDir);
            }
        }
    }

    /**
     * 检查远程目录是否存在
     */
    public static boolean checkRemoteDirectoryExists(Session session, String remoteDir, Consumer<String> progressCallback)
            throws JSchException, IOException {
        try {
            String command = String.format("test -d %s && echo 'EXISTS' || echo 'NOT_EXISTS'", remoteDir);
            String result = executeCommand(session, command, null);
            boolean exists = result.trim().contains("EXISTS");

            if (progressCallback != null) {
                progressCallback.accept("目录 " + remoteDir + (exists ? " 存在" : " 不存在"));
            }

            return exists;
        } catch (Exception e) {
            if (progressCallback != null) {
                progressCallback.accept("检查目录失败: " + e.getMessage());
            }
            return false;
        }
    }
    
    /**
     * 格式化文件大小
     */
    private static String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.1f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", size / (1024.0 * 1024));
        } else {
            return String.format("%.1f GB", size / (1024.0 * 1024 * 1024));
        }
    }
}
