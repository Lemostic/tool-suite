package io.github.lemostic.toolsuite.modules.devops.deploy.task;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class TaskPipeline {
    private final String name;
    private final List<TaskStep> steps = new ArrayList<>();
    private final TaskContext context = new TaskContext();
    
    private Consumer<String> logConsumer = System.out::println;
    private BiConsumer<TaskStep, TaskResult> stepCallback;
    private Consumer<TaskResult> completionCallback;
    private volatile boolean running = false;
    
    public TaskPipeline(String name) {
        this.name = name;
    }
    
    public TaskPipeline addStep(TaskStep step) {
        steps.add(step);
        return this;
    }
    
    public TaskPipeline onLog(Consumer<String> logConsumer) {
        this.logConsumer = logConsumer;
        return this;
    }
    
    public TaskPipeline onStepComplete(BiConsumer<TaskStep, TaskResult> callback) {
        this.stepCallback = callback;
        return this;
    }
    
    public TaskPipeline onComplete(Consumer<TaskResult> callback) {
        this.completionCallback = callback;
        return this;
    }
    
    public CompletableFuture<TaskResult> execute() {
        if (running) {
            throw new IllegalStateException("Pipeline is already running");
        }
        running = true;
        
        return CompletableFuture.supplyAsync(() -> {
            log("[Pipeline] 开始执行: " + name);
            
            for (TaskStep step : steps) {
                if (context.isCancelled()) {
                    log("[Pipeline] 任务已取消");
                    return TaskResult.failure("任务已取消");
                }
                
                log("[Step] 开始: " + step.getName() + " - " + step.getDescription());
                
                TaskResult result;
                try {
                    result = step.execute(context, logConsumer);
                } catch (Exception e) {
                    result = TaskResult.failure("执行异常: " + e.getMessage(), e);
                }
                
                if (stepCallback != null) {
                    stepCallback.accept(step, result);
                }
                
                if (!result.isSuccess()) {
                    log("[Step] 失败: " + step.getName() + " - " + result.getMessage());
                    
                    // 尝试回滚
                    rollback(step);
                    
                    if (completionCallback != null) {
                        completionCallback.accept(result);
                    }
                    return result;
                }
                
                log("[Step] 完成: " + step.getName());
            }
            
            TaskResult success = TaskResult.success("所有步骤执行完成");
            log("[Pipeline] 执行完成: " + name);
            
            if (completionCallback != null) {
                completionCallback.accept(success);
            }
            
            return success;
            
        }).whenComplete((result, throwable) -> {
            running = false;
        });
    }
    
    private void rollback(TaskStep failedStep) {
        log("[Pipeline] 开始回滚...");
        
        int failedIndex = steps.indexOf(failedStep);
        for (int i = failedIndex - 1; i >= 0; i--) {
            TaskStep step = steps.get(i);
            if (step.canRollback()) {
                log("[Rollback] 回滚: " + step.getName());
                try {
                    TaskResult result = step.rollback(context, logConsumer);
                    if (!result.isSuccess()) {
                        log("[Rollback] 回滚失败: " + step.getName() + " - " + result.getMessage());
                    }
                } catch (Exception e) {
                    log("[Rollback] 回滚异常: " + step.getName() + " - " + e.getMessage());
                }
            }
        }
    }
    
    public void cancel() {
        context.cancel();
        log("[Pipeline] 取消请求已发送");
    }
    
    public boolean isRunning() {
        return running;
    }
    
    public TaskContext getContext() {
        return context;
    }
    
    private void log(String message) {
        if (logConsumer != null) {
            logConsumer.accept(message);
        }
    }
}
