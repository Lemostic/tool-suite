package io.github.lemostic.toolsuite.modules.devops.deploy.task;

import io.github.lemostic.toolsuite.modules.devops.deploy.model.*;
import io.github.lemostic.toolsuite.modules.devops.deploy.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.function.Consumer;

public class DeploySteps {
    private static final Logger logger = LoggerFactory.getLogger(DeploySteps.class);
    
    // 连接服务器步骤
    public static TaskStep createConnectStep(SshCommandService sshService, ServerConfigDTO server) {
        return new TaskStep() {
            @Override
            public String getName() {
                return "CONNECT";
            }
            
            @Override
            public String getDescription() {
                return "连接服务器 " + server.getName() + " (" + server.getHost() + ")";
            }
            
            @Override
            public TaskResult execute(TaskContext context, Consumer<String> logConsumer) {
                logConsumer.accept("正在连接服务器: " + server.getHost() + ":" + server.getPort());
                
                try {
                    if (sshService.testConnection(server)) {
                        context.setAttribute("server", server);
                        logConsumer.accept("服务器连接成功");
                        return TaskResult.success("连接成功");
                    } else {
                        return TaskResult.failure("连接失败");
                    }
                } catch (Exception e) {
                    logger.error("连接服务器失败", e);
                    return TaskResult.failure("连接异常: " + e.getMessage(), e);
                }
            }
        };
    }
    
    // 停止服务步骤
    public static TaskStep createStopServiceStep(SshCommandService sshService, ServerConfigDTO server) {
        return new TaskStep() {
            @Override
            public String getName() {
                return "STOP_SERVICE";
            }
            
            @Override
            public String getDescription() {
                return "停止服务";
            }
            
            @Override
            public TaskResult execute(TaskContext context, Consumer<String> logConsumer) {
                String stopScript = server.getBinDirectory() + "/" + server.getStopScript();
                logConsumer.accept("正在停止服务: " + stopScript);
                
                try {
                    StringBuilder output = new StringBuilder();
                    int exitCode = sshService.executeCommand(server, stopScript, 
                        line -> {
                            output.append(line).append("\n");
                            logConsumer.accept("[STOP] " + line);
                        },
                        line -> logConsumer.accept("[STOP-ERR] " + line)
                    );
                    
                    if (exitCode == 0) {
                        logConsumer.accept("服务停止成功");
                        return TaskResult.success("服务已停止");
                    } else {
                        // 服务可能本来就没运行，不视为失败
                        logConsumer.accept("服务停止命令返回: " + exitCode + " (可能服务未运行)");
                        return TaskResult.success("服务停止完成 (可能未运行)");
                    }
                } catch (Exception e) {
                    logger.error("停止服务失败", e);
                    return TaskResult.failure("停止服务异常: " + e.getMessage(), e);
                }
            }
            
            @Override
            public boolean canRollback() {
                return true;
            }
            
            @Override
            public TaskResult rollback(TaskContext context, Consumer<String> logConsumer) {
                // 回滚时启动服务
                String startScript = server.getBinDirectory() + "/" + server.getStartScript();
                logConsumer.accept("[Rollback] 重新启动服务: " + startScript);
                try {
                    sshService.executeCommand(server, startScript, 
                        line -> logConsumer.accept("[ROLLBACK-START] " + line),
                        line -> logConsumer.accept("[ROLLBACK-ERR] " + line)
                    );
                    return TaskResult.success("服务已重新启动");
                } catch (Exception e) {
                    return TaskResult.failure("回滚启动失败: " + e.getMessage());
                }
            }
        };
    }
    
