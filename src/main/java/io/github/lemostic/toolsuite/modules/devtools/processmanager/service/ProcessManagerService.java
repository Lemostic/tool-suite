package io.github.lemostic.toolsuite.modules.devtools.processmanager.service;

import io.github.lemostic.toolsuite.modules.devtools.processmanager.model.ProcessInfo;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 进程管理服务
 * 负责获取进程列表、通过端口查找进程、结束进程
 */
@Slf4j
public class ProcessManagerService {
    
    private final StringProperty statusMessage = new SimpleStringProperty("就绪");
    private final DoubleProperty progress = new SimpleDoubleProperty(0);
    
    // Windows 系统标志
    private final boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
    
    public StringProperty statusMessageProperty() {
        return statusMessage;
    }
    
    public DoubleProperty progressProperty() {
        return progress;
    }
    
    /**
     * 获取所有进程列表
     */
    public CompletableFuture<List<ProcessInfo>> getProcessListAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Platform.runLater(() -> {
                    statusMessage.set("正在获取进程列表...");
                    progress.set(0.3);
                });
                
                List<ProcessInfo> processes;
                if (isWindows) {
                    processes = getWindowsProcessList();
                } else {
                    processes = getLinuxProcessList();
                }
                
                Platform.runLater(() -> progress.set(0.6));
                
                // 获取端口映射并填充到进程信息
                try {
                    Map<Integer, Integer> portToPidMap = isWindows ? getWindowsPortToPidMap() : getLinuxPortToPidMap();
                    for (ProcessInfo process : processes) {
                        List<Integer> processPorts = new ArrayList<>();
                        for (Map.Entry<Integer, Integer> entry : portToPidMap.entrySet()) {
                            if (entry.getValue() == process.getPid()) {
                                processPorts.add(entry.getKey());
                            }
                        }
                        process.setPorts(processPorts);
                    }
                } catch (Exception e) {
                    logger.warn("获取端口映射失败: {}", e.getMessage());
                    // 端口信息获取失败不影响进程列表显示
                }
                
                Platform.runLater(() -> {
                    statusMessage.set("找到 " + processes.size() + " 个进程");
                    progress.set(1.0);
                });
                
