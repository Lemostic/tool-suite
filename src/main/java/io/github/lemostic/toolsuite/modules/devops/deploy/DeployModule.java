package io.github.lemostic.toolsuite.modules.devops.deploy;

import io.github.lemostic.toolsuite.core.module.BaseToolModule;
import io.github.lemostic.toolsuite.core.module.ModuleCategory;
import io.github.lemostic.toolsuite.core.module.ToolModule;
import io.github.lemostic.toolsuite.modules.devops.deploy.view.OptimizedDeployView;
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
        // 创建优化的部署视图
        return new OptimizedDeployView();
    }

    @Override
    protected void onDestroy() {
        logger.debug("部署模块已销毁");
    }

    // 新增方法：处理自定义部署步骤
    private void handleCustomDeploymentSteps() {
        // 从配置文件或其他方式获取自定义部署步骤
        // 并根据配置创建 TaskStep 实例
        // 示例：
        // List<Map<String, Object>> stepConfigs = getStepConfigs();
        // for (Map<String, Object> config : stepConfigs) {
        //     TaskStep step = TaskStep.createFromConfig(config);
        //     // 执行步骤
        //     step.execute(context, logConsumer);
        // }
    }
}