    // 备份步骤
    public static TaskStep createBackupStep(SshCommandService sshService, ServerConfigDTO server,
                                             BackupOptions options) {
        return new TaskStep() {
            @Override
            public String getName() {
                return "BACKUP";
            }
            
            @Override
            public String getDescription() {
                return "备份原程序";
            }
            
            @Override
            public TaskResult execute(TaskContext context, Consumer<String> logConsumer) {
                String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                String backupDir = server.getBackupDirectory() + "/backup_" + timestamp;
                
                logConsumer.accept("开始备份原程序到: " + backupDir);
                
                try {
                    // 创建备份目录
                    String mkdirCmd = "mkdir -p " + backupDir;
                    sshService.executeCommand(server, mkdirCmd, 
                        line -> {}, line -> {});
                    
                    String backupCmd;
                    switch (options.getMode()) {
                        case ALL:
                            backupCmd = String.format("cp -r %s/* %s/", 
                                server.getAppDirectory(), backupDir);
                            break;
                        case SELECTIVE:
                            // 选择性备份特定文件
                            StringBuilder sb = new StringBuilder();
                            for (String file : options.getSelectedFiles()) {
                                sb.append("cp -r ").append(server.getAppDirectory())
                                  .append("/").append(file).append(" ")
                                  .append(backupDir).append("/; ");
                            }
                            backupCmd = sb.toString();
                            break;
                        case PATTERN:
                            // 按模式备份
                            backupCmd = String.format("cd %s && find . -name '%s' -exec cp --parents {} %s/ \\;",
                                server.getAppDirectory(), options.getPattern(), backupDir);
                            break;
                        default:
                            backupCmd = String.format("cp -r %s/* %s/", 
                                server.getAppDirectory(), backupDir);
                    }
                    
                    logConsumer.accept("执行备份命令: " + backupCmd);
                    int exitCode = sshService.executeCommand(server, backupCmd,
                        line -> logConsumer.accept("[BACKUP] " + line),
                        line -> logConsumer.accept("[BACKUP-ERR] " + line)
                    );
                    
                    if (exitCode == 0) {
                        context.setAttribute("backupDir", backupDir);
                        logConsumer.accept("备份完成: " + backupDir);
                        
                        // 如果需要删除原文件
                        if (options.getDeleteAfterBackup()) {
                            logConsumer.accept("删除原程序文件...");
                            String cleanCmd = String.format("rm -rf %s/*", server.getAppDirectory());
                            sshService.executeCommand(server, cleanCmd,
                                line -> {}, line -> {});
                            logConsumer.accept("原程序文件已删除");
                        }
                        
                        return TaskResult.success("备份完成: " + backupDir);
                    } else {
                        return TaskResult.failure("备份失败，退出码: " + exitCode);
                    }
                    
                } catch (Exception e) {
                    logger.error("备份失败", e);
                    return TaskResult.failure("备份异常: " + e.getMessage(), e);
                }
            }
            
            @Override
            public boolean canRollback() {
                return true;
            }
            
            @Override
            public TaskResult rollback(TaskContext context, Consumer<String> logConsumer) {
                String backupDir = context.getAttribute("backupDir");
                if (backupDir == null) {
                    return TaskResult.success("无需回滚备份");
                }
                
                logConsumer.accept("[Rollback] 从备份恢复: " + backupDir);
                try {
                    String restoreCmd = String.format("cp -r %s/* %s/", backupDir, server.getAppDirectory());
                    sshService.executeCommand(server, restoreCmd,
                        line -> logConsumer.accept("[ROLLBACK] " + line),
                        line -> logConsumer.accept("[ROLLBACK-ERR] " + line)
                    );
                    return TaskResult.success("已从备份恢复");
                } catch (Exception e) {
                    return TaskResult.failure("回滚恢复失败: " + e.getMessage());
                }
            }
        };
    }
    
