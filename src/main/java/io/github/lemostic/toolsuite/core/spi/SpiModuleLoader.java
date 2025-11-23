package io.github.lemostic.toolsuite.core.spi;

import com.dlsc.workbenchfx.model.WorkbenchModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * SPI 模块加载器
 * 通过 Java SPI 机制自动发现和加载外部插件
 */
public class SpiModuleLoader {
    
    private static final Logger logger = LoggerFactory.getLogger(SpiModuleLoader.class);
    
    private static final List<ToolModuleProvider> providers = new ArrayList<>();
    
    /**
     * 加载所有 SPI 提供者
     */
    public static void loadProviders() {
        logger.info("开始扫描 SPI 模块提供者...");
        
        ServiceLoader<ToolModuleProvider> serviceLoader = ServiceLoader.load(ToolModuleProvider.class);
        
        int count = 0;
        for (ToolModuleProvider provider : serviceLoader) {
            try {
                provider.initialize();
                providers.add(provider);
                count++;
                
                logger.info("发现 SPI 提供者: {} (版本: {}, 描述: {})", 
                    provider.getProviderName(), 
                    provider.getVersion(),
                    provider.getDescription());
                
                // 记录提供的模块
                List<Class<? extends WorkbenchModule>> modules = provider.getModuleClasses();
                if (modules != null && !modules.isEmpty()) {
                    logger.info("  提供 {} 个模块:", modules.size());
                    for (Class<? extends WorkbenchModule> moduleClass : modules) {
                        logger.info("    - {}", moduleClass.getSimpleName());
                    }
                }
            } catch (Exception e) {
                logger.error("加载 SPI 提供者失败: {}", provider.getClass().getName(), e);
            }
        }
        
        logger.info("SPI 扫描完成，共发现 {} 个提供者", count);
    }
    
    /**
     * 获取所有 SPI 提供的模块类
     */
    public static List<Class<? extends WorkbenchModule>> getAllSpiModules() {
        List<Class<? extends WorkbenchModule>> allModules = new ArrayList<>();
        
        for (ToolModuleProvider provider : providers) {
            try {
                List<Class<? extends WorkbenchModule>> modules = provider.getModuleClasses();
                if (modules != null) {
                    allModules.addAll(modules);
                }
            } catch (Exception e) {
                logger.error("获取提供者模块失败: {}", provider.getProviderName(), e);
            }
        }
        
        return allModules;
    }
    
    /**
     * 获取所有已加载的提供者
     */
    public static List<ToolModuleProvider> getProviders() {
        return Collections.unmodifiableList(providers);
    }
    
    /**
     * 销毁所有提供者
     */
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
        logger.info("SPI 提供者销毁完成");
    }
}
