package com.lemostic.work.modules.deployment;

import cn.hutool.core.util.ObjectUtil;
import com.dlsc.workbenchfx.model.WorkbenchModule;
import com.dlsc.workbenchfx.view.controls.ToolbarItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.lemostic.work.modules.deployment.model.ServerConfiguration;
import com.lemostic.work.modules.deployment.service.DeploymentService;
import com.lemostic.work.modules.deployment.view.PackageDeploymentView;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Tooltip;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign.MaterialDesign;

import java.util.List;

/**
 * 包部署模块
 */
@Component
public class PackageDeploymentModule extends WorkbenchModule {
    
    /**
     * 包部署视图
     */
    private PackageDeploymentView deploymentView;
    
    /**
     * 部署服务
     */
    @Autowired
    private DeploymentService deploymentService;
    
    public PackageDeploymentModule() {
        super("包部署", MaterialDesign.MDI_CLOUD_UPLOAD);
        
        // 添加工具栏按钮
        ToolbarItem refreshButton = new ToolbarItem(
            new FontIcon(MaterialDesign.MDI_REFRESH),
            event -> {
                if (deploymentView != null) {
                    deploymentView.refreshServerConfigurations();
                }
            }
        );
        refreshButton.setTooltip(new Tooltip("刷新配置"));

        ToolbarItem stopAllButton = new ToolbarItem(
            new FontIcon(MaterialDesign.MDI_STOP),
            event -> {
                if (deploymentService != null) {
                    getWorkbench().showConfirmationDialog(
                        "停止所有部署",
                        "确定要停止所有正在运行的部署任务吗？",
                        buttonType -> {
                            if (ButtonType.YES.equals(buttonType)) {
                                deploymentService.stopAllDeployments();
                                if (deploymentView != null) {
                                    deploymentView.updateStatus("所有部署任务已停止");
                                }
                            }
                        }
                    );
                }
            }
        );
        stopAllButton.setTooltip(new Tooltip("停止所有部署"));

        ToolbarItem cleanupButton = new ToolbarItem(
            new FontIcon(MaterialDesign.MDI_DELETE_SWEEP),
            event -> {
                if (deploymentService != null) {
                    getWorkbench().showConfirmationDialog(
                        "清理历史记录",
                        "确定要清理30天前的部署历史记录吗？",
                        buttonType -> {
                            if (ButtonType.YES.equals(buttonType)) {
                                deploymentService.cleanupDeploymentHistory(30);
                                if (deploymentView != null) {
                                    deploymentView.refreshDeploymentHistory();
                                    deploymentView.updateStatus("历史记录清理完成");
                                }
                            }
                        }
                    );
                }
            }
        );
        cleanupButton.setTooltip(new Tooltip("清理历史记录"));
        
        getToolbarControlsLeft().addAll(refreshButton, stopAllButton, cleanupButton);
    }
    
    @Override
    public Node activate() {
        // deploymentService现在通过@Autowired自动注入，不需要手动获取

        if (ObjectUtil.isNull(deploymentView)) {
            deploymentView = new PackageDeploymentView(deploymentService);
        }

        return deploymentView;
    }


    
    @Override
    public boolean destroy() {
        if (deploymentService != null) {
            deploymentService.stopAllDeployments();
        }
        return true;
    }
}
