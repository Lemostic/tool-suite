package com.lemostic.work.modules.datamigrator.service;

import java.util.function.Consumer;

public interface MigrationService {
    
    void migrate() throws Exception;
    
    void stop();
    
    void setOnProgressUpdate(Consumer<Double> listener);
    void setOnStatusChange(Consumer<String> listener);
    
    // 其他扩展方法...
}