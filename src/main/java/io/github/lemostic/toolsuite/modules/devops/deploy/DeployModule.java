package io.github.lemostic.toolsuite.modules.devops.deploy;

import io.github.lemostic.toolsuite.core.module.BaseToolModule;
import io.github.lemostic.toolsuite.core.module.ModuleCategory;
import io.github.lemostic.toolsuite.core.module.ToolModule;
import io.github.lemostic.toolsuite.modules.devops.deploy.view.DeployView;
import javafx.scene.Node;
import org.kordamp.ikonli.materialdesign.MaterialDesign;

@ToolModule(
    name = "应用部署工具",
    category = ModuleCategory.DEVOPS,
    menuGroup = "运维工具",
    menuGroupOrder = 20,
    description = "快速部署应用程序到远程服务器，支持多服务器并行部署",
    version = "1.0.0",
    author = "lemostic",
    priority = 40
)
public class DeployModule extends BaseToolModule {
    
    public DeployModule() {
        super("应用部署工具", MaterialDesign.MDI_CLOUD_UPLOAD);
    }
    
    @Override
    protected Node createView() {
        return new DeployView();
    }
    
    @Override
    protected void onDestroy() {
        logger.debug("部署模块已销毁");
    }
}
