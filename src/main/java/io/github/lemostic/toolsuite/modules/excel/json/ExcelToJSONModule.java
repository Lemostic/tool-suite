package io.github.lemostic.toolsuite.modules.excel.json;

import com.dlsc.workbenchfx.model.WorkbenchModule;
import io.github.lemostic.toolsuite.core.module.ModuleCategory;
import io.github.lemostic.toolsuite.core.module.ToolModule;
import javafx.scene.Node;
import org.kordamp.ikonli.materialdesign.MaterialDesign;

@ToolModule(
    name = "Excel转JSON",
    category = ModuleCategory.DATA_PROCESS,
    description = "将Excel数据转换为JSON数组格式，支持批量处理",
    version = "2.0.0",
    author = "Tool Suite",
    requiresPreferences = false,
    priority = 80
)
public class ExcelToJSONModule extends WorkbenchModule {

    public ExcelToJSONModule() {
        super("Excel转JSON", MaterialDesign.MDI_FILE_EXCEL);
    }

    @Override
    public Node activate() {
        return new ExcelToJSONView();
    }
}