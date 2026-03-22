package io.github.lemostic.toolsuite.modules.devops.logviewer;

import com.dlsc.workbenchfx.model.WorkbenchModule;
import io.github.lemostic.toolsuite.core.module.ModuleCategory;
import io.github.lemostic.toolsuite.core.module.ToolModule;
import javafx.scene.Node;
import org.kordamp.ikonli.materialdesign.MaterialDesign;

/**
 * 服务器日志查看器模块
 * 
 * 功能特性：
 * - 支持多服务器配置管理
 * - 每个服务器支持多个日志目录
 * - 目录以Tab形式展示
 * - 上下结构：上方文件列表，下方日志内容
 * - 支持加载头/尾指定行数（200/500/1000行）
 * - 支持日志搜索和复制
 */
@ToolModule(
    name = "服务器日志查看器",
    category = ModuleCategory.DEVOPS,
    menuGroup = "运维工具",
    menuGroupOrder = 20,
    description = "通过SSH连接远程服务器，实时查看和管理日志文件，支持多服务器、多目录配置",
    version = "1.0.0",
    author = "Tool Suite",
    requiresPreferences = false,
    priority = 60
)
public class LogViewerModule extends WorkbenchModule {
    
    public LogViewerModule() {
        super("服务器日志查看器", MaterialDesign.MDI_FILE_DOCUMENT_BOX);
    }
    
    @Override
    public Node activate() {
        return new LogViewerView();
    }
    
    @Override
    public boolean destroy() {
        // 清理资源
        return super.destroy();
    }
}
