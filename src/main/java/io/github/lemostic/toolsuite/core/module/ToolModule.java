package io.github.lemostic.toolsuite.core.module;

import java.lang.annotation.*;

/**
 * 工具模块注解
 * 用于声明式配置模块的元数据信息，支持左侧抽屉的菜单分组。
 *
 * <p>菜单分组：{@link #menuGroup()} 为空时使用 {@link #category()} 的显示名作为抽屉分组名；
 * 非空时使用 menuGroup 作为分组名，便于同一 category 下再细分（如「开发/格式化」「开发/加解密」）。
 * 分组排序使用 {@link #menuGroupOrder()}，数值越小越靠前。
 *
 * <p>使用示例：
 * <pre>
 * {@code
 * @ToolModule(
 *     name = "MySQL客户端",
 *     category = ModuleCategory.DATABASE,
 *     menuGroup = "数据库",
 *     menuGroupOrder = 10,
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
     * 模块分类（必填），用于抽屉分组与图标等
     */
    ModuleCategory category();

    /**
     * 左侧抽屉菜单分组显示名。为空时使用 {@link #category()} 的显示名
     */
    String menuGroup() default "";

    /**
     * 菜单分组排序权重，数值越小在抽屉中越靠前
     */
    int menuGroupOrder() default 100;

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