                return processes;
            } catch (Exception e) {
                logger.error("获取进程列表失败: {}", e.getMessage(), e);
                Platform.runLater(() -> {
                    statusMessage.set("获取进程列表失败: " + e.getMessage());
                    progress.set(0);
                });
                throw new RuntimeException("获取进程列表失败: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * 通过端口号查找进程
     * 支持多个端口号，用逗号分隔
     */
    public CompletableFuture<List<ProcessInfo>> findProcessesByPortsAsync(String portsStr) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 解析端口号
                List<Integer> targetPorts = parsePorts(portsStr);
                if (targetPorts.isEmpty()) {
                    Platform.runLater(() -> statusMessage.set("请输入有效的端口号"));
                    return new ArrayList<ProcessInfo>();
                }
                
                Platform.runLater(() -> {
                    statusMessage.set("正在查找占用端口 " + portsStr + " 的进程...");
                    progress.set(0.3);
                });
                
                // 获取所有进程
                List<ProcessInfo> allProcesses = isWindows ? getWindowsProcessList() : getLinuxProcessList();
                
                Platform.runLater(() -> progress.set(0.7));
                
                // 获取端口映射
                Map<Integer, Integer> portToPidMap = isWindows ? getWindowsPortToPidMap() : getLinuxPortToPidMap();
                
                // 为进程设置端口信息
                for (ProcessInfo process : allProcesses) {
                    List<Integer> processPorts = new ArrayList<>();
                    for (Map.Entry<Integer, Integer> entry : portToPidMap.entrySet()) {
                        if (entry.getValue() == process.getPid()) {
                            processPorts.add(entry.getKey());
                        }
                    }
                    process.setPorts(processPorts);
                }
                
                // 过滤出占用目标端口的进程
                List<ProcessInfo> result = allProcesses.stream()
                    .filter(p -> p.getPorts() != null && 
                                 p.getPorts().stream().anyMatch(targetPorts::contains))
                    .collect(Collectors.toList());
                
                Platform.runLater(() -> {
                    statusMessage.set("找到 " + result.size() + " 个占用指定端口的进程");
                    progress.set(1.0);
                });
                
                return result;
            } catch (Exception e) {
                logger.error("查找进程失败: {}", e.getMessage(), e);
                Platform.runLater(() -> {
                    statusMessage.set("查找进程失败: " + e.getMessage());
                    progress.set(0);
                });
                throw new RuntimeException("查找进程失败: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * 结束指定进程
     */
    public CompletableFuture<Boolean> killProcessAsync(int pid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Platform.runLater(() -> statusMessage.set("正在结束进程 PID: " + pid));
                
                ProcessBuilder pb;
                if (isWindows) {
                    pb = new ProcessBuilder("taskkill", "/F", "/PID", String.valueOf(pid));
                } else {
                    pb = new ProcessBuilder("kill", "-9", String.valueOf(pid));
                }
                
                Process process = pb.start();
                int exitCode = process.waitFor();
                
                boolean success = exitCode == 0;
                Platform.runLater(() -> {
                    if (success) {
                        statusMessage.set("进程 PID: " + pid + " 已结束");
                    } else {
                        statusMessage.set("结束进程 PID: " + pid + " 失败");
                    }
                });
                
                return success;
            } catch (Exception e) {
                logger.error("结束进程失败: {}", e.getMessage(), e);
                Platform.runLater(() -> statusMessage.set("结束进程失败: " + e.getMessage()));
                return false;
            }
        });
    }
    
    /**
     * 批量结束进程
     */
    public CompletableFuture<int[]> killProcessesAsync(List<Integer> pids) {
        return CompletableFuture.supplyAsync(() -> {
            int successCount = 0;
            int failCount = 0;
            
            for (int pid : pids) {
                try {
                    ProcessBuilder pb;
                    if (isWindows) {
                        pb = new ProcessBuilder("taskkill", "/F", "/PID", String.valueOf(pid));
                    } else {
                        pb = new ProcessBuilder("kill", "-9", String.valueOf(pid));
                    }
                    
                    Process process = pb.start();
                    int exitCode = process.waitFor();
                    
                    if (exitCode == 0) {
                        successCount++;
                    } else {
                        failCount++;
                    }
                } catch (Exception e) {
                    logger.error("结束进程 {} 失败: {}", pid, e.getMessage());
                    failCount++;
                }
            }
            
            final int finalSuccess = successCount;
            final int finalFail = failCount;
            Platform.runLater(() -> {
                statusMessage.set("批量结束进程完成: " + finalSuccess + " 成功, " + finalFail + " 失败");
            });
            
            return new int[]{successCount, failCount};
        });
    }
    
    /**
     * 获取 Windows 进程列表
     */
    private List<ProcessInfo> getWindowsProcessList() throws Exception {
        List<ProcessInfo> processes = new ArrayList<>();
        
        // 优先使用 Java ProcessHandle API（Java 9+）
        List<ProcessInfo> finalProcesses = processes;
        ProcessHandle.allProcesses().forEach(ph -> {
            try {
                ProcessHandle.Info info = ph.info();
                if (info != null) {
                    String name = info.command().orElse("");
                    if (name.isEmpty()) {
                        return;
                    }
                    
                    // 提取进程名
                    int lastSlash = name.lastIndexOf('\\');
                    if (lastSlash >= 0) {
                        name = name.substring(lastSlash + 1);
                    }
                    
                    long memoryKb = 0;
                    try {
                        // 尝试获取内存信息
                        memoryKb = ph.info().totalCpuDuration().isPresent() ? 
                            ph.info().totalCpuDuration().get().toMillis() / 1024 : 0;
                    } catch (Exception e) {
                        // 忽略
                    }
                    
                    ProcessInfo processInfo = ProcessInfo.builder()
                        .pid((int) ph.pid())
                        .name(name)
                        .path(info.command().orElse(""))
                        .commandLine(info.commandLine().orElse(""))
                        .memoryKb(memoryKb)
                        .status("Running")
                        .ports(new ArrayList<>())
                        .build();
                    
                    finalProcesses.add(processInfo);
                }
            } catch (Exception e) {
                // 忽略单个进程的错误
            }
        });
        
        // 如果 ProcessHandle 获取失败或数量太少，尝试使用 tasklist 作为备选
        if (processes.size() < 10) {
            processes = getWindowsProcessListByTasklist();
        }
        
        return processes;
    }
    
    /**
     * 使用 tasklist 命令获取进程列表（备选方案）
     */
    private List<ProcessInfo> getWindowsProcessListByTasklist() throws Exception {
        List<ProcessInfo> processes = new ArrayList<>();
        
        ProcessBuilder pb = new ProcessBuilder("tasklist", "/fo", "csv", "/nh");
        Process process = pb.start();
        
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(process.getInputStream(), Charset.forName("GBK")));
        
        String line;
        while ((line = reader.readLine()) != null) {
            ProcessInfo info = parseTasklistLine(line);
            if (info != null) {
                processes.add(info);
            }
        }
        
        return processes;
    }
    
    /**
     * 解析 tasklist 输出行
     * 格式: "进程名","PID","会话名","会话#","内存使用"
     */
    private ProcessInfo parseTasklistLine(String line) {
        try {
            // 解析 CSV 格式
            List<String> parts = parseCSVLine(line);
            if (parts.size() < 5) return null;
            
            String name = parts.get(0).trim();
            String pidStr = parts.get(1).trim();
            String memStr = parts.get(4).trim();
            
            if (name.isEmpty() || pidStr.isEmpty()) return null;
            
            int pid = Integer.parseInt(pidStr);
            
            // 解析内存，格式如 "123,456 K"
            long memoryKb = 0;
            memStr = memStr.replace(",", "").replace("K", "").replace(" ", "").trim();
            try {
                memoryKb = Long.parseLong(memStr);
            } catch (NumberFormatException e) {
                // 忽略
            }
            
            return ProcessInfo.builder()
                .pid(pid)
                .name(name)
                .path("")
                .commandLine("")
                .memoryKb(memoryKb)
                .status("Running")
                .ports(new ArrayList<>())
                .build();
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 解析 CSV 行
     */
    private List<String> parseCSVLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        result.add(current.toString());
        
        return result;
    }
    
    /**
     * 获取 Linux 进程列表
     */
    private List<ProcessInfo> getLinuxProcessList() throws Exception {
        List<ProcessInfo> processes = new ArrayList<>();
        
        ProcessBuilder pb = new ProcessBuilder("ps", "aux");
        Process process = pb.start();
        
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(process.getInputStream()));
        
        String line;
        boolean firstLine = true;
        while ((line = reader.readLine()) != null) {
            if (firstLine) {
                firstLine = false;
                continue;
            }
            
            ProcessInfo info = parseLinuxProcessLine(line);
            if (info != null) {
                processes.add(info);
            }
        }
        
        return processes;
    }
    
    /**
     * 解析 Linux 进程信息行
     */
    private ProcessInfo parseLinuxProcessLine(String line) {
        try {
            // ps aux 格式: USER PID %CPU %MEM VSZ RSS TTY STAT START TIME COMMAND
            String[] parts = line.split("\\s+", 11);
            if (parts.length < 11) return null;
            
            int pid = Integer.parseInt(parts[1]);
            double cpu = Double.parseDouble(parts[2]);
            long memKb = Long.parseLong(parts[5]);
            String command = parts[10];
            
            // 提取进程名
            String name = command;
            int spaceIdx = command.indexOf(' ');
            if (spaceIdx > 0) {
                name = command.substring(0, spaceIdx);
            }
            int slashIdx = name.lastIndexOf('/');
            if (slashIdx > 0) {
                name = name.substring(slashIdx + 1);
            }
            
            return ProcessInfo.builder()
                .pid(pid)
                .name(name)
                .path(command)
                .commandLine(command)
                .memoryKb(memKb)
                .cpuPercent(cpu)
                .status(parts[7])
                .ports(new ArrayList<>())
                .build();
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 获取 Windows 端口到 PID 的映射
     */
    private Map<Integer, Integer> getWindowsPortToPidMap() throws Exception {
        Map<Integer, Integer> map = new HashMap<>();
        
        ProcessBuilder pb = new ProcessBuilder("netstat", "-ano");
        Process process = pb.start();
        
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(process.getInputStream()));
        
        Pattern pattern = Pattern.compile("\\s*\\w+\\s+([\\d.]+):(\\d+)\\s+.*?(\\d+)\\s*$");
        
        String line;
        while ((line = reader.readLine()) != null) {
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                try {
                    int port = Integer.parseInt(matcher.group(2));
                    int pid = Integer.parseInt(matcher.group(3));
                    map.put(port, pid);
                } catch (NumberFormatException e) {
                    // 忽略解析错误
                }
            }
        }
        
        return map;
    }
    
    /**
     * 获取 Linux 端口到 PID 的映射
     */
    private Map<Integer, Integer> getLinuxPortToPidMap() throws Exception {
        Map<Integer, Integer> map = new HashMap<>();
        
        try {
            ProcessBuilder pb = new ProcessBuilder("ss", "-tlnp");
            Process process = pb.start();
            
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));
            
            Pattern pattern = Pattern.compile(".*:(\\d+).*pid=(\\d+)");
            
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    try {
                        int port = Integer.parseInt(matcher.group(1));
                        int pid = Integer.parseInt(matcher.group(2));
                        map.put(port, pid);
                    } catch (NumberFormatException e) {
                        // 忽略
                    }
                }
            }
        } catch (Exception e) {
            // 如果 ss 命令失败，尝试 netstat
            try {
                ProcessBuilder pb = new ProcessBuilder("netstat", "-tlnp");
                Process process = pb.start();
                
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
                
                Pattern pattern = Pattern.compile(".*:(\\d+).*?(\\d+)/");
                
                String line;
                while ((line = reader.readLine()) != null) {
                    Matcher matcher = pattern.matcher(line);
                    if (matcher.find()) {
                        try {
                            int port = Integer.parseInt(matcher.group(1));
                            int pid = Integer.parseInt(matcher.group(2));
                            map.put(port, pid);
                        } catch (NumberFormatException ex) {
                            // 忽略
                        }
                    }
                }
            } catch (Exception ex) {
                logger.warn("获取端口映射失败: {}", ex.getMessage());
            }
        }
        
        return map;
    }
    
    /**
     * 解析端口号字符串
     */
    private List<Integer> parsePorts(String portsStr) {
        List<Integer> ports = new ArrayList<>();
        if (portsStr == null || portsStr.trim().isEmpty()) {
            return ports;
        }
        
        String[] parts = portsStr.split("[,，\\s]+");
        for (String part : parts) {
            part = part.trim();
            if (part.isEmpty()) continue;
            try {
                int port = Integer.parseInt(part);
                if (port > 0 && port <= 65535) {
                    ports.add(port);
                }
            } catch (NumberFormatException e) {
                // 忽略无效端口号
            }
        }
        
        return ports;
    }
}
