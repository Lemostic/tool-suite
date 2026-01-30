package io.github.lemostic.toolsuite.core.module;

import com.dlsc.workbenchfx.model.WorkbenchModule;
import io.github.lemostic.toolsuite.modules.preferences.Preferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 模块注册表 - 统一管理所有工具模块
 * 支持注解驱动的模块自动发现和加载，以及按菜单分组生成抽屉树
 */
public class ModuleRegistry {
    
    private static final Logger logger = LoggerFactory.getLogger(ModuleRegistry.class);
    
    /**
     * 已注册的模块类列表
     */
    private static final List<Class<? extends WorkbenchModule>> REGISTERED_MODULES = new ArrayList<>();
    
    /**
     * 注册模块
     * @param moduleClass 模块类
     */
    public static void register(Class<? extends WorkbenchModule> moduleClass) {
        if (!REGISTERED_MODULES.contains(moduleClass)) {
            REGISTERED_MODULES.add(moduleClass);
            logger.debug("注册模块: {}", moduleClass.getName());
        }
    }
    
    /**
     * 批量注册模块
     * @param moduleClasses 模块类列表
     */
    @SafeVarargs
    public static void registerAll(Class<? extends WorkbenchModule>... moduleClasses) {
        for (Class<? extends WorkbenchModule> moduleClass : moduleClasses) {
            register(moduleClass);
        }
    }
    
    /**
     * 获取所有已注册的模块
     */
    public static List<Class<? extends WorkbenchModule>> getRegisteredModules() {
        return Collections.unmodifiableList(REGISTERED_MODULES);
    }
    
    /**
     * 根据分类获取模块
     */
    public static List<Class<? extends WorkbenchModule>> getModulesByCategory(ModuleCategory category) {
        return REGISTERED_MODULES.stream()
                .filter(clazz -> {
                    ToolModule annotation = clazz.getAnnotation(ToolModule.class);
                    return annotation != null && annotation.category() == category;
                })
                .collect(Collectors.toList());
    }
    
    /**
     * 加载所有已注册的模块实例
     * @param preferences 偏好设置
     * @return 模块实例列表
     */
    public static List<WorkbenchModule> loadAllModules(Preferences preferences) {
        List<ModuleInfo> moduleInfos = new ArrayList<>();
        
        // 收集模块信息并排序
        for (Class<? extends WorkbenchModule> moduleClass : REGISTERED_MODULES) {
            ToolModule annotation = moduleClass.getAnnotation(ToolModule.class);
            
            // 检查是否启用
            if (annotation != null && !annotation.enabled()) {
                logger.info("模块 {} 已禁用，跳过加载", moduleClass.getSimpleName());
                continue;
            }
            
            int priority = annotation != null ? annotation.priority() : 100;
            boolean requiresPreferences = annotation != null && annotation.requiresPreferences();
            
            moduleInfos.add(new ModuleInfo(moduleClass, priority, requiresPreferences));
        }
        
        // 按优先级排序
        moduleInfos.sort(Comparator.comparingInt(m -> m.priority));
        
        // 创建模块实例
        List<WorkbenchModule> modules = new ArrayList<>();
        for (ModuleInfo info : moduleInfos) {
            try {
                WorkbenchModule module = createModuleInstance(info.moduleClass, info.requiresPreferences, preferences);
                if (module != null) {
                    modules.add(module);
                    logger.info("成功加载模块: {} (优先级: {})", module.getName(), info.priority);
                }
            } catch (Exception e) {
                logger.error("加载模块失败: {}", info.moduleClass.getName(), e);
            }
        }
        
        logger.info("共加载 {} 个模块", modules.size());
        return modules;
    }
    
    /**
     * 创建模块实例
     */
    private static WorkbenchModule createModuleInstance(
            Class<? extends WorkbenchModule> moduleClass,
            boolean requiresPreferences,
            Preferences preferences) throws Exception {
        
        // 尝试带 Preferences 参数的构造器
        if (requiresPreferences) {
            try {
                Constructor<? extends WorkbenchModule> constructor = 
                        moduleClass.getConstructor(Preferences.class);
                return constructor.newInstance(preferences);
            } catch (NoSuchMethodException e) {
                logger.warn("模块 {} 配置为需要 Preferences，但未找到对应构造函数，尝试使用无参构造", 
                        moduleClass.getSimpleName());
            }
        }
        
        // 尝试无参构造器
        Constructor<? extends WorkbenchModule> constructor = moduleClass.getConstructor();
        return constructor.newInstance();
    }
    
    /**
     * 模块信息内部类
     */
    private static class ModuleInfo {
        final Class<? extends WorkbenchModule> moduleClass;
        final int priority;
        final boolean requiresPreferences;
        
        ModuleInfo(Class<? extends WorkbenchModule> moduleClass, int priority, boolean requiresPreferences) {
            this.moduleClass = moduleClass;
            this.priority = priority;
            this.requiresPreferences = requiresPreferences;
        }
    }
    
    /**
     * 按菜单分组整理模块，供左侧抽屉生成分组树。
     * 分组名：注解中 menuGroup 非空用 menuGroup，否则用 category 的显示名。
     * 分组顺序：按 menuGroupOrder 升序，同组内保持传入的 modules 顺序。
     *
     * @param modules 已加载的模块实例列表
     * @return 有序的「分组名 + 该组模块列表」，可直接用于构建 Menu(MenuItem)
     */
    public static List<GroupedModules> getGroupedModulesForDrawer(List<WorkbenchModule> modules) {
        if (modules == null || modules.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, GroupedModules> groupMap = new LinkedHashMap<>();
        for (WorkbenchModule module : modules) {
            ToolModule ann = module.getClass().getAnnotation(ToolModule.class);
            String groupName = (ann != null && ann.menuGroup() != null && !ann.menuGroup().isEmpty())
                    ? ann.menuGroup()
                    : (ann != null ? ann.category().getDisplayName() : "其他");
            int groupOrder = ann != null ? ann.menuGroupOrder() : 100;
            GroupedModules g = groupMap.computeIfAbsent(groupName, k -> new GroupedModules(groupName, groupOrder, new ArrayList<>()));
        g.getModules().add(module);
        }
        List<GroupedModules> result = new ArrayList<>(groupMap.values());
        result.sort(Comparator.comparingInt(GroupedModules::getGroupOrder));
        return result;
    }

    /**
     * 左侧抽屉用：一个菜单分组（显示名 + 排序权重 + 该组下的模块列表）
     */
    public static final class GroupedModules {
        private final String groupName;
        private final int groupOrder;
        private final List<WorkbenchModule> modules;

        public GroupedModules(String groupName, int groupOrder, List<WorkbenchModule> modules) {
            this.groupName = groupName;
            this.groupOrder = groupOrder;
            this.modules = modules;
        }

        public String getGroupName() { return groupName; }
        public int getGroupOrder() { return groupOrder; }
        public List<WorkbenchModule> getModules() { return modules; }
    }

    /**
     * 清空注册表（主要用于测试）
     */
    public static void clear() {
        REGISTERED_MODULES.clear();
    }
}
