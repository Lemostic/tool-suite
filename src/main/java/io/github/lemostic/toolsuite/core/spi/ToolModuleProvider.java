package io.github.lemostic.toolsuite.core.spi;

import com.dlsc.workbenchfx.model.WorkbenchModule;

import java.util.List;

/**
 * 工具模块提供者接口（SPI）
 * 
 * 外部插件需要实现此接口，并通过 SPI 机制注册
 * 
 * 使用方式：
 * 1. 在独立的 JAR 项目中实现此接口
 * 2. 在 META-INF/services 目录下创建文件：
 *    io.github.lemostic.toolsuite.core.spi.ToolModuleProvider
 * 3. 文件内容为实现类的全限定名
 * 4. 将 JAR 放入 classpath，模块会自动被加载
 * 
 * 示例：
 * <pre>
 * {@code
 * public class MyPluginProvider implements ToolModuleProvider {
 *     @Override
 *     public List<Class<? extends WorkbenchModule>> getModuleClasses() {
 *         return Arrays.asList(
 *             MyCustomModule1.class,
 *             MyCustomModule2.class
 *         );
 *     }
 *     
 *     @Override
 *     public String getProviderName() {
 *         return "我的插件包";
 *     }
 *     
 *     @Override
 *     public String getVersion() {
 *         return "1.0.0";
 *     }
 * }
 * }
 * </pre>
 */
public interface ToolModuleProvider {
    
    /**
     * 获取此提供者提供的所有模块类
     * @return 模块类列表
     */
    List<Class<? extends WorkbenchModule>> getModuleClasses();
    
    /**
     * 获取提供者名称
     * @return 提供者名称
     */
    default String getProviderName() {
        return "Unknown Provider";
    }
    
    /**
     * 获取提供者版本
     * @return 版本号
     */
    default String getVersion() {
        return "1.0.0";
    }
    
    /**
     * 获取提供者描述
     * @return 描述信息
     */
    default String getDescription() {
        return "";
    }
    
    /**
     * 提供者初始化回调
     * 在模块加载前调用，可用于初始化资源
     */
    default void initialize() {
        // 默认不做任何操作
    }
    
    /**
     * 提供者销毁回调
     * 在应用关闭时调用，可用于清理资源
     */
    default void destroy() {
        // 默认不做任何操作
    }
}
