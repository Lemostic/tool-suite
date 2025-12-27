package io.github.lemostic.toolsuite.modules.devtools;

import com.dlsc.workbenchfx.model.WorkbenchModule;
import io.github.lemostic.toolsuite.core.spi.ToolModuleProvider;
import io.github.lemostic.toolsuite.modules.helloworld.HelloWorldModule;
import io.github.lemostic.toolsuite.modules.search.es.EsQueryModule;

import java.util.Arrays;
import java.util.List;

/**
 * 内置开发工具提供者
 * 
 * 这是一个示例，展示如何通过 SPI 方式提供模块
 * 实际上内置模块也可以通过 SPI 方式加载
 */
public class BuiltinDevToolsProvider implements ToolModuleProvider {
    
    @Override
    public List<Class<? extends WorkbenchModule>> getModuleClasses() {
        return Arrays.asList(
            HelloWorldModule.class,
            EsQueryModule.class
            // 未来可以添加更多开发工具模块
            // JsonFormatterModule.class,
            // Base64EncoderModule.class,
            // RegexTesterModule.class
        );
    }
    
    @Override
    public String getProviderName() {
        return "内置开发工具集";
    }
    
    @Override
    public String getVersion() {
        return "1.0.0";
    }
    
    @Override
    public String getDescription() {
        return "提供基础的开发辅助工具";
    }
    
    @Override
    public void initialize() {
        // 可以在这里进行初始化操作
        System.out.println("内置开发工具集初始化...");
    }
    
    @Override
    public void destroy() {
        // 可以在这里进行清理操作
        System.out.println("内置开发工具集清理...");
    }
}
