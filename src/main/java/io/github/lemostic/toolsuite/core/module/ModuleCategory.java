package io.github.lemostic.toolsuite.core.module;

/**
 * 模块分类枚举
 * 用于对工具模块进行分类管理，便于组织和导航
 */
public enum ModuleCategory {
    
    /** 系统管理类 - 偏好设置、系统配置等 */
    SYSTEM("系统管理", "mdi-cog"),
    
    /** 数据库工具类 - MySQL、PostgreSQL、Redis等数据库相关工具 */
    DATABASE("数据库工具", "mdi-database"),
    
    /** 数据处理类 - 数据迁移、转换、清洗等 */
    DATA_PROCESS("数据处理", "mdi-table-edit"),
    
    /** 开发工具类 - 代码生成、格式化、加解密等 */
    DEV_TOOLS("开发工具", "mdi-code-tags"),
    
    /** 网络工具类 - HTTP客户端、接口测试、抓包等 */
    NETWORK("网络工具", "mdi-web"),
    
    /** 文件工具类 - 文件对比、批量处理、格式转换等 */
    FILE_TOOLS("文件工具", "mdi-file-multiple"),
    
    /** 运维工具类 - SSH、部署、监控等 */
    DEVOPS("运维工具", "mdi-server"),
    
    /** 搜索引擎工具类 - Elasticsearch、Solr等 */
    SEARCH_ENGINE("搜索引擎", "mdi-magnify"),
    
    /** 消息队列工具类 - Kafka、RabbitMQ等 */
    MESSAGE_QUEUE("消息队列", "mdi-message-processing"),
    
    /** 其他工具类 */
    OTHERS("其他工具", "mdi-puzzle");
    
    private final String displayName;
    private final String iconCode;
    
    ModuleCategory(String displayName, String iconCode) {
        this.displayName = displayName;
        this.iconCode = iconCode;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getIconCode() {
        return iconCode;
    }
}
