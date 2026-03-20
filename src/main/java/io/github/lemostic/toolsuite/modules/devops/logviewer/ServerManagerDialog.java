package io.github.lemostic.toolsuite.modules.devops.logviewer;

import io.github.lemostic.toolsuite.modules.devops.logviewer.model.LogDirectory;
import io.github.lemostic.toolsuite.modules.devops.logviewer.model.ServerConfig;
import io.github.lemostic.toolsuite.modules.devops.logviewer.service.LogViewerService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign.MaterialDesign;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 服务器管理对话框
 */
public class ServerManagerDialog extends Dialog<ServerConfig> {
    
    private TextField nameField;
    private TextField hostField;
    private TextField portField;
    private TextField usernameField;
    private PasswordField passwordField;
    private TextField privateKeyPathField;
    private PasswordField privateKeyPassphraseField;
    private RadioButton passwordAuthRadio;
    private RadioButton keyAuthRadio;
    private TextArea remarkArea;
    private ListView<LogDirectory> directoryListView;
    private List<LogDirectory> directories = new ArrayList<>();
    
    private ServerConfig editingServer;
    private boolean connectionTested = false;
    private Button testConnectionBtn;
    private Label testStatusLabel;
    private Button saveButton;
    
    public ServerManagerDialog() {
        this(null);
    }
    
