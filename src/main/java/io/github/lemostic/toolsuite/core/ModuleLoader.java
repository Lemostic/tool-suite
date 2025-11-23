package io.github.lemostic.toolsuite.core;

import com.dlsc.workbenchfx.model.WorkbenchModule;
import io.github.lemostic.toolsuite.modules.preferences.Preferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

/**
 * 模块加载器 - 自动扫描并加载工具模块
 */
public class ModuleLoader {
    
    private static final Logger logger = LoggerFactory.getLogger(ModuleLoader.class);
    
    // 模块包扫描路径
    private static final String MODULE_PACKAGE = "io.github.lemostic.toolsuite.modules";
    
    /**
     * 加载所有模块
     * @param preferences 偏好设置（某些模块需要）
     * @return 模块列表
     */
    public static List<WorkbenchModule> loadModules(Preferences preferences) {
        List<WorkbenchModule> modules = new ArrayList<>();
        
        // 定义需要加载的模块类（避免使用反射扫描，明确声明更可靠）
        List<ModuleConfig> moduleConfigs = getModuleConfigs();
        
        for (ModuleConfig config : moduleConfigs) {
            try {
                WorkbenchModule module = createModule(config, preferences);
                if (module != null) {
                    modules.add(module);
                    logger.info("成功加载模块: {}", module.getName());
                }
            } catch (Exception e) {
                logger.error("加载模块失败: {}", config.className, e);
            }
        }
        
        logger.info("共加载 {} 个模块", modules.size());
        return modules;
    }
    
    /**
     * 创建模块实例
     */
    private static WorkbenchModule createModule(ModuleConfig config, Preferences preferences) throws Exception {
        Class<?> clazz = Class.forName(config.className);
        
        // 尝试带 Preferences 参数的构造器
        if (config.requiresPreferences) {
            try {
                Constructor<?> constructor = clazz.getConstructor(Preferences.class);
                return (WorkbenchModule) constructor.newInstance(preferences);
            } catch (NoSuchMethodException e) {
                logger.warn("模块 {} 配置为需要 Preferences，但未找到对应构造函数，尝试使用无参构造", config.className);
            }
        }
        
        // 尝试无参构造器
        Constructor<?> constructor = clazz.getConstructor();
        return (WorkbenchModule) constructor.newInstance();
    }
    
    /**
     * 获取模块配置列表
     * 新增模块只需在此处添加配置即可
     */
    private static List<ModuleConfig> getModuleConfigs() {
        List<ModuleConfig> configs = new ArrayList<>();
        
        // 添加模块配置 - 新增模块只需在此处添加一行即可
        configs.add(ModuleConfig.of(io.github.lemostic.toolsuite.modules.helloworld.HelloWorldModule.class));
        configs.add(ModuleConfig.of(io.github.lemostic.toolsuite.modules.preferences.PreferencesModule.class, true));
        // 未来新增模块示例：
        // configs.add(ModuleConfig.of(io.github.lemostic.toolsuite.modules.datamigrator.DataMigrateModule.class, true));
        // configs.add(ModuleConfig.of(io.github.lemostic.toolsuite.modules.deployment.PackageDeploymentModule.class, true));
        
        return configs;
    }
    
    /**
     * 模块配置类
     */
    private static class ModuleConfig {
        String className;
        boolean requiresPreferences;

        private ModuleConfig(String className, boolean requiresPreferences) {
            this.className = className;
            this.requiresPreferences = requiresPreferences;
        }
        
        /**
         * 创建不需要 Preferences 参数的模块配置
         */
        static ModuleConfig of(Class<? extends WorkbenchModule> moduleClass) {
            return new ModuleConfig(moduleClass.getName(), false);
        }
        
        /**
         * 创建模块配置
         * @param moduleClass 模块类
         * @param requiresPreferences 是否需要 Preferences 参数
         */
        static ModuleConfig of(Class<? extends WorkbenchModule> moduleClass, boolean requiresPreferences) {
            return new ModuleConfig(moduleClass.getName(), requiresPreferences);
        }
    }
}
