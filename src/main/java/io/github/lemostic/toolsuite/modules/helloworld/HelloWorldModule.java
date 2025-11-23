package io.github.lemostic.toolsuite.modules.helloworld;

import com.dlsc.workbenchfx.model.WorkbenchModule;
import io.github.lemostic.toolsuite.core.module.ModuleCategory;
import io.github.lemostic.toolsuite.core.module.ToolModule;
import javafx.scene.Node;
import org.kordamp.ikonli.materialdesign.MaterialDesign;

@ToolModule(
    name = "Hello World",
    category = ModuleCategory.DEV_TOOLS,
    description = "示例模块，展示基本功能",
    version = "1.0.0",
    author = "lemostic",
    requiresPreferences = false,
    priority = 999  // 示例模块，低优先级
)
public class HelloWorldModule extends WorkbenchModule {

  public HelloWorldModule() {
    super("Hello World", MaterialDesign.MDI_HUMAN_HANDSUP);
  }

  @Override
  public Node activate() {
    return new HelloWorldView();
  }

}
