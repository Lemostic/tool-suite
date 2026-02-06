package io.github.lemostic.toolsuite.modules.devops.deploy.task;

import java.util.function.Consumer;

public interface TaskStep {
    String getName();
    String getDescription();
    TaskResult execute(TaskContext context, Consumer<String> logConsumer);
    
    default boolean canRollback() {
        return false;
    }
    
    default TaskResult rollback(TaskContext context, Consumer<String> logConsumer) {
        return TaskResult.success("无需回滚");
    }
}
