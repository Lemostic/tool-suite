package io.github.lemostic.toolsuite.modules.file.zipclean;

import io.github.lemostic.toolsuite.core.module.BaseToolModule;
import io.github.lemostic.toolsuite.core.module.ModuleCategory;
import io.github.lemostic.toolsuite.core.module.ToolModule;
import javafx.scene.Node;
import org.kordamp.ikonli.materialdesign.MaterialDesign;

/**
 * 压缩包清理模块
 * 用于清理压缩包中不需要的文件，支持正则表达式匹配
 */
@ToolModule(
    name = "压缩包清理工具",
    category = ModuleCategory.FILE_TOOLS,
    menuGroup = "文件工具",
    menuGroupOrder = 20,
    description = "智能清理压缩包中的无用文件，支持正则表达式过滤",
    version = "1.0.0",
    author = "lemostic",
    requiresPreferences = false,
    priority = 50
)
public class ZipCleanModule extends BaseToolModule {
    
    public ZipCleanModule() {
        super("压缩包清理工具", MaterialDesign.MDI_ZIP_BOX);
    }
    
    @Override
    protected Node createView() {
        return new ZipCleanView();
    }
    
    @Override
    protected void onDestroy() {
        logger.debug("压缩包清理模块已销毁");
    }
}
