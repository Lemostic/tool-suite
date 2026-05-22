package io.github.lemostic.toolsuite.modules.excel.transpose;

import com.dlsc.workbenchfx.model.WorkbenchModule;
import io.github.lemostic.toolsuite.core.module.ModuleCategory;
import io.github.lemostic.toolsuite.core.module.ToolModule;
import javafx.scene.Node;
import org.kordamp.ikonli.materialdesign.MaterialDesign;

@ToolModule(
    name = "Excel行列转置",
    category = ModuleCategory.DATA_PROCESS,
    menuGroup = "数据处理",
    menuGroupOrder = 30,
    description = "Excel数据行列转置、预览、列拼接复制工具",
    version = "1.0.0",
    author = "Tool Suite",
    requiresPreferences = false,
    priority = 75
)
public class ExcelTransposeModule extends WorkbenchModule {

    public ExcelTransposeModule() {
        super("Excel行列转置", MaterialDesign.MDI_SWAP_HORIZONTAL);
    }

    @Override
    public Node activate() {
        return new ExcelTransposeView();
    }
}
