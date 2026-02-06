package io.github.lemostic.toolsuite.modules.devops.deploy.task;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TaskContext {
    private final Map<String, Object> attributes = new ConcurrentHashMap<>();
    private volatile boolean cancelled = false;
    
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }
    
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) {
        return (T) attributes.get(key);
    }
    
    public void cancel() {
        this.cancelled = true;
    }
    
    public boolean isCancelled() {
        return cancelled;
    }
    
    public void clear() {
        attributes.clear();
    }
}
