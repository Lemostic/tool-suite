package io.github.lemostic.toolsuite.modules.devops.jenkinsdownloader;

import io.github.lemostic.toolsuite.core.module.BaseToolModule;
import io.github.lemostic.toolsuite.core.module.ModuleCategory;
import io.github.lemostic.toolsuite.core.module.ToolModule;
import javafx.scene.Node;
import org.kordamp.ikonli.materialdesign.MaterialDesign;

/**
 * Jenkins文件下载模块
 * 用于从Jenkins构建工作区中选择性下载文件
 * 
 * 功能特性：
 * - 支持保存多个Jenkins服务器配置
 * - 支持Basic Auth认证
 * - 多种文件匹配方式（前缀、后缀、包含、正则、精确）
 * - 文件列表预览和选择性下载
 * - 批量下载进度显示
 */
@ToolModule(
    name = "Jenkins文件下载",
    category = ModuleCategory.DEVOPS,
    menuGroup = "运维工具",
    menuGroupOrder = 10,
    description = "从Jenkins构建工作区中选择性下载文件，支持多种匹配方式和认证配置",
    version = "1.0.0",
    author = "lemostic",
    requiresPreferences = false,
    priority = 40
)
public class JenkinsDownloaderModule extends BaseToolModule {
    
    public JenkinsDownloaderModule() {
        super("Jenkins文件下载", MaterialDesign.MDI_CLOUD_DOWNLOAD);
    }
    
    @Override
    protected Node createView() {
        return new JenkinsDownloaderView();
    }
    
    @Override
    protected void onDestroy() {
        logger.debug("Jenkins文件下载模块已销毁");
        // 清理资源
    }
}
