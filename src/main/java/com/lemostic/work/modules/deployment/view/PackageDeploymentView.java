package com.lemostic.work.modules.deployment.view;

import cn.hutool.core.util.StrUtil;
import com.lemostic.work.modules.deployment.model.DeploymentResult;
import com.lemostic.work.modules.deployment.model.DeploymentTask;
import com.lemostic.work.modules.deployment.model.ServerConfiguration;
import com.lemostic.work.modules.deployment.service.DeploymentService;
import com.lemostic.work.utils.UIUtils;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign.MaterialDesign;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 包部署视图
 */
public class PackageDeploymentView extends StackPane {
    
    private static final int LABEL_WIDTH = 100;
    private static final int FORM_ITEM_WIDTH = 250;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private final DeploymentService deploymentService;
    
    // 服务器配置相关控件
    private TableView<ServerConfiguration> serverConfigTable;
    private ObservableList<ServerConfiguration> serverConfigs;
    private TextField configNameField;
    private TextField hostField;
    private TextField portField;
    private TextField usernameField;
    private PasswordField passwordField;
    private TextField privateKeyPathField;
    private PasswordField privateKeyPassphraseField;
    private TextField uploadDirField;
    private TextField installDirField;
    private TextField backupDirField;
    private TextArea descriptionArea;
    private CheckBox enabledCheckBox;
    private Button saveConfigButton;
    private Button deleteConfigButton;
    private Button testConnectionButton;
    private Label connectionStatusLabel;
    
    // 部署任务相关控件
    private TextField packagePathField;
    private Button browsePackageButton;
    private TextArea filesToMoveArea;
    private ListView<ServerConfiguration> targetServersListView;
    private CheckBox createBackupCheckBox;
    private CheckBox cleanupAfterDeploymentCheckBox;
    private TextField taskDescriptionField;
    private Button startDeploymentButton;
    private ProgressBar deploymentProgressBar;
    private TextArea deploymentLogArea;
    private Label statusLabel;
    
    // 历史记录相关控件
    private TableView<DeploymentResult> historyTable;
    private ObservableList<DeploymentResult> deploymentHistory;
    
    public PackageDeploymentView(DeploymentService deploymentService) {
        this.deploymentService = deploymentService;
        
        initializeComponents();
        setupLayout();
        setupEventHandlers();

        // 初始化时刷新数据
        refreshServerConfigurations();
        refreshDeploymentHistory();
        loadData();
    }
    