    // 上传步骤
    public static TaskStep createUploadStep(SftpTransferService sftpService, ServerConfigDTO server,
                                             String localPackagePath, UploadOptions options) {
        return new TaskStep() {
            @Override
            public String getName() {
                return "UPLOAD";
            }
            
            @Override
            public String getDescription() {
                return "上传程序包";
            }
            
            @Override
            public TaskResult execute(TaskContext context, Consumer<String> logConsumer) {
                File packageFile = new File(localPackagePath);
                String remotePath = server.getAppDirectory() + "/" + packageFile.getName();
                
                logConsumer.accept("开始上传: " + packageFile.getName());
                logConsumer.accept("目标路径: " + remotePath);
                logConsumer.accept("文件大小: " + (packageFile.length() / 1024 / 1024) + " MB");
                
                try {
                    sftpService.uploadFile(server, localPackagePath, remotePath, 
                        uploaded -> {
                            int percent = (int) ((uploaded * 100) / packageFile.length());
                            if (percent % 10 == 0) {
                                logConsumer.accept("上传进度: " + percent + "%");
                            }
                        }
                    );
                    
                    logConsumer.accept("文件上传完成");
                    
                    // 更新权限
                    if (options.getUpdatePermissions()) {
                        logConsumer.accept("更新文件权限: " + options.getFilePermissions());
                        sftpService.setFilePermissions(server, remotePath, options.getFilePermissions());
                        
                        if (options.getOwner() != null && !options.getOwner().isEmpty()) {
                            logConsumer.accept("更新文件所有者: " + options.getOwner() + ":" + options.getGroup());
                            sftpService.setFileOwner(server, remotePath, options.getOwner(), options.getGroup());
                        }
                    }
                    
                    // 如果是压缩包，解压
                    if (remotePath.endsWith(".zip") || remotePath.endsWith(".tar.gz") || remotePath.endsWith(".tgz")) {
                        logConsumer.accept("解压程序包...");
                        String extractCmd;
                        if (remotePath.endsWith(".zip")) {
                            extractCmd = String.format("cd %s && unzip -o %s", 
                                server.getAppDirectory(), packageFile.getName());
                        } else {
                            extractCmd = String.format("cd %s && tar -xzf %s", 
                                server.getAppDirectory(), packageFile.getName());
                        }
                        
                        SshCommandService sshService = context.getAttribute("sshService");
                        sshService.executeCommand(server, extractCmd,
                            line -> logConsumer.accept("[EXTRACT] " + line),
                            line -> logConsumer.accept("[EXTRACT-ERR] " + line)
                        );
                        
                        // 删除压缩包
                        String rmCmd = "rm " + remotePath;
                        sshService.executeCommand(server, rmCmd, line -> {}, line -> {});
                        logConsumer.accept("程序包解压完成");
                    }
                    
                    return TaskResult.success("上传完成");
                    
                } catch (Exception e) {
                    logger.error("上传失败", e);
                    return TaskResult.failure("上传异常: " + e.getMessage(), e);
                }
            }
        };
    }
    
    // 启动服务步骤
    public static TaskStep createStartServiceStep(SshCommandService sshService, ServerConfigDTO server) {
        return new TaskStep() {
            @Override
            public String getName() {
                return "START_SERVICE";
            }
            
            @Override
            public String getDescription() {
                return "启动服务";
            }
            
            @Override
            public TaskResult execute(TaskContext context, Consumer<String> logConsumer) {
                String startScript = server.getBinDirectory() + "/" + server.getStartScript();
                logConsumer.accept("正在启动服务: " + startScript);
                
                try {
                    // 启动服务（后台运行）
                    String cmd = "nohup " + startScript + " > /dev/null 2>&1 &";
                    sshService.executeCommand(server, cmd,
                        line -> logConsumer.accept("[START] " + line),
                        line -> logConsumer.accept("[START-ERR] " + line)
                    );
                    
                    // 等待几秒检查服务状态
                    logConsumer.accept("等待服务启动...");
                    Thread.sleep(3000);
                    
                    // 检查进程是否存在
                    String checkCmd = "ps aux | grep -v grep | grep " + server.getAppDirectory();
                    StringBuilder checkOutput = new StringBuilder();
                    sshService.executeCommand(server, checkCmd,
                        line -> checkOutput.append(line).append("\n"),
                        line -> {}
                    );
                    
                    if (checkOutput.length() > 0) {
                        logConsumer.accept("服务已启动");
                        return TaskResult.success("服务启动成功");
                    } else {
                        logConsumer.accept("警告: 无法确认服务状态，请手动检查");
                        return TaskResult.success("服务启动命令已执行（状态未知）");
                    }
                    
                } catch (Exception e) {
                    logger.error("启动服务失败", e);
                    return TaskResult.failure("启动服务异常: " + e.getMessage(), e);
                }
            }
        };
    }
}
