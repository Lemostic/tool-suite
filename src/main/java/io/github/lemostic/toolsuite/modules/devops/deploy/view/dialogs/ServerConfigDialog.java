package io.github.lemostic.toolsuite.modules.devops.deploy.view.dialogs;

import io.github.lemostic.toolsuite.modules.devops.deploy.model.ServerConfig;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign.MaterialDesign;

public class ServerConfigDialog extends Dialog<ServerConfig> {
    
    private TextField nameField;
    private TextField hostField;
    private TextField portField;
    private TextField usernameField;
    private PasswordField passwordField;
    private TextField appDirField;
    private TextField backupDirField;
    private TextField binDirField;
    private TextField stopScriptField;
    private TextField startScriptField;
    private TextArea descriptionArea;
    
    private ServerConfig existingConfig;
    
    public ServerConfigDialog() {
        this(null);
    }
    
    public ServerConfigDialog(ServerConfig config) {
        this.existingConfig = config;
        initializeUI();
        if (config != null) {
            loadConfig(config);
        }
    }
    
    private void initializeUI() {
        setTitle(existingConfig == null ? "添加服务器" : "编辑服务器");
        setHeaderText(existingConfig == null ? "配置新的服务器" : "编辑服务器配置");
        
        // 设置对话框图标
        FontIcon icon = new FontIcon(MaterialDesign.MDI_SERVER);
        icon.setIconSize(48);
        setGraphic(icon);
        
        // 按钮
        ButtonType saveButtonType = new ButtonType("保存", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        // 创建表单内容
        GridPane grid = createFormGrid();
        getDialogPane().setContent(grid);
        
        // 设置结果转换器
        setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return validateAndCreateConfig();
            }
            return null;
        });
        
        // 设置对话框大小
        getDialogPane().setPrefWidth(500);
    }
    
    private GridPane createFormGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        
        // 基本信息
        nameField = new TextField();
        nameField.setPromptText("例如: 生产服务器");
        
        hostField = new TextField();
        hostField.setPromptText("例如: 192.168.1.100");
        
        portField = new TextField("22");
        portField.setPrefWidth(80);
        
        usernameField = new TextField();
        usernameField.setPromptText("用户名");
        
        passwordField = new PasswordField();
        passwordField.setPromptText("密码");
        
        // 目录配置
        appDirField = new TextField();
        appDirField.setPromptText("例如: /opt/myapp");
        
        backupDirField = new TextField();
        backupDirField.setPromptText("例如: /opt/myapp/backup");
        
        binDirField = new TextField("bin");
        binDirField.setPromptText("bin目录相对路径");
        
        // 脚本配置
        stopScriptField = new TextField("stop.sh");
        stopScriptField.setPromptText("停止脚本文件名");
        
        startScriptField = new TextField("start.sh");
        startScriptField.setPromptText("启动脚本文件名");
        
        // 描述
        descriptionArea = new TextArea();
        descriptionArea.setPromptText("服务器描述（可选）");
        descriptionArea.setPrefRowCount(3);
        descriptionArea.setWrapText(true);
        
        // 添加到网格
        int row = 0;
        grid.add(new Label("名称*"), 0, row);
        grid.add(nameField, 1, row, 2, 1);
        row++;
        
        grid.add(new Label("主机*"), 0, row);
        HBox hostBox = new HBox(10, hostField, new Label("端口"), portField);
        HBox.setHgrow(hostField, Priority.ALWAYS);
        grid.add(hostBox, 1, row, 2, 1);
        row++;
        
        grid.add(new Label("用户名*"), 0, row);
        grid.add(usernameField, 1, row);
        grid.add(new Label("密码*"), 2, row);
        grid.add(passwordField, 3, row);
        row++;
        
        grid.add(new Separator(), 0, row, 4, 1);
        row++;
        
        grid.add(new Label("程序目录*"), 0, row);
        grid.add(appDirField, 1, row, 3, 1);
        row++;
        
        grid.add(new Label("备份目录"), 0, row);
        grid.add(backupDirField, 1, row, 3, 1);
        row++;
        
        grid.add(new Label("Bin目录"), 0, row);
        grid.add(binDirField, 1, row);
        row++;
        
        grid.add(new Label("停止脚本"), 0, row);
        grid.add(stopScriptField, 1, row);
        grid.add(new Label("启动脚本"), 2, row);
        grid.add(startScriptField, 3, row);
        row++;
        
        grid.add(new Separator(), 0, row, 4, 1);
        row++;
        
        grid.add(new Label("描述"), 0, row);
        grid.add(descriptionArea, 1, row, 3, 1);
        
        // 列约束
        ColumnConstraints col0 = new ColumnConstraints();
        col0.setMinWidth(80);
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setHgrow(Priority.ALWAYS);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setMinWidth(60);
        ColumnConstraints col3 = new ColumnConstraints();
        col3.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(col0, col1, col2, col3);
        
        return grid;
    }
    
    private void loadConfig(ServerConfig config) {
        nameField.setText(config.getName());
        hostField.setText(config.getHost());
        portField.setText(String.valueOf(config.getPort()));
        usernameField.setText(config.getUsername());
        passwordField.setText(config.getPassword());
        appDirField.setText(config.getAppDirectory());
        backupDirField.setText(config.getBackupDirectory());
        binDirField.setText(config.getBinDirectory());
        stopScriptField.setText(config.getStopScript());
        startScriptField.setText(config.getStartScript());
        descriptionArea.setText(config.getDescription());
    }
    
    private ServerConfig validateAndCreateConfig() {
        // 验证必填字段
        if (isNullOrEmpty(nameField.getText())) {
            showError("名称不能为空");
            return null;
        }
        if (isNullOrEmpty(hostField.getText())) {
            showError("主机不能为空");
            return null;
        }
        if (isNullOrEmpty(usernameField.getText())) {
            showError("用户名不能为空");
            return null;
        }
        if (isNullOrEmpty(passwordField.getText())) {
            showError("密码不能为空");
            return null;
        }
        if (isNullOrEmpty(appDirField.getText())) {
            showError("程序目录不能为空");
            return null;
        }
        
        ServerConfig config = existingConfig != null ? existingConfig : new ServerConfig();
        
        config.setName(nameField.getText().trim());
        config.setHost(hostField.getText().trim());
        try {
            config.setPort(Integer.parseInt(portField.getText().trim()));
        } catch (NumberFormatException e) {
            config.setPort(22);
        }
        config.setUsername(usernameField.getText().trim());
        config.setPassword(passwordField.getText());
        config.setAppDirectory(appDirField.getText().trim());
        config.setBackupDirectory(backupDirField.getText() != null ? 
            backupDirField.getText().trim() : config.getAppDirectory() + "/backup");
        config.setBinDirectory(binDirField.getText() != null ? 
            binDirField.getText().trim() : "bin");
        config.setStopScript(stopScriptField.getText() != null ? 
            stopScriptField.getText().trim() : "stop.sh");
        config.setStartScript(startScriptField.getText() != null ? 
            startScriptField.getText().trim() : "start.sh");
        config.setDescription(descriptionArea.getText());
        
        return config;
    }
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("验证错误");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
}