    private void initializeComponents() {
        // 服务器配置表格
        serverConfigs = FXCollections.observableArrayList();
        serverConfigTable = new TableView<>(serverConfigs);
        setupServerConfigTable();
        
        // 服务器配置表单
        configNameField = new TextField();
        configNameField.setPromptText("配置名称");
        configNameField.setPrefWidth(FORM_ITEM_WIDTH);
        
        hostField = new TextField();
        hostField.setPromptText("服务器地址");
        hostField.setPrefWidth(FORM_ITEM_WIDTH);
        
        portField = new TextField("22");
        portField.setPromptText("SSH端口");
        portField.setPrefWidth(FORM_ITEM_WIDTH);
        
        usernameField = new TextField();
        usernameField.setPromptText("用户名");
        usernameField.setPrefWidth(FORM_ITEM_WIDTH);
        
        passwordField = new PasswordField();
        passwordField.setPromptText("密码");
        passwordField.setPrefWidth(FORM_ITEM_WIDTH);
        
        privateKeyPathField = new TextField();
        privateKeyPathField.setPromptText("私钥文件路径（可选）");
        privateKeyPathField.setPrefWidth(FORM_ITEM_WIDTH);
        
        privateKeyPassphraseField = new PasswordField();
        privateKeyPassphraseField.setPromptText("私钥密码（可选）");
        privateKeyPassphraseField.setPrefWidth(FORM_ITEM_WIDTH);
        
        uploadDirField = new TextField();
        uploadDirField.setPromptText("上传目录 (如不存在将自动创建)");
        uploadDirField.setPrefWidth(FORM_ITEM_WIDTH);

        installDirField = new TextField();
        installDirField.setPromptText("安装目录 (如不存在将自动创建)");
        installDirField.setPrefWidth(FORM_ITEM_WIDTH);

        backupDirField = new TextField();
        backupDirField.setPromptText("备份目录 (如不存在将自动创建)");
        backupDirField.setPrefWidth(FORM_ITEM_WIDTH);
        
        descriptionArea = new TextArea();
        descriptionArea.setPromptText("配置描述");
        descriptionArea.setPrefRowCount(3);
        descriptionArea.setPrefWidth(FORM_ITEM_WIDTH);
        
        enabledCheckBox = new CheckBox("启用此配置");
        enabledCheckBox.setSelected(true);
        
        saveConfigButton = new Button("保存配置");
        saveConfigButton.setGraphic(new FontIcon(MaterialDesign.MDI_CONTENT_SAVE));
        
        deleteConfigButton = new Button("删除配置");
        deleteConfigButton.setGraphic(new FontIcon(MaterialDesign.MDI_DELETE));
        
        testConnectionButton = new Button("测试连接");
        testConnectionButton.setGraphic(new FontIcon(MaterialDesign.MDI_WIFI));
        
        connectionStatusLabel = new Label();
        connectionStatusLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
        
        // 部署任务控件
        packagePathField = new TextField();
        packagePathField.setPromptText("选择要部署的包文件");
        packagePathField.setPrefWidth(FORM_ITEM_WIDTH);
        packagePathField.setEditable(false);
        
        browsePackageButton = new Button("浏览");
        browsePackageButton.setGraphic(new FontIcon(MaterialDesign.MDI_FOLDER));
        
        filesToMoveArea = new TextArea();
        filesToMoveArea.setPromptText("要移动的文件/目录列表（每行一个，相对于解压后的包根目录）");
        filesToMoveArea.setPrefRowCount(5);
        filesToMoveArea.setPrefWidth(FORM_ITEM_WIDTH);
        
        targetServersListView = new ListView<>();
        targetServersListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        targetServersListView.setPrefHeight(150);

        // 设置空数据显示文本
        UIUtils.setEmptyDataPlaceholder(targetServersListView);
        
        createBackupCheckBox = new CheckBox("部署前创建备份");
        createBackupCheckBox.setSelected(true);
        
        cleanupAfterDeploymentCheckBox = new CheckBox("部署后清理临时文件");
        cleanupAfterDeploymentCheckBox.setSelected(true);
        
        taskDescriptionField = new TextField();
        taskDescriptionField.setPromptText("任务描述");
        taskDescriptionField.setPrefWidth(FORM_ITEM_WIDTH);
        
        startDeploymentButton = new Button("开始部署");
        startDeploymentButton.setGraphic(new FontIcon(MaterialDesign.MDI_ROCKET));
        
        deploymentProgressBar = new ProgressBar(0);
        deploymentProgressBar.setPrefWidth(FORM_ITEM_WIDTH);
        deploymentProgressBar.setVisible(false);
        
        deploymentLogArea = new TextArea();
        deploymentLogArea.setPromptText("部署日志将在这里显示");
        deploymentLogArea.setEditable(false);
        deploymentLogArea.setPrefRowCount(10);
        
        statusLabel = new Label("就绪");
        statusLabel.setTextFill(Color.GREEN);
        statusLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
        
        // 历史记录表格
        deploymentHistory = FXCollections.observableArrayList();
        historyTable = new TableView<>(deploymentHistory);
        setupHistoryTable();
    }
    