    public ServerManagerDialog(ServerConfig serverConfig) {
        this.editingServer = serverConfig;
        
        setTitle(serverConfig == null ? "添加服务器" : "编辑服务器");
        setHeaderText(serverConfig == null ? "配置新的日志服务器" : "编辑服务器配置");
        
        // 设置对话框图标
        Stage stage = (Stage) getDialogPane().getScene().getWindow();
        
        initModality(Modality.APPLICATION_MODAL);
        
        // 创建内容
        getDialogPane().setContent(createContent());
        getDialogPane().setPrefWidth(600);
        getDialogPane().setPrefHeight(700);
        getDialogPane().setMinWidth(600);
        getDialogPane().setMinHeight(900);
        
        // 添加按钮
        ButtonType saveButtonType = new ButtonType("保存", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(saveButtonType, cancelButtonType);
        
        // 获取按钮引用
        saveButton = (Button) getDialogPane().lookupButton(saveButtonType);
        
        // 初始化保存按钮为禁用状态（如果是编辑模式则启用）
        saveButton.setDisable(serverConfig == null);
        
        // 创建测试连接按钮（使用普通按钮而不是ButtonType，避免触发对话框关闭）
        testConnectionBtn = new Button("测试连接", new FontIcon(MaterialDesign.MDI_LAN_CONNECT));
        testConnectionBtn.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white;");
        testConnectionBtn.setOnAction(e -> testConnection());
        
        // 将测试连接按钮添加到按钮栏，放在保存按钮左边（紧挨着）
        ButtonBar buttonBar = (ButtonBar) getDialogPane().lookup(".button-bar");
        if (buttonBar != null) {
            // 找到保存按钮的位置
            int saveButtonIndex = buttonBar.getButtons().indexOf(saveButton);
            if (saveButtonIndex >= 0) {
                // 在保存按钮之前插入测试连接按钮
                buttonBar.getButtons().add(saveButtonIndex, testConnectionBtn);
            } else {
                // 如果找不到保存按钮，添加到最前面
                buttonBar.getButtons().add(0, testConnectionBtn);
            }
            ButtonBar.setButtonData(testConnectionBtn, ButtonBar.ButtonData.LEFT);
        }
        
        // 设置结果转换器
        setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return createServerConfig();
            }
            return null;
        });
        
        // 加载现有数据
        if (serverConfig != null) {
            loadServerData(serverConfig);
            connectionTested = true; // 编辑模式下视为已测试
        }
    }
    
    private Node createContent() {
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        
        // 基本信息区域
        content.getChildren().add(createBasicInfoSection());
        
        // 认证方式区域
        content.getChildren().add(createAuthSection());
        
        // 日志目录区域
        content.getChildren().add(createDirectorySection());
        
        // 备注区域
        content.getChildren().add(createRemarkSection());
        
        return content;
    }
    
    private Node createBasicInfoSection() {
        VBox section = new VBox(10);
        section.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-radius: 5; -fx-background-radius: 5; -fx-padding: 15;");
        
        Label titleLabel = new Label("基本信息");
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #333;");
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        
        // 服务器名称
        grid.add(new Label("服务器名称:"), 0, 0);
        nameField = new TextField();
        nameField.setPromptText("例如: 生产服务器");
        grid.add(nameField, 1, 0);
        
        // 主机地址
        grid.add(new Label("主机地址:"), 0, 1);
        hostField = new TextField();
        hostField.setPromptText("例如: 192.168.1.100");
        grid.add(hostField, 1, 1);
        
        // 端口
        grid.add(new Label("SSH端口:"), 0, 2);
        portField = new TextField("22");
        portField.setPrefWidth(80);
        grid.add(portField, 1, 2);
        
        // 用户名
        grid.add(new Label("用户名:"), 0, 3);
        usernameField = new TextField();
        usernameField.setPromptText("例如: root");
        grid.add(usernameField, 1, 3);
        
        GridPane.setHgrow(nameField, Priority.ALWAYS);
        GridPane.setHgrow(hostField, Priority.ALWAYS);
        GridPane.setHgrow(usernameField, Priority.ALWAYS);
        
        section.getChildren().addAll(titleLabel, new Separator(), grid);
        return section;
    }
    
    private Node createAuthSection() {
        VBox section = new VBox(10);
        section.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-radius: 5; -fx-background-radius: 5; -fx-padding: 15;");
        
        Label titleLabel = new Label("认证方式");
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #333;");
        
        ToggleGroup authGroup = new ToggleGroup();
        
        passwordAuthRadio = new RadioButton("密码认证");
        passwordAuthRadio.setToggleGroup(authGroup);
        passwordAuthRadio.setSelected(true);
        
        keyAuthRadio = new RadioButton("私钥认证");
        keyAuthRadio.setToggleGroup(authGroup);
        
        HBox authTypeBox = new HBox(20);
        authTypeBox.getChildren().addAll(passwordAuthRadio, keyAuthRadio);
        
        // 使用 GridPane 保持与基本信息区域一致的布局
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        
        // 密码输入
        grid.add(new Label("密码:"), 0, 0);
        passwordField = new PasswordField();
        passwordField.setPromptText("输入SSH密码");
        grid.add(passwordField, 1, 0);
        
        // 私钥路径
        grid.add(new Label("私钥路径:"), 0, 1);
        HBox keyPathBox = new HBox(5);
        privateKeyPathField = new TextField();
        privateKeyPathField.setPromptText("私钥文件路径");
        privateKeyPathField.setDisable(true);
        Button browseBtn = new Button("浏览...");
        browseBtn.setDisable(true);
        browseBtn.setOnAction(e -> browsePrivateKey());
        keyPathBox.getChildren().addAll(privateKeyPathField, browseBtn);
        HBox.setHgrow(privateKeyPathField, Priority.ALWAYS);
        grid.add(keyPathBox, 1, 1);
        
        // 私钥密码
        grid.add(new Label("私钥密码:"), 0, 2);
        privateKeyPassphraseField = new PasswordField();
        privateKeyPassphraseField.setPromptText("私钥密码（如果有）");
        privateKeyPassphraseField.setDisable(true);
        grid.add(privateKeyPassphraseField, 1, 2);
        
        GridPane.setHgrow(passwordField, Priority.ALWAYS);
        GridPane.setHgrow(keyPathBox, Priority.ALWAYS);
        GridPane.setHgrow(privateKeyPassphraseField, Priority.ALWAYS);
        
        // 认证方式切换
        passwordAuthRadio.selectedProperty().addListener((obs, oldVal, newVal) -> {
            passwordField.setDisable(!newVal);
            privateKeyPathField.setDisable(newVal);
            privateKeyPassphraseField.setDisable(newVal);
            browseBtn.setDisable(newVal);
        });
        
        section.getChildren().addAll(titleLabel, new Separator(), authTypeBox, grid);
        return section;
    }
    
    private Node createDirectorySection() {
        VBox section = new VBox(10);
        section.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-radius: 5; -fx-background-radius: 5; -fx-padding: 15;");
        VBox.setVgrow(section, Priority.ALWAYS);
        
        Label titleLabel = new Label("日志目录配置");
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #333;");
        
        // 目录列表
        directoryListView = new ListView<>();
        directoryListView.setPrefHeight(100);
        directoryListView.setMinHeight(100);
        directoryListView.setCellFactory(lv -> new ListCell<LogDirectory>() {
            @Override
            protected void updateItem(LogDirectory item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getDisplayName() + " [" + item.getFilePattern() + "]");
                }
            }
        });
        VBox.setVgrow(directoryListView, Priority.ALWAYS);
        
        // 按钮区域
        HBox buttonBox = new HBox(5);
        buttonBox.setAlignment(Pos.CENTER_LEFT);
        
        Button addBtn = new Button("添加", new FontIcon(MaterialDesign.MDI_PLUS));
        addBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        addBtn.setOnAction(e -> addDirectory());
        
        Button editBtn = new Button("编辑", new FontIcon(MaterialDesign.MDI_PENCIL));
        editBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
        editBtn.setOnAction(e -> editDirectory());
        
        Button deleteBtn = new Button("删除", new FontIcon(MaterialDesign.MDI_DELETE));
        deleteBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
        deleteBtn.setOnAction(e -> deleteDirectory());
        
        buttonBox.getChildren().addAll(addBtn, editBtn, deleteBtn);
        
        section.getChildren().addAll(titleLabel, new Separator(), directoryListView, buttonBox);
        return section;
    }
    
    private Node createRemarkSection() {
        VBox section = new VBox(10);
        section.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-radius: 5; -fx-background-radius: 5; -fx-padding: 15;");
        
        Label titleLabel = new Label("备注");
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #333;");
        
        remarkArea = new TextArea();
        remarkArea.setPrefRowCount(3);
        remarkArea.setPromptText("添加备注信息...");
        
        section.getChildren().addAll(titleLabel, new Separator(), remarkArea);
        return section;
    }
    
    private void browsePrivateKey() {
        // 简化实现，实际应该打开文件选择器
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("私钥路径");
        dialog.setHeaderText("输入私钥文件的完整路径");
        dialog.setContentText("路径:");
        dialog.showAndWait().ifPresent(path -> privateKeyPathField.setText(path));
    }
    
    private void addDirectory() {
        DirectoryConfigDialog dialog = new DirectoryConfigDialog();
        dialog.showAndWait().ifPresent(dir -> {
            directories.add(dir);
            refreshDirectoryList();
        });
    }
    
    private void editDirectory() {
        LogDirectory selected = directoryListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("提示", "请先选择一个目录", Alert.AlertType.WARNING);
            return;
        }
        
        DirectoryConfigDialog dialog = new DirectoryConfigDialog(selected);
        dialog.showAndWait().ifPresent(updated -> {
            int index = directories.indexOf(selected);
            directories.set(index, updated);
            refreshDirectoryList();
        });
    }
    
    private void deleteDirectory() {
        LogDirectory selected = directoryListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("提示", "请先选择一个目录", Alert.AlertType.WARNING);
            return;
        }
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("确认删除");
        confirm.setHeaderText("删除目录配置");
        confirm.setContentText("确定要删除目录 \"" + selected.getDisplayName() + "\" 吗？");
        
        confirm.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                directories.remove(selected);
                refreshDirectoryList();
            }
        });
    }
    
    private void refreshDirectoryList() {
        directoryListView.getItems().clear();
        directoryListView.getItems().addAll(directories);
    }
    
    private void loadServerData(ServerConfig server) {
        nameField.setText(server.getName());
        hostField.setText(server.getHost());
        portField.setText(String.valueOf(server.getPort()));
        usernameField.setText(server.getUsername());
        passwordField.setText(server.getPassword());
        privateKeyPathField.setText(server.getPrivateKeyPath());
        privateKeyPassphraseField.setText(server.getPrivateKeyPassphrase());
        remarkArea.setText(server.getRemark());
        
        passwordAuthRadio.setSelected(server.isUsePasswordAuth());
        keyAuthRadio.setSelected(!server.isUsePasswordAuth());
        
        if (server.getLogDirectories() != null) {
            directories.addAll(server.getLogDirectories());
            refreshDirectoryList();
        }
    }
    
    private ServerConfig createServerConfig() {
        String id = editingServer != null ? editingServer.getId() : UUID.randomUUID().toString();
        
        return ServerConfig.builder()
            .id(id)
            .name(nameField.getText().trim())
            .host(hostField.getText().trim())
            .port(parsePort(portField.getText()))
            .username(usernameField.getText().trim())
            .password(passwordField.getText())
            .privateKeyPath(privateKeyPathField.getText().trim())
            .privateKeyPassphrase(privateKeyPassphraseField.getText())
            .usePasswordAuth(passwordAuthRadio.isSelected())
            .logDirectories(new ArrayList<>(directories))
            .remark(remarkArea.getText())
            .build();
    }
    
    private int parsePort(String portText) {
        try {
            return Integer.parseInt(portText.trim());
        } catch (NumberFormatException e) {
            return 22;
        }
    }
    
    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
    
    /**
     * 测试连接
     */
    private void testConnection() {
        // 验证必填字段
        if (nameField.getText().trim().isEmpty()) {
            showAlert("验证失败", "请输入服务器名称", Alert.AlertType.WARNING);
            return;
        }
        if (hostField.getText().trim().isEmpty()) {
            showAlert("验证失败", "请输入主机地址", Alert.AlertType.WARNING);
            return;
        }
        if (usernameField.getText().trim().isEmpty()) {
            showAlert("验证失败", "请输入用户名", Alert.AlertType.WARNING);
            return;
        }
        
        // 检查认证信息
        if (passwordAuthRadio.isSelected() && passwordField.getText().isEmpty()) {
            showAlert("验证失败", "请输入密码", Alert.AlertType.WARNING);
            return;
        }
        if (!passwordAuthRadio.isSelected() && privateKeyPathField.getText().trim().isEmpty()) {
            showAlert("验证失败", "请输入私钥路径", Alert.AlertType.WARNING);
            return;
        }
        
        // 创建临时配置进行测试
        ServerConfig testConfig = ServerConfig.builder()
            .id(UUID.randomUUID().toString())
            .name(nameField.getText().trim())
            .host(hostField.getText().trim())
            .port(parsePort(portField.getText()))
            .username(usernameField.getText().trim())
            .password(passwordField.getText())
            .privateKeyPath(privateKeyPathField.getText().trim())
            .privateKeyPassphrase(privateKeyPassphraseField.getText())
            .usePasswordAuth(passwordAuthRadio.isSelected())
            .logDirectories(new ArrayList<>())
            .build();
        
        // 禁用测试按钮
        testConnectionBtn.setDisable(true);
        testConnectionBtn.setText("测试中...");
        
        // 执行连接测试
        LogViewerService testService = new LogViewerService();
        testService.connectAsync(testConfig)
            .thenAccept(success -> Platform.runLater(() -> {
                if (success) {
                    connectionTested = true;
                    saveButton.setDisable(false);
                    testConnectionBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
                    testConnectionBtn.setText("测试通过");
                    testConnectionBtn.setGraphic(new FontIcon(MaterialDesign.MDI_CHECK));
                    showAlert("连接成功", "服务器连接测试通过！", Alert.AlertType.INFORMATION);
                }
                testService.disconnect();
            }))
            .exceptionally(throwable -> {
                Platform.runLater(() -> {
                    connectionTested = false;
                    saveButton.setDisable(true);
                    testConnectionBtn.setDisable(false);
                    testConnectionBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
                    testConnectionBtn.setText("测试失败");
                    testConnectionBtn.setGraphic(new FontIcon(MaterialDesign.MDI_CLOSE));
                    showAlert("连接失败", "无法连接到服务器：" + throwable.getMessage(), Alert.AlertType.ERROR);
                });
                return null;
            });
    }
    
    /**
     * 目录配置对话框
     */
    private static class DirectoryConfigDialog extends Dialog<LogDirectory> {
        
        private TextField nameField;
        private TextField pathField;
        private TextField patternField;
        
        public DirectoryConfigDialog() {
            this(null);
        }
        
        public DirectoryConfigDialog(LogDirectory directory) {
            setTitle(directory == null ? "添加日志目录" : "编辑日志目录");
            
            VBox content = new VBox(10);
            content.setPadding(new Insets(20));
            
            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            
            grid.add(new Label("目录名称:"), 0, 0);
            nameField = new TextField();
            nameField.setPromptText("例如: 应用日志");
            grid.add(nameField, 1, 0);
            
            grid.add(new Label("目录路径:"), 0, 1);
            pathField = new TextField();
            pathField.setPromptText("例如: /var/log/app");
            grid.add(pathField, 1, 1);
            
            grid.add(new Label("文件过滤:"), 0, 2);
            patternField = new TextField("*.log");
            patternField.setPromptText("例如: *.log, *.log.*, *.out (多个用逗号分隔)");
            grid.add(patternField, 1, 2);
            
            GridPane.setHgrow(nameField, Priority.ALWAYS);
            GridPane.setHgrow(pathField, Priority.ALWAYS);
            GridPane.setHgrow(patternField, Priority.ALWAYS);
            
            content.getChildren().add(grid);
            
            getDialogPane().setContent(content);
            getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            
            if (directory != null) {
                nameField.setText(directory.getName());
                pathField.setText(directory.getPath());
                patternField.setText(directory.getFilePattern());
            }
            
            setResultConverter(button -> {
                if (button == ButtonType.OK) {
                    return LogDirectory.builder()
                        .id(directory != null ? directory.getId() : UUID.randomUUID().toString())
                        .name(nameField.getText().trim())
                        .path(pathField.getText().trim())
                        .filePattern(patternField.getText().trim())
                        .build();
                }
                return null;
            });
        }
    }
}
