package io.github.lemostic.toolsuite.modules.convert.xmljson;

import com.dlsc.workbenchfx.model.WorkbenchModule;
import io.github.lemostic.toolsuite.core.module.ModuleCategory;
import io.github.lemostic.toolsuite.core.module.ToolModule;
import javafx.scene.Node;
import org.kordamp.ikonli.materialdesign.MaterialDesign;

@ToolModule(
    name = "XML/JSON互转",
    category = ModuleCategory.DATA_PROCESS,
    menuGroup = "数据处理",
    menuGroupOrder = 30,
    description = "XML与JSON数据格式互相转换，支持格式化输出和一键复制",
    version = "1.0.0",
    author = "Tool Suite",
    requiresPreferences = false,
    priority = 75
)
public class XmlJsonConverterModule extends WorkbenchModule {

    public XmlJsonConverterModule() {
        super("XML/JSON互转", MaterialDesign.MDI_SWAP_HORIZONTAL);
    }

    @Override
    public Node activate() {
        return new XmlJsonConverterView();
    }

}