    private void setupServerConfigTable() {
        TableColumn<ServerConfiguration, String> nameColumn = new TableColumn<>("配置名称");
        nameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));

        TableColumn<ServerConfiguration, String> hostColumn = new TableColumn<>("服务器地址");
        hostColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getHost()));

        TableColumn<ServerConfiguration, String> usernameColumn = new TableColumn<>("用户名");
        usernameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getUsername()));

        TableColumn<ServerConfiguration, String> statusColumn = new TableColumn<>("状态");
        statusColumn.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().isEnabled() ? "启用" : "禁用"));

        serverConfigTable.getColumns().addAll(nameColumn, hostColumn, usernameColumn, statusColumn);

        // 设置列宽自动调整，铺满整个表格
        serverConfigTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // 设置各列的相对宽度
        nameColumn.setMaxWidth(1f * Integer.MAX_VALUE * 25); // 25%
        hostColumn.setMaxWidth(1f * Integer.MAX_VALUE * 35); // 35%
        usernameColumn.setMaxWidth(1f * Integer.MAX_VALUE * 25); // 25%
        statusColumn.setMaxWidth(1f * Integer.MAX_VALUE * 15); // 15%

        serverConfigTable.setPrefHeight(200);

        // 设置空数据显示文本
        UIUtils.setEmptyDataPlaceholder(serverConfigTable);

        // 选择事件
        serverConfigTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                loadServerConfigToForm(newSelection);
            }
        });

    }
    
    private void setupHistoryTable() {
        TableColumn<DeploymentResult, String> taskIdColumn = new TableColumn<>("任务ID");
        taskIdColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTaskId()));

        TableColumn<DeploymentResult, String> serverColumn = new TableColumn<>("服务器");
        serverColumn.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getServerConfiguration().getHost()));

        TableColumn<DeploymentResult, String> statusColumn = new TableColumn<>("状态");
        statusColumn.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().isSuccess() ? "成功" : "失败"));

        TableColumn<DeploymentResult, String> timeColumn = new TableColumn<>("完成时间");
        timeColumn.setCellValueFactory(data -> {
            if (data.getValue().getEndTime() != null) {
                return new SimpleStringProperty(data.getValue().getEndTime().format(TIME_FORMAT));
            }
            return new SimpleStringProperty("");
        });

        TableColumn<DeploymentResult, String> durationColumn = new TableColumn<>("耗时");
        durationColumn.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getFormattedDuration()));

        historyTable.getColumns().addAll(taskIdColumn, serverColumn, statusColumn, timeColumn, durationColumn);

        // 设置列宽自动调整，铺满整个表格
        historyTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // 设置各列的相对宽度
        taskIdColumn.setMaxWidth(1f * Integer.MAX_VALUE * 20); // 20%
        serverColumn.setMaxWidth(1f * Integer.MAX_VALUE * 25); // 25%
        statusColumn.setMaxWidth(1f * Integer.MAX_VALUE * 15); // 15%
        timeColumn.setMaxWidth(1f * Integer.MAX_VALUE * 30); // 30%
        durationColumn.setMaxWidth(1f * Integer.MAX_VALUE * 10); // 10%

        historyTable.setPrefHeight(200);

        // 设置空数据显示文本
        UIUtils.setEmptyDataPlaceholder(historyTable);
    }

    private void setupLayout() {
        TabPane tabPane = new TabPane();

        // 服务器配置标签页
        Tab configTab = new Tab("服务器配置");
        configTab.setClosable(false);
        configTab.setContent(createServerConfigPane());

        // 部署任务标签页
        Tab deploymentTab = new Tab("部署任务");
        deploymentTab.setClosable(false);
        deploymentTab.setContent(createDeploymentPane());

        // 历史记录标签页
        Tab historyTab = new Tab("历史记录");
        historyTab.setClosable(false);
        historyTab.setContent(createHistoryPane());

        tabPane.getTabs().addAll(configTab, deploymentTab, historyTab);

        getChildren().add(tabPane);
    }

    private VBox createServerConfigPane() {
        VBox pane = new VBox(10);
        pane.setPadding(new Insets(20));

        // 配置列表
        Label tableLabel = new Label("服务器配置列表");
        tableLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        // 配置表单
        Label formLabel = new Label("配置详情");
        formLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        // 创建两栏布局
        HBox formContainer = new HBox(20);
        formContainer.setPadding(new Insets(10));

        // 左栏
        VBox leftColumn = new VBox(10);
        leftColumn.setPrefWidth(350);

        GridPane leftGrid = new GridPane();
        leftGrid.setHgap(10);
        leftGrid.setVgap(10);

        int leftRow = 0;
        addFormRow(leftGrid, leftRow++, "配置名称:", configNameField);
        addFormRow(leftGrid, leftRow++, "服务器地址:", hostField);
        addFormRow(leftGrid, leftRow++, "SSH端口:", portField);
        addFormRow(leftGrid, leftRow++, "用户名:", usernameField);
        addFormRow(leftGrid, leftRow++, "密码:", passwordField);
        addFormRow(leftGrid, leftRow++, "私钥路径:", privateKeyPathField);

        leftColumn.getChildren().add(leftGrid);

        // 右栏
        VBox rightColumn = new VBox(10);
        rightColumn.setPrefWidth(350);

        GridPane rightGrid = new GridPane();
        rightGrid.setHgap(10);
        rightGrid.setVgap(10);

        int rightRow = 0;
        addFormRow(rightGrid, rightRow++, "私钥密码:", privateKeyPassphraseField);
        addFormRow(rightGrid, rightRow++, "上传目录:", uploadDirField);
        addFormRow(rightGrid, rightRow++, "安装目录:", installDirField);
        addFormRow(rightGrid, rightRow++, "备份目录:", backupDirField);
        addFormRow(rightGrid, rightRow++, "描述:", descriptionArea);

        rightGrid.add(enabledCheckBox, 1, rightRow++);

        rightColumn.getChildren().add(rightGrid);

        formContainer.getChildren().addAll(leftColumn, rightColumn);

        // 操作按钮区域 - 居中显示
        HBox buttonContainer = new HBox();
        buttonContainer.setAlignment(Pos.CENTER);
        buttonContainer.setPadding(new Insets(15, 0, 10, 0));

        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.getChildren().addAll(saveConfigButton, deleteConfigButton, testConnectionButton);

        // 连接状态标签单独一行居中显示
        HBox statusContainer = new HBox();
        statusContainer.setAlignment(Pos.CENTER);
        statusContainer.getChildren().add(connectionStatusLabel);

        buttonContainer.getChildren().add(buttonBox);

        pane.getChildren().addAll(tableLabel, serverConfigTable, formLabel, formContainer, buttonContainer, statusContainer);

        return pane;
    }

    private VBox createDeploymentPane() {
        VBox pane = new VBox(20);
        pane.setPadding(new Insets(20));

        // 上半部分 - 配置区域
        GridPane configGrid = new GridPane();
        configGrid.setHgap(20);
        configGrid.setVgap(15);
        configGrid.setPadding(new Insets(10));

        // 第一行：包文件选择
        Label packageLabel = new Label("包文件:");
        packageLabel.setStyle("-fx-font-weight: bold;");
        packageLabel.setPrefWidth(100);

        HBox packageBox = new HBox(10);
        packageBox.setAlignment(Pos.CENTER_LEFT);
        packageBox.getChildren().addAll(packagePathField, browsePackageButton);

        configGrid.add(packageLabel, 0, 0);
        configGrid.add(packageBox, 1, 0, 2, 1);

        // 第二行：任务描述
        Label descLabel = new Label("任务描述:");
        descLabel.setStyle("-fx-font-weight: bold;");
        descLabel.setPrefWidth(100);

        configGrid.add(descLabel, 0, 1);
        configGrid.add(taskDescriptionField, 1, 1, 2, 1);

        // 第三行：部署选项
        Label optionsLabel = new Label("部署选项:");
        optionsLabel.setStyle("-fx-font-weight: bold;");

        HBox optionsBox = new HBox(20);
        optionsBox.getChildren().addAll(createBackupCheckBox, cleanupAfterDeploymentCheckBox);

        configGrid.add(optionsLabel, 0, 2);
        configGrid.add(optionsBox, 1, 2, 2, 1);

        // 中间部分 - 两栏布局
        HBox middleContainer = new HBox(20);
        middleContainer.setPadding(new Insets(10, 0, 10, 0));

        // 左栏 - 文件移动配置
        VBox leftColumn = new VBox(10);
        leftColumn.setPrefWidth(400);

        Label filesLabel = new Label("文件移动配置");
        filesLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        Label filesHint = new Label("要移动的文件/目录 (每行一个，相对于解压后的包根目录):");
        filesHint.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");

        leftColumn.getChildren().addAll(filesLabel, filesHint, filesToMoveArea);

        // 右栏 - 服务器选择
        VBox rightColumn = new VBox(10);
        rightColumn.setPrefWidth(350);

        Label serversLabel = new Label("目标服务器选择");
        serversLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        Label serversHint = new Label("选择目标服务器 (可多选):");
        serversHint.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");

        rightColumn.getChildren().addAll(serversLabel, serversHint, targetServersListView);

        middleContainer.getChildren().addAll(leftColumn, rightColumn);

        // 部署控制区域 - 居中
        HBox controlContainer = new HBox();
        controlContainer.setAlignment(Pos.CENTER);
        controlContainer.setPadding(new Insets(15, 0, 15, 0));

        VBox controlBox = new VBox(10);
        controlBox.setAlignment(Pos.CENTER);
        controlBox.getChildren().addAll(startDeploymentButton, deploymentProgressBar, statusLabel);

        controlContainer.getChildren().add(controlBox);

        // 部署日志区域
        Label logLabel = new Label("部署日志");
        logLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        pane.getChildren().addAll(configGrid, middleContainer, controlContainer, logLabel, deploymentLogArea);

        return pane;
    }

    private VBox createHistoryPane() {
        VBox pane = new VBox(10);
        pane.setPadding(new Insets(20));

        Label historyLabel = new Label("部署历史记录");
        historyLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        pane.getChildren().addAll(historyLabel, historyTable);

        return pane;
    }

    private void addFormRow(GridPane grid, int row, String labelText, Control control) {
        Label label = new Label(labelText);
        label.setPrefWidth(LABEL_WIDTH);
        label.setMinWidth(LABEL_WIDTH);

        grid.add(label, 0, row);
        grid.add(control, 1, row);
    }

    private void setupEventHandlers() {
        // 保存配置按钮
        saveConfigButton.setOnAction(event -> saveServerConfiguration());

        // 删除配置按钮
        deleteConfigButton.setOnAction(event -> deleteServerConfiguration());

        // 测试连接按钮
        testConnectionButton.setOnAction(event -> testConnection());

        // 浏览包文件按钮
        browsePackageButton.setOnAction(event -> browsePackageFile());

        // 开始部署按钮
        startDeploymentButton.setOnAction(event -> startDeployment());

        // 清空表单按钮
        Button clearFormButton = new Button("清空表单");
        clearFormButton.setOnAction(event -> clearServerConfigForm());
    }

    private void saveServerConfiguration() {
        try {
            ServerConfiguration config = new ServerConfiguration();
            config.setName(configNameField.getText().trim());
            config.setHost(hostField.getText().trim());
            config.setPort(Integer.parseInt(portField.getText().trim()));
            config.setUsername(usernameField.getText().trim());
            config.setPassword(passwordField.getText());
            config.setPrivateKeyPath(privateKeyPathField.getText().trim());
            config.setPrivateKeyPassphrase(privateKeyPassphraseField.getText());
            config.setUploadDirectory(uploadDirField.getText().trim());
            config.setInstallationDirectory(installDirField.getText().trim());
            config.setBackupDirectory(backupDirField.getText().trim());
            config.setDescription(descriptionArea.getText().trim());
            config.setEnabled(enabledCheckBox.isSelected());

            if (!config.isValid()) {
                updateConnectionStatus("请填写完整的配置信息", Color.RED);
                return;
            }

            deploymentService.saveServerConfiguration(config);
            refreshServerConfigurations();
            updateConnectionStatus("配置保存成功", Color.GREEN);

        } catch (NumberFormatException e) {
            updateConnectionStatus("端口号必须是数字", Color.RED);
        } catch (Exception e) {
            updateConnectionStatus("保存配置失败: " + e.getMessage(), Color.RED);
        }
    }

    private void deleteServerConfiguration() {
        ServerConfiguration selected = serverConfigTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            deploymentService.deleteServerConfiguration(selected.getName());
            refreshServerConfigurations();
            clearServerConfigForm();
            updateConnectionStatus("配置删除成功", Color.GREEN);
        } else {
            updateConnectionStatus("请选择要删除的配置", Color.RED);
        }
    }

    private void testConnection() {
        try {
            ServerConfiguration config = new ServerConfiguration();
            config.setHost(hostField.getText().trim());
            config.setPort(Integer.parseInt(portField.getText().trim()));
            config.setUsername(usernameField.getText().trim());
            config.setPassword(passwordField.getText());
            config.setPrivateKeyPath(privateKeyPathField.getText().trim());
            config.setPrivateKeyPassphrase(privateKeyPassphraseField.getText());

            if (StrUtil.isBlank(config.getHost()) || StrUtil.isBlank(config.getUsername())) {
                connectionStatusLabel.setText("请填写主机地址和用户名");
                connectionStatusLabel.setTextFill(Color.RED);
                return;
            }

            connectionStatusLabel.setText("正在连接...");
            connectionStatusLabel.setTextFill(Color.BLUE);

            // 在后台线程中测试连接
            Thread testThread = new Thread(() -> {
                boolean success = deploymentService.testConnection(config, message -> {
                    Platform.runLater(() -> {
                        connectionStatusLabel.setText(message);
                    });
                });

                Platform.runLater(() -> {
                    connectionStatusLabel.setTextFill(success ? Color.GREEN : Color.RED);
                });
            });
            testThread.setDaemon(true);
            testThread.start();

        } catch (NumberFormatException e) {
            connectionStatusLabel.setText("端口号必须是数字");
            connectionStatusLabel.setTextFill(Color.RED);
        }
    }

    private void browsePackageFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择包文件");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("压缩文件", "*.tar.gz", "*.tgz", "*.zip", "*.tar"),
            new FileChooser.ExtensionFilter("所有文件", "*.*")
        );

        File selectedFile = fileChooser.showOpenDialog(getScene().getWindow());
        if (selectedFile != null) {
            packagePathField.setText(selectedFile.getAbsolutePath());
        }
    }

    private void startDeployment() {
        try {
            // 验证输入
            if (StrUtil.isBlank(packagePathField.getText())) {
                updateStatus("请选择包文件", Color.RED);
                packagePathField.requestFocus();
                return;
            }

            // 验证文件是否存在
            File packageFile = new File(packagePathField.getText());
            if (!packageFile.exists()) {
                updateStatus("包文件不存在: " + packagePathField.getText(), Color.RED);
                packagePathField.requestFocus();
                return;
            }

            if (StrUtil.isBlank(filesToMoveArea.getText())) {
                updateStatus("请指定要移动的文件/目录", Color.RED);
                filesToMoveArea.requestFocus();
                return;
            }

            List<ServerConfiguration> selectedServers = new ArrayList<>(targetServersListView.getSelectionModel().getSelectedItems());
            if (selectedServers.isEmpty()) {
                // 如果没有选择服务器，但只有一个可用服务器，自动选择
                if (targetServersListView.getItems().size() == 1) {
                    selectedServers.add(targetServersListView.getItems().get(0));
                    targetServersListView.getSelectionModel().select(0);
                } else {
                    updateStatus("请选择目标服务器", Color.RED);
                    targetServersListView.requestFocus();
                    return;
                }
            }

            // 创建部署任务
            DeploymentTask task = new DeploymentTask();
            task.setLocalPackagePath(packagePathField.getText());
            task.setTargetServers(selectedServers);
            task.setFilesToMove(filesToMoveArea.getText().lines()
                               .filter(line -> !line.trim().isEmpty())
                               .collect(Collectors.toList()));
            task.setCreateBackup(createBackupCheckBox.isSelected());
            task.setCleanupAfterDeployment(cleanupAfterDeploymentCheckBox.isSelected());
            task.setDescription(StrUtil.isBlank(taskDescriptionField.getText()) ?
                              "部署任务 - " + packageFile.getName() : taskDescriptionField.getText().trim());

            // 禁用部署按钮，显示进度条
            startDeploymentButton.setDisable(true);
            deploymentProgressBar.setVisible(true);
            deploymentLogArea.clear();

            updateStatus("部署开始...", Color.BLUE);

            // 执行部署
            deploymentService.executeDeployment(task,
                result -> {
                    Platform.runLater(() -> {
                        refreshDeploymentHistory();
                        if (result.isSuccess()) {
                            updateStatus("部署到 " + result.getServerConfiguration().getHost() + " 成功", Color.GREEN);
                        } else {
                            updateStatus("部署到 " + result.getServerConfiguration().getHost() + " 失败", Color.RED);
                        }

                        // 检查是否所有部署都完成了，如果是则重新启用按钮
                        if (task.getTargetServers().size() == 1 ||
                            task.getTargetServers().indexOf(result.getServerConfiguration()) == task.getTargetServers().size() - 1) {
                            startDeploymentButton.setDisable(false);
                            deploymentProgressBar.setVisible(false);
                        }
                    });
                },
                message -> {
                    Platform.runLater(() -> {
                        deploymentLogArea.appendText(message + "\n");
                        deploymentLogArea.setScrollTop(Double.MAX_VALUE);
                    });
                }
            );

        } catch (Exception e) {
            updateStatus("启动部署失败: " + e.getMessage(), Color.RED);
            startDeploymentButton.setDisable(false);
            deploymentProgressBar.setVisible(false);
        }
    }

    private void loadData() {
        refreshServerConfigurations();
        refreshDeploymentHistory();
    }

    public void refreshServerConfigurations() {
        List<ServerConfiguration> configs = deploymentService.getAllServerConfigurations();
        serverConfigs.clear();
        serverConfigs.addAll(configs);

        // 更新目标服务器列表
        ObservableList<ServerConfiguration> enabledConfigs = FXCollections.observableArrayList(
            configs.stream().filter(ServerConfiguration::isEnabled).collect(Collectors.toList())
        );
        targetServersListView.setItems(enabledConfigs);

        // 如果只有一个启用的配置，自动选中
        if (enabledConfigs.size() == 1) {
            targetServersListView.getSelectionModel().select(0);
        }
    }

    public void refreshDeploymentHistory() {
        List<DeploymentResult> history = deploymentService.getDeploymentHistory();
        deploymentHistory.clear();
        deploymentHistory.addAll(history);
    }

    private void loadServerConfigToForm(ServerConfiguration config) {
        configNameField.setText(config.getName());
        hostField.setText(config.getHost());
        portField.setText(String.valueOf(config.getPort()));
        usernameField.setText(config.getUsername());
        passwordField.setText(config.getPassword());
        privateKeyPathField.setText(config.getPrivateKeyPath());
        privateKeyPassphraseField.setText(config.getPrivateKeyPassphrase());
        uploadDirField.setText(config.getUploadDirectory());
        installDirField.setText(config.getInstallationDirectory());
        backupDirField.setText(config.getBackupDirectory());
        descriptionArea.setText(config.getDescription());
        enabledCheckBox.setSelected(config.isEnabled());

        connectionStatusLabel.setText("");
    }

    private void clearServerConfigForm() {
        configNameField.clear();
        hostField.clear();
        portField.setText("22");
        usernameField.clear();
        passwordField.clear();
        privateKeyPathField.clear();
        privateKeyPassphraseField.clear();
        uploadDirField.clear();
        installDirField.clear();
        backupDirField.clear();
        descriptionArea.clear();
        enabledCheckBox.setSelected(true);
        connectionStatusLabel.setText("");
    }

    public void updateStatus(String message) {
        updateStatus(message, Color.BLACK);
    }

    public void updateStatus(String message, Color color) {
        statusLabel.setText(message);
        statusLabel.setTextFill(color);
    }

    /**
     * 更新服务器配置状态显示
     */
    public void updateConnectionStatus(String message) {
        updateConnectionStatus(message, Color.BLACK);
    }

    public void updateConnectionStatus(String message, Color color) {
        connectionStatusLabel.setText(message);
        connectionStatusLabel.setTextFill(color);
    }
}
