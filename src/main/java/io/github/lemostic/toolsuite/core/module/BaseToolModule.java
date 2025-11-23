package io.github.lemostic.toolsuite.core.module;

import com.dlsc.workbenchfx.model.WorkbenchModule;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import org.kordamp.ikonli.Ikon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 工具模块基类
 * 提供通用功能和便捷方法，简化模块开发
 */
public abstract class BaseToolModule extends WorkbenchModule {
    
    protected final Logger logger;
    
    /**
     * 构造函数
     * @param name 模块名称
     * @param icon 模块图标
     */
    public BaseToolModule(String name, Ikon icon) {
        super(name, icon);
        this.logger = LoggerFactory.getLogger(this.getClass());
    }
    
    /**
     * 激活模块时调用
     * 子类可以覆盖此方法来初始化界面
     */
    @Override
    public Node activate() {
        logger.debug("激活模块: {}", getName());
        return createView();
    }
    
    /**
     * 创建模块视图
     * 子类必须实现此方法来提供具体的UI界面
     */
    protected abstract Node createView();
    
    /**
     * 模块销毁时调用
     * 子类可以覆盖此方法来清理资源
     */
    @Override
    public boolean destroy() {
        logger.debug("销毁模块: {}", getName());
        onDestroy();
        return super.destroy();
    }
    
    /**
     * 模块销毁时的清理逻辑
     * 子类可以覆盖此方法来实现自定义清理逻辑
     */
    protected void onDestroy() {
        // 默认不做任何操作，子类可以覆盖
    }
    
    /**
     * 创建一个简单的占位视图
     * 用于快速开发时的占位
     */
    protected Node createPlaceholder(String message) {
        StackPane pane = new StackPane();
        Label label = new Label(message);
        label.setStyle("-fx-font-size: 16px; -fx-text-fill: #666;");
        pane.getChildren().add(label);
        return pane;
    }
    
    /**
     * 显示错误信息
     */
    protected void showError(String title, String message) {
        logger.error("{}: {}", title, message);
        getWorkbench().showErrorDialog(title, message, null);
    }
    
    /**
     * 显示信息提示
     */
    protected void showInfo(String title, String message) {
        logger.info("{}: {}", title, message);
        getWorkbench().showInformationDialog(title, message, null);
    }
    
    /**
     * 显示确认对话框
     */
    protected void showConfirmation(String title, String message, java.util.function.Consumer<javafx.scene.control.ButtonType> callback) {
        getWorkbench().showConfirmationDialog(title, message, callback);
    }
}
