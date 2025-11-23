package io.github.lemostic.toolsuite.core.module;

import java.lang.annotation.*;

/**
 * 工具模块注解
 * 用于声明式配置模块的元数据信息
 * 
 * 使用示例：
 * <pre>
 * {@code
 * @ToolModule(
 *     name = "MySQL客户端",
 *     category = ModuleCategory.DATABASE,
 *     description = "MySQL数据库连接和查询工具",
 *     version = "1.0.0",
 *     author = "lemostic",
 *     requiresPreferences = false
 * )
 * public class MySQLClientModule extends WorkbenchModule {
 *     // ...
 * }
 * }
 * </pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ToolModule {
    
    /**
     * 模块名称（必填）
     */
    String name();
    
    /**
     * 模块分类（必填）
     */
    ModuleCategory category();
    
    /**
     * 模块描述
     */
    String description() default "";
    
    /**
     * 模块版本
     */
    String version() default "1.0.0";
    
    /**
     * 作者
     */
    String author() default "";
    
    /**
     * 是否需要 Preferences 依赖
     */
    boolean requiresPreferences() default false;
    
    /**
     * 是否默认启用
     */
    boolean enabled() default true;
    
    /**
     * 加载优先级（数字越小优先级越高）
     */
    int priority() default 100;
}
