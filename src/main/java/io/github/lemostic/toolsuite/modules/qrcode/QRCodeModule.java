package io.github.lemostic.toolsuite.modules.qrcode;

import com.dlsc.workbenchfx.model.WorkbenchModule;
import io.github.lemostic.toolsuite.core.module.ModuleCategory;
import io.github.lemostic.toolsuite.core.module.ToolModule;
import javafx.scene.Node;
import org.kordamp.ikonli.materialdesign.MaterialDesign;

@ToolModule(
    name = "二维码生成",
    category = ModuleCategory.DEV_TOOLS,
    menuGroup = "开发工具",
    description = "将文本或URL生成二维码图片",
    version = "1.0.0",
    author = "Tool Suite",
    priority = 90
)
public class QRCodeModule extends WorkbenchModule {

    public QRCodeModule() {
        super("二维码生成", MaterialDesign.MDI_QRCODE);
    }

    @Override
    public Node activate() {
        return new QRCodeView();
    }
}
