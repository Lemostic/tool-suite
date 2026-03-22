package io.github.lemostic.toolsuite.modules.devops.deploy.task;

import java.util.Map;
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

    static TaskStep createFromConfig(Map<String, Object> config) {
        // 根据配置创建相应的 TaskStep 实现类实例
        // 注意: 这里需要你根据实际情况实现具体的逻辑
        throw new UnsupportedOperationException("请实现此方法");
    }
}
