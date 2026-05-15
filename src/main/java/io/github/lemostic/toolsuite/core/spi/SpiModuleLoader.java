package io.github.lemostic.toolsuite.core.spi;

import com.dlsc.workbenchfx.model.WorkbenchModule;
import io.github.lemostic.toolsuite.core.module.ModuleCategory;
import lombok.Data;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Data
public class SpiModuleLoader {
    
    private static final Logger logger = LoggerFactory.getLogger(SpiModuleLoader.class);
    
    private static final List<ToolModuleProvider> providers = new ArrayList<>();
    private static final Map<String, ToolModuleProvider.ModuleDescriptor> registeredModules = new ConcurrentHashMap<>();
    @Getter
    private static boolean loaded = false;
    
    public static void loadProviders() {
        if (loaded) {
            logger.debug("SPI providers already loaded, skipping");
            return;
        }
        
        logger.info("开始扫描 SPI 模块提供者...");
        
        ServiceLoader<ToolModuleProvider> serviceLoader = ServiceLoader.load(ToolModuleProvider.class);
        
        List<ToolModuleProvider> sortedProviders = new ArrayList<>();
        for (ToolModuleProvider provider : serviceLoader) {
            try {
                if (!provider.isEnabled()) {
                    logger.debug("跳过禁用的提供者: {}", provider.getProviderName());
                    continue;
                }
                provider.initialize();
                sortedProviders.add(provider);
                logger.info("发现 SPI 提供者: {} (版本: {}, 优先级: {})", 
                    provider.getProviderName(), 
                    provider.getVersion(),
                    provider.getPriority());
            } catch (Exception e) {
                logger.error("加载 SPI 提供者失败: {}", provider.getClass().getName(), e);
            }
        }
        
        sortedProviders.sort(Comparator.comparingInt(ToolModuleProvider::getPriority));
        providers.clear();
        providers.addAll(sortedProviders);
        
        for (ToolModuleProvider provider : providers) {
            registerProviderModules(provider);
        }
        
        loaded = true;
        logger.info("SPI 扫描完成，共发现 {} 个提供者, 注册了 {} 个模块", 
            providers.size(), registeredModules.size());
    }
    
    private static void registerProviderModules(ToolModuleProvider provider) {
        List<Class<? extends WorkbenchModule>> modules = provider.getModuleClasses();
        if (modules != null && !modules.isEmpty()) {
            logger.info("提供者 {} 提供 {} 个模块:", provider.getProviderName(), modules.size());
            for (Class<? extends WorkbenchModule> moduleClass : modules) {
                try {
                    ToolModuleProvider.ModuleDescriptor descriptor = createDescriptor(provider, moduleClass);
                    if (descriptor != null) {
                        registeredModules.put(moduleClass.getName(), descriptor);
                        logger.info("  ✓ {} - {} ({})", 
                            moduleClass.getSimpleName(), 
                            descriptor.getName(),
                            descriptor.getCategory());
                    }
                } catch (Exception e) {
                    logger.error("  ✗ 注册模块失败: {} - {}", 
                        moduleClass.getSimpleName(), e.getMessage());
                }
            }
        }
    }
    
    private static ToolModuleProvider.ModuleDescriptor createDescriptor(
            ToolModuleProvider provider, Class<? extends WorkbenchModule> moduleClass) {
        
        try {
            WorkbenchModule instance = moduleClass.getDeclaredConstructor().newInstance();
            io.github.lemostic.toolsuite.core.module.ToolModule annotation = 
                moduleClass.getAnnotation(io.github.lemostic.toolsuite.core.module.ToolModule.class);
            
            if (annotation != null) {
                return ToolModuleProvider.ModuleDescriptor.builder()
                    .moduleClass(moduleClass)
                    .name(annotation.name())
                    .category(annotation.category())
                    .menuGroup(annotation.menuGroup())
                    .menuGroupOrder(annotation.menuGroupOrder())
                    .description(annotation.description())
                    .version(annotation.version())
                    .author(annotation.author())
                    .enabled(annotation.enabled())
                    .priority(annotation.priority())
                    .metadata(Map.of(
                        "provider", provider.getProviderName(),
                        "providerVersion", provider.getVersion(),
                        "author", annotation.author()
                    ))
                    .moduleFactory(() -> instance)
                    .build();
            } else {
                return ToolModuleProvider.ModuleDescriptor.builder()
                    .moduleClass(moduleClass)
                    .name(moduleClass.getSimpleName())
                    .category(ModuleCategory.OTHERS)
                    .enabled(true)
                    .priority(100)
                    .metadata(Map.of("provider", provider.getProviderName()))
                    .moduleFactory(() -> instance)
                    .build();
            }
        } catch (Exception e) {
            logger.error("创建模块描述符失败: {}", e.getMessage());
            return null;
        }
    }
    
    public static List<Class<? extends WorkbenchModule>> getAllSpiModules() {
        return registeredModules.values().stream()
            .filter(d -> d.isEnabled())
            .map(ToolModuleProvider.ModuleDescriptor::getModuleClass)
            .collect(Collectors.toList());
    }
    
    public static List<ToolModuleProvider.ModuleDescriptor> getAllDescriptors() {
        return new ArrayList<>(registeredModules.values());
    }
    
    public static List<ToolModuleProvider.ModuleDescriptor> getDescriptors(Predicate<ToolModuleProvider.ModuleDescriptor> filter) {
        return registeredModules.values().stream()
            .filter(filter)
            .collect(Collectors.toList());
    }
    
    public static List<ToolModuleProvider.ModuleDescriptor> getDescriptorsByCategory(ModuleCategory category) {
        return getDescriptors(d -> d.getCategory() == category);
    }
    
    public static List<ToolModuleProvider.ModuleDescriptor> getDescriptorsByProvider(String providerName) {
        return getDescriptors(d -> 
            d.getMetadata().get("provider") != null && 
            d.getMetadata().get("provider").equals(providerName));
    }
    
    public static ToolModuleProvider.ModuleDescriptor getDescriptor(String moduleName) {
        return registeredModules.get(moduleName);
    }
    
    public static WorkbenchModule createModule(String moduleName) {
        ToolModuleProvider.ModuleDescriptor descriptor = registeredModules.get(moduleName);
        if (descriptor != null) {
            return descriptor.createModule();
        }
        return null;
    }
    
    public static void registerModule(ToolModuleProvider.ModuleDescriptor descriptor) {
        if (descriptor != null && descriptor.getModuleClass() != null) {
            registeredModules.put(descriptor.getModuleClass().getName(), descriptor);
            logger.info("手动注册模块: {}", descriptor.getName());
        }
    }
    
    public static void unregisterModule(String moduleName) {
        registeredModules.remove(moduleName);
        logger.info("取消注册模块: {}", moduleName);
    }
    
    public static List<ToolModuleProvider> getProviders() {
        return Collections.unmodifiableList(providers);
    }
    
    public static void destroyProviders() {
        logger.info("开始销毁 SPI 提供者...");
        
        for (ToolModuleProvider provider : providers) {
            try {
                provider.destroy();
                logger.debug("销毁提供者: {}", provider.getProviderName());
            } catch (Exception e) {
                logger.error("销毁提供者失败: {}", provider.getProviderName(), e);
            }
        }
        
        providers.clear();
        registeredModules.clear();
        loaded = false;
        logger.info("SPI 提供者销毁完成");
    }
    
    public static void reload() {
        destroyProviders();
        loadProviders();
    }
    
    public static int getProviderCount() {
        return providers.size();
    }
    
    public static int getModuleCount() {
        return registeredModules.size();
    }

}