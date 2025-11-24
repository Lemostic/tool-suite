package io.github.lemostic.toolsuite.core;

import com.dlsc.workbenchfx.model.WorkbenchModule;
import io.github.lemostic.toolsuite.core.module.ModuleRegistry;
import io.github.lemostic.toolsuite.core.spi.SpiModuleLoader;
import io.github.lemostic.toolsuite.modules.preferences.Preferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 模块加载器 - 支持双模式加载
 * 
 * 加载模式：
 * 1. 内置模块：通过 ModuleRegistry 注册的模块
 * 2. SPI 插件：通过 Java SPI 机制动态发现的模块
 * 
 * 新架构使用方式：
 * 1. 内置模块：在 registerModules() 中注册
 * 2. SPI 插件：实现 ToolModuleProvider 接口，放入 classpath
 */
public class ModuleLoader {
    
    private static final Logger logger = LoggerFactory.getLogger(ModuleLoader.class);
    
    /**
     * 注册所有内置模块
     * 新增内置模块只需在此处添加注册即可
     */
    public static void registerModules() {
        logger.info("开始注册内置模块...");
        
        // ==================== 系统管理类 ====================
        ModuleRegistry.register(io.github.lemostic.toolsuite.modules.preferences.PreferencesModule.class);
        
        // ==================== 开发工具类 ====================
        // HelloWorldModule 已改为通过 SPI 加载，见 BuiltinDevToolsProvider
        // ModuleRegistry.register(io.github.lemostic.toolsuite.modules.helloworld.HelloWorldModule.class);
        
        // ==================== 文件工具类 ====================
        ModuleRegistry.register(io.github.lemostic.toolsuite.modules.file.zipclean.ZipCleanModule.class);
        
        // ==================== 数据库工具类 ====================
        // 未来添加：
        // ModuleRegistry.register(io.github.lemostic.toolsuite.modules.database.mysql.MySQLClientModule.class);
        // ModuleRegistry.register(io.github.lemostic.toolsuite.modules.database.redis.RedisClientModule.class);
        
        // ==================== 数据处理类 ====================
        // 未来添加：
        // ModuleRegistry.register(io.github.lemostic.toolsuite.modules.data.DataMigrateModule.class);
        
        // ==================== 运维工具类 ====================
        // 未来添加：
        // ModuleRegistry.register(io.github.lemostic.toolsuite.modules.devops.PackageDeploymentModule.class);
        
        // ==================== 网络工具类 ====================
        // 未来添加：
        // ModuleRegistry.register(io.github.lemostic.toolsuite.modules.network.HttpClientModule.class);
        
        logger.info("内置模块注册完成，共注册 {} 个模块", ModuleRegistry.getRegisteredModules().size());
    }
    
    /**
     * 加载所有 SPI 插件模块
     */
    public static void loadSpiModules() {
        logger.info("开始加载 SPI 插件...");
        
        // 1. 扫描并加载 SPI 提供者
        SpiModuleLoader.loadProviders();
        
        // 2. 获取所有 SPI 模块并注册到注册表
        List<Class<? extends WorkbenchModule>> spiModules = SpiModuleLoader.getAllSpiModules();
        for (Class<? extends WorkbenchModule> moduleClass : spiModules) {
            ModuleRegistry.register(moduleClass);
        }
        
        logger.info("SPI 插件加载完成，共加载 {} 个模块", spiModules.size());
    }
    
    /**
     * 加载所有模块（内置 + SPI）
     * @param preferences 偏好设置（某些模块需要）
     * @return 模块列表
     */
    public static List<WorkbenchModule> loadModules(Preferences preferences) {
        logger.info("开始加载所有模块...");
        
        // 1. 注册内置模块
        if (ModuleRegistry.getRegisteredModules().isEmpty()) {
            registerModules();
        }
        
        // 2. 加载 SPI 插件模块
        loadSpiModules();
        
        // 3. 从注册表加载所有模块（内置 + SPI）
        List<WorkbenchModule> modules = ModuleRegistry.loadAllModules(preferences);
        
        logger.info("所有模块加载完成，共 {} 个模块", modules.size());
        return modules;
    }
    
    /**
     * 销毁所有 SPI 提供者
     */
    public static void destroySpiProviders() {
        SpiModuleLoader.destroyProviders();
    }
}
