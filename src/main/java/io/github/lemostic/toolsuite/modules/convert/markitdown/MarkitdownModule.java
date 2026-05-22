package io.github.lemostic.toolsuite.modules.convert.markitdown;

import io.github.lemostic.toolsuite.core.module.BaseToolModule;
import io.github.lemostic.toolsuite.core.module.ModuleCategory;
import io.github.lemostic.toolsuite.core.module.ToolModule;
import javafx.scene.Node;
import org.kordamp.ikonli.materialdesign.MaterialDesign;

@ToolModule(
    name = "文档转Markdown",
    category = ModuleCategory.DATA_PROCESS,
    menuGroup = "数据处理",
    menuGroupOrder = 20,
    description = "将 Word、PDF、Excel、PPT 等文档转换为 Markdown 格式，支持图片提取",
    version = "1.0.0",
    author = "Tool Suite",
    priority = 60
)
public class MarkitdownModule extends BaseToolModule {

    public MarkitdownModule() {
        super("文档转Markdown", MaterialDesign.MDI_FILE_DOCUMENT);
    }

    @Override
    protected Node createView() {
        return new MarkitdownView();
    }

    @Override
    protected void onDestroy() {
        logger.debug("Markitdown 模块已销毁");
    }
}
