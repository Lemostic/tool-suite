package io.github.lemostic.toolsuite.modules.devtools.processmanager;

import com.dlsc.workbenchfx.model.WorkbenchModule;
import io.github.lemostic.toolsuite.core.module.ModuleCategory;
import io.github.lemostic.toolsuite.core.module.ToolModule;
import javafx.scene.Node;
import org.kordamp.ikonli.materialdesign.MaterialDesign;

/**
 * 进程管理器模块
 * 
 * 功能特性：
 * - 查看系统进程列表
 * - 通过端口号查找进程（支持多个端口）
 * - 通过进程名称筛选
 * - 结束单个进程
 * - 批量结束进程
 * - 显示进程占用的端口、内存等信息
 */
@ToolModule(
    name = "进程管理器",
    category = ModuleCategory.DEV_TOOLS,
    menuGroup = "开发工具",
    menuGroupOrder = 25,
    description = "管理系统进程，支持通过端口号查找进程、批量结束进程等功能",
    version = "1.0.0",
    author = "Tool Suite",
    requiresPreferences = false,
    priority = 55
)
public class ProcessManagerModule extends WorkbenchModule {
    
    public ProcessManagerModule() {
        super("进程管理器", MaterialDesign.MDI_APPLICATION);
    }
    
    @Override
    public Node activate() {
        return new ProcessManagerView();
    }
    
    @Override
    public boolean destroy() {
        // 清理资源
        return super.destroy();
    }
}
