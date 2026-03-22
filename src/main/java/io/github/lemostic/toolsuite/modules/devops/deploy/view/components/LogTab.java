package io.github.lemostic.toolsuite.modules.devops.deploy.view.components;

import io.github.lemostic.toolsuite.modules.devops.deploy.model.ServerConfigDTO;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign.MaterialDesign;

public class LogTab extends Tab {
    
    private final ServerConfigDTO server;
    private TextArea logArea;
    private ProgressBar progressBar;
    private Label statusLabel;
    private Button cancelButton;
    private Label serverLabel;
    
    private volatile boolean autoScroll = true;
    
    public LogTab(ServerConfigDTO server) {
        super(server.getName());
        this.server = server;
        initializeUI();
    }
    
    private void initializeUI() {
        // 设置Tab标题和图标
        FontIcon icon = new FontIcon(MaterialDesign.MDI_SERVER_NETWORK);
        icon.setIconSize(16);
        setGraphic(icon);
        
        // 主容器
        VBox container = new VBox(5);
        container.setPadding(new Insets(10));
        
        // 顶部信息栏
        HBox infoBar = new HBox(15);
        infoBar.setAlignment(Pos.CENTER_LEFT);
        infoBar.setPadding(new Insets(0, 0, 5, 0));
        
        serverLabel = new Label(server.getHost() + ":" + server.getPort());
        serverLabel.setStyle("-fx-font-weight: bold;");
        
        statusLabel = new Label("等待开始...");
        statusLabel.setStyle("-fx-text-fill: #666;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        cancelButton = new Button("取消", new FontIcon(MaterialDesign.MDI_CLOSE));
        cancelButton.setDisable(true);
        
        infoBar.getChildren().addAll(serverLabel, new Label("|"), statusLabel, spacer, cancelButton);
        
        // 进度条
        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(Double.MAX_VALUE);
        
        // 日志区域
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setWrapText(true);
        logArea.setStyle("-fx-font-family: 'Consolas', 'Monaco', monospace; -fx-font-size: 12px;");
        logArea.setPromptText("部署日志将显示在这里...");
        
        // 工具栏
        ToolBar toolbar = new ToolBar();
        Button clearBtn = new Button("清空", new FontIcon(MaterialDesign.MDI_DELETE));
        clearBtn.setOnAction(e -> clearLog());
        
        Button copyBtn = new Button("复制", new FontIcon(MaterialDesign.MDI_CONTENT_COPY));
        copyBtn.setOnAction(e -> copyLog());
        
        CheckBox autoScrollCheck = new CheckBox("自动滚动");
        autoScrollCheck.setSelected(true);
        autoScrollCheck.selectedProperty().addListener((obs, oldVal, newVal) -> autoScroll = newVal);
        
        toolbar.getItems().addAll(clearBtn, copyBtn, new Separator(), autoScrollCheck);
        
        VBox.setVgrow(logArea, Priority.ALWAYS);
        container.getChildren().addAll(infoBar, progressBar, logArea, toolbar);
        
        setContent(container);
    }
    
    public void appendLog(String message) {
        Platform.runLater(() -> {
            logArea.appendText(message + "\n");
            if (autoScroll) {
                logArea.setScrollTop(Double.MAX_VALUE);
            }
        });
    }
    
    public void updateStatus(String status) {
        Platform.runLater(() -> statusLabel.setText(status));
    }
    
    public void updateProgress(double progress) {
        Platform.runLater(() -> progressBar.setProgress(progress));
    }
    
    public void setRunning(boolean running) {
        Platform.runLater(() -> {
            cancelButton.setDisable(!running);
            if (running) {
                statusLabel.setStyle("-fx-text-fill: #2196F3; -fx-font-weight: bold;");
            }
        });
    }
    
    public void setCompleted(boolean success) {
        Platform.runLater(() -> {
            cancelButton.setDisable(true);
            progressBar.setProgress(1.0);
            if (success) {
                statusLabel.setText("完成");
                statusLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
                FontIcon icon = new FontIcon(MaterialDesign.MDI_CHECK_CIRCLE);
                icon.setIconSize(16);
                icon.setStyle("-fx-icon-color: #4CAF50;");
                setGraphic(icon);
            } else {
                statusLabel.setText("失败");
                statusLabel.setStyle("-fx-text-fill: #f44336; -fx-font-weight: bold;");
                FontIcon icon = new FontIcon(MaterialDesign.MDI_CLOSE_CIRCLE);
                icon.setIconSize(16);
                icon.setStyle("-fx-icon-color: #f44336;");
                setGraphic(icon);
            }
        });
    }
    
    public void clearLog() {
        Platform.runLater(() -> logArea.clear());
    }
    
    public void copyLog() {
        Platform.runLater(() -> {
            javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
            javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
            content.putString(logArea.getText());
            clipboard.setContent(content);
        });
    }
    
    public void setOnCancel(Runnable action) {
        cancelButton.setOnAction(e -> {
            if (action != null) {
                action.run();
            }
        });
    }
    
    public ServerConfigDTO getServer() {
        return server;
    }
}
