package io.github.lemostic.toolsuite.modules.devops.deploy.view;

import io.github.lemostic.toolsuite.modules.devops.deploy.dao.*;
import io.github.lemostic.toolsuite.modules.devops.deploy.model.*;
import io.github.lemostic.toolsuite.modules.devops.deploy.model.ServerConfigDTO;
import io.github.lemostic.toolsuite.modules.devops.deploy.service.*;
import io.github.lemostic.toolsuite.modules.devops.deploy.task.*;
import io.github.lemostic.toolsuite.modules.devops.deploy.view.components.*;
import io.github.lemostic.toolsuite.modules.devops.deploy.view.dialogs.*;
import io.github.lemostic.toolsuite.util.UIUtil;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign.MaterialDesign;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

public class OptimizedDeployView extends BorderPane {
    
    private static final Logger logger = LoggerFactory.getLogger(OptimizedDeployView.class);
    
    // DAO层和服务层
    private final ServerConfigDao serverConfigDao = new ServerConfigDao();
    private final DeploymentHistoryDao deploymentHistoryDao = new DeploymentHistoryDao();
    private final SshConnectionPool connectionPool = new SshConnectionPool();
    private final SshCommandService sshService = new SshCommandService(connectionPool);
    private final SftpTransferService sftpService = new SftpTransferService(connectionPool);
    private final CustomStepService customStepService = new CustomStepService();
    
    // 数据
    private final ObservableList<ServerConfigDTO> serverConfigDTOS = FXCollections.observableArrayList();
    private File selectedPackage;
    
    // UI 组件
    private TableView<ServerConfigDTO> serverTable;
    private Label packageLabel;
    private TabPane logTabPane;
    private BackupOptionsPanel backupOptionsPanel;
    private UploadOptionsPanel uploadOptionsPanel;
    private Button deployButton;
    private Button selectPackageButton;
    private ProgressBar progressBar;
    private Label statusLabel;
    
    // 部署任务跟踪
    private final Map<String, TaskPipeline> runningPipelines = new HashMap<>();
    private final Map<String, LogTab> logTabs = new HashMap<>();
    
    public OptimizedDeployView() {
        initializeUI();
        loadServerConfigs();
        setupDragAndDrop();
        setupAnimations();
    }
    
    private void initializeUI() {
        setStyle("-fx-background-color: linear-gradient(to bottom, #f8f9fa, #e9ecef);");
        
        // 顶部工具栏
        setTop(createToolbar());
        
        // 主内容区 - 分割面板
        SplitPane splitPane = new SplitPane();
        splitPane.setOrientation(javafx.geometry.Orientation.VERTICAL);
        splitPane.setDividerPositions(0.6);
        
        // 上半部分：服务器和包选择
        splitPane.getItems().add(createMainContent());
        
        // 下半部分：日志和进度
        VBox bottomSection = createBottomSection();
        splitPane.getItems().add(bottomSection);
        
        setCenter(splitPane);
        
        // 底部状态栏
        setBottom(createStatusBar());
    }
    
    private Node createToolbar() {
        ToolBar toolbar = new ToolBar();
        toolbar.setStyle("-fx-background-color: linear-gradient(to bottom, #ffffff, #f1f3f4); " +
                        "-fx-border-color: #dadce0; -fx-border-width: 0 0 1 0;");
        
        // 左侧按钮组
        HBox leftButtons = new HBox(8);
        
        Button addServerBtn = createStyledButton("添加服务器", MaterialDesign.MDI_SERVER_PLUS, "#4285F4");
        addServerBtn.setOnAction(e -> showAddServerDialog());
        
        Button editServerBtn = createStyledButton("编辑", MaterialDesign.MDI_PENCIL, "#FBBC05");
        editServerBtn.setOnAction(e -> showEditServerDialog());
        
        Button removeServerBtn = createStyledButton("删除", MaterialDesign.MDI_DELETE, "#EA4335");
        removeServerBtn.setOnAction(e -> removeSelectedServer());
        
        Button testConnBtn = createStyledButton("测试连接", MaterialDesign.MDI_LAN_CONNECT, "#34A853");
        testConnBtn.setOnAction(e -> testSelectedConnection());
        
        leftButtons.getChildren().addAll(addServerBtn, editServerBtn, removeServerBtn, 
                                       new Separator(), testConnBtn);
        
        // 右侧按钮组
        HBox rightButtons = new HBox(8);
        
        Button refreshBtn = createStyledButton("刷新", MaterialDesign.MDI_REFRESH, "#9AA0A6");
        refreshBtn.setOnAction(e -> loadServerConfigs());
        
        Button settingsBtn = createStyledButton("设置", MaterialDesign.MDI_SETTINGS, "#5F6368");
        settingsBtn.setOnAction(e -> showSettingsDialog());
        
        Button helpBtn = createStyledButton("帮助", MaterialDesign.MDI_HELP_CIRCLE, "#5F6368");
        helpBtn.setOnAction(e -> showHelp());
        
        rightButtons.getChildren().addAll(refreshBtn, settingsBtn, helpBtn);
        
        // 中间弹性空间
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        toolbar.getItems().addAll(leftButtons, spacer, rightButtons);
        
        return toolbar;
    }
    
    private Node createMainContent() {
        VBox mainContent = new VBox(15);
        mainContent.setPadding(new Insets(15));
        
        // 标题区域
        HBox titleArea = new HBox(10);
        titleArea.setAlignment(Pos.CENTER_LEFT);
        
        FontIcon titleIcon = new FontIcon(MaterialDesign.MDI_ROCKET);
        titleIcon.setIconSize(28);
        titleIcon.setStyle("-fx-icon-color: #4285F4;");
        
        Label titleLabel = new Label("应用部署工具");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #202124;");
        
        Region titleSpacer = new Region();
        HBox.setHgrow(titleSpacer, Priority.ALWAYS);
        
        Label subtitleLabel = new Label("快速、安全地部署您的应用程序");
        subtitleLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #5F6368;");
        
        titleArea.getChildren().addAll(titleIcon, titleLabel, titleSpacer, subtitleLabel);
        
        // 内容区域 - 卡片布局
        HBox contentArea = new HBox(15);
        
        // 左侧：服务器卡片
        VBox serverCard = createServerCard();
        
        // 中间：程序包卡片
        VBox packageCard = createPackageCard();
        
        // 右侧：选项卡片
        VBox optionsCard = createOptionsCard();
        
        HBox.setHgrow(serverCard, Priority.ALWAYS);
        contentArea.getChildren().addAll(serverCard, packageCard, optionsCard);
        
        mainContent.getChildren().addAll(titleArea, contentArea);
        
        return mainContent;
    }
    
    private VBox createServerCard() {
        VBox card = createStyledCard("服务器配置", MaterialDesign.MDI_SERVER_NETWORK);
        
        // 服务器表格
        serverTable = new TableView<>();
        serverTable.setItems(serverConfigDTOS);
        serverTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        serverTable.setPrefHeight(300);
        
        // 选择列
        TableColumn<ServerConfigDTO, Boolean> selectCol = new TableColumn<>("部署");
        selectCol.setCellValueFactory(cellData -> cellData.getValue().enabledProperty());
        selectCol.setCellFactory(CheckBoxTableCell.forTableColumn(selectCol));
        selectCol.setEditable(true);
        selectCol.setMaxWidth(60);
        selectCol.setStyle("-fx-alignment: CENTER;");
        
        // 名称列
        TableColumn<ServerConfigDTO, String> nameCol = new TableColumn<>("名称");
        nameCol.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        nameCol.setPrefWidth(120);
        
        // 主机列
        TableColumn<ServerConfigDTO, String> hostCol = new TableColumn<>("主机");
        hostCol.setCellValueFactory(cellData -> cellData.getValue().hostProperty());
        hostCol.setPrefWidth(150);
        
        // 状态列
        TableColumn<ServerConfigDTO, String> statusCol = new TableColumn<>("状态");
        statusCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty("就绪"));
        statusCol.setPrefWidth(80);
        statusCol.setStyle("-fx-alignment: CENTER;");
        
        serverTable.getColumns().addAll(selectCol, nameCol, hostCol, statusCol);
        serverTable.setEditable(true);
        serverTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        
        UIUtil.setEmptyDataPlaceholder(serverTable, "暂无服务器配置");
        
        VBox.setVgrow(serverTable, Priority.ALWAYS);
        card.getChildren().add(serverTable);
        
        return card;
    }
    
    private VBox createPackageCard() {
        VBox card = createStyledCard("程序包", MaterialDesign.MDI_PACKAGE_VARIANT);
        card.setPrefWidth(280);
        
        // 拖放区域
        VBox dropZone = new VBox(15);
        dropZone.setAlignment(Pos.CENTER);
        dropZone.setStyle("-fx-background-color: #f1f3f4; " +
                         "-fx-border-color: #dadce0; " +
                         "-fx-border-style: dashed; " +
                         "-fx-border-width: 2; " +
                         "-fx-border-radius: 8; " +
                         "-fx-background-radius: 8;" +
                         "-fx-padding: 30;");
        dropZone.setMinHeight(200);
        
        FontIcon dropIcon = new FontIcon(MaterialDesign.MDI_CLOUD_UPLOAD);
        dropIcon.setIconSize(48);
        dropIcon.setStyle("-fx-icon-color: #5F6368;");
        
        Label dropLabel = new Label("拖放程序包到此处");
        dropLabel.setStyle("-fx-text-fill: #5F6368; -fx-font-size: 14px;");
        
        Label orLabel = new Label("或");
        orLabel.setStyle("-fx-text-fill: #9AA0A6; -fx-font-size: 12px;");
        
        selectPackageButton = new Button("选择文件");
        selectPackageButton.setStyle("-fx-background-color: #4285F4; " +
                                   "-fx-text-fill: white; " +
                                   "-fx-font-weight: bold; " +
                                   "-fx-padding: 8 16;");
        selectPackageButton.setOnAction(e -> selectPackageFile());
        
        dropZone.getChildren().addAll(dropIcon, dropLabel, orLabel, selectPackageButton);
        
        // 已选文件信息
        packageLabel = new Label("未选择文件");
        packageLabel.setWrapText(true);
        packageLabel.setStyle("-fx-text-fill: #5F6368; -fx-font-size: 12px; " +
                             "-fx-background-color: #f8f9fa; " +
                             "-fx-padding: 10; " +
                             "-fx-border-radius: 4;");
        packageLabel.setAlignment(Pos.CENTER);
        packageLabel.setMinHeight(60);
        
        // 部署按钮
        deployButton = new Button("开始部署", new FontIcon(MaterialDesign.MDI_ROCKET));
        deployButton.setStyle("-fx-background-color: #34A853; " +
                             "-fx-text-fill: white; " +
                             "-fx-font-size: 14px; " +
                             "-fx-font-weight: bold; " +
                             "-fx-padding: 12 24;");
        deployButton.setPrefWidth(Double.MAX_VALUE);
        deployButton.setDisable(true);
        deployButton.setOnAction(e -> startDeployment());
        
        card.getChildren().addAll(dropZone, packageLabel, deployButton);
        
        return card;
    }
    
    private VBox createOptionsCard() {
        VBox card = createStyledCard("部署选项", MaterialDesign.MDI_SETTINGS);
        card.setPrefWidth(320);
        
        // 选项标签页
        TabPane optionsTabPane = new TabPane();
        optionsTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        optionsTabPane.setStyle("-fx-tab-min-width: 100;");
        
        // 备份选项Tab
        backupOptionsPanel = new BackupOptionsPanel();
        Tab backupTab = new Tab("备份策略", backupOptionsPanel);
        backupTab.setGraphic(new FontIcon(MaterialDesign.MDI_BACKUP_RESTORE));
        
        // 上传选项Tab
        uploadOptionsPanel = new UploadOptionsPanel();
        Tab uploadTab = new Tab("传输设置", uploadOptionsPanel);
        uploadTab.setGraphic(new FontIcon(MaterialDesign.MDI_UPLOAD));
        
        // 自定义步骤Tab
        Tab customStepsTab = new Tab("自定义步骤");
        customStepsTab.setGraphic(new FontIcon(MaterialDesign.MDI_FORMAT_LIST_BULLETED));
        customStepsTab.setContent(createCustomStepsPanel());
        
        optionsTabPane.getTabs().addAll(backupTab, uploadTab, customStepsTab);
        VBox.setVgrow(optionsTabPane, Priority.ALWAYS);
        
        card.getChildren().add(optionsTabPane);
        
        return card;
    }
    
    private Node createCustomStepsPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(10));
        
        Label titleLabel = new Label("自定义部署步骤");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        TextArea stepsTextArea = new TextArea();
        stepsTextArea.setPromptText("在此处定义自定义部署步骤...\n" +
                                  "例如：\n" +
                                  "1. 检查磁盘空间\n" +
                                  "2. 验证配置文件\n" +
                                  "3. 执行预部署脚本");
        stepsTextArea.setPrefRowCount(8);
        
        Button saveStepsBtn = new Button("保存步骤");
        saveStepsBtn.setStyle("-fx-background-color: #4285F4; -fx-text-fill: white;");
        saveStepsBtn.setOnAction(e -> saveCustomSteps(stepsTextArea.getText()));
        
        panel.getChildren().addAll(titleLabel, stepsTextArea, saveStepsBtn);
        
        return panel;
    }
    
    private VBox createBottomSection() {
        VBox bottomSection = new VBox(10);
        bottomSection.setPadding(new Insets(10));
        
        // 进度条区域
        HBox progressArea = new HBox(10);
        progressArea.setAlignment(Pos.CENTER_LEFT);
        
        Label progressLabel = new Label("部署进度:");
        progressLabel.setStyle("-fx-font-weight: bold;");
        
        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(200);
        progressBar.setVisible(false);
        
        statusLabel = new Label("就绪");
        statusLabel.setStyle("-fx-text-fill: #5F6368;");
        
        Region progressSpacer = new Region();
        HBox.setHgrow(progressSpacer, Priority.ALWAYS);
        
        progressArea.getChildren().addAll(progressLabel, progressBar, statusLabel, progressSpacer);
        
        // 日志标签页
        logTabPane = new TabPane();
        logTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
        logTabPane.setPrefHeight(200);
        
        bottomSection.getChildren().addAll(progressArea, logTabPane);
        
        return bottomSection;
    }
    
    private Node createStatusBar() {
        HBox statusBar = new HBox(15);
        statusBar.setPadding(new Insets(8, 15, 8, 15));
        statusBar.setStyle("-fx-background-color: linear-gradient(to top, #ffffff, #f8f9fa); " +
                          "-fx-border-color: #dadce0; -fx-border-width: 1 0 0 0;");
        statusBar.setAlignment(Pos.CENTER_LEFT);
        
        FontIcon statusIcon = new FontIcon(MaterialDesign.MDI_CHECK_CIRCLE);
        statusIcon.setIconSize(16);
        statusIcon.setStyle("-fx-icon-color: #34A853;");
        
        Label statusTextLabel = new Label("系统就绪");
        statusTextLabel.setStyle("-fx-text-fill: #5F6368; -fx-font-size: 12px;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Label timeLabel = new Label(java.time.LocalTime.now().toString().substring(0, 8));
        timeLabel.setStyle("-fx-text-fill: #9AA0A6; -fx-font-size: 12px;");
        
        statusBar.getChildren().addAll(statusIcon, statusTextLabel, spacer, timeLabel);
        
        // 定时更新时间
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            timeLabel.setText(java.time.LocalTime.now().toString().substring(0, 8));
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
        
        return statusBar;
    }
    
    // 辅助方法
    private Button createStyledButton(String text, MaterialDesign icon, String color) {
        Button button = new Button(text, new FontIcon(icon));
        button.setStyle("-fx-background-color: " + color + "; " +
                       "-fx-text-fill: white; " +
                       "-fx-font-weight: bold; " +
                       "-fx-padding: 6 12; " +
                       "-fx-background-radius: 4;");
        return button;
    }
    
    private VBox createStyledCard(String title, MaterialDesign icon) {
        VBox card = new VBox(12);
        card.setStyle("-fx-background-color: white; " +
                     "-fx-border-color: #dadce0; " +
                     "-fx-border-radius: 8; " +
                     "-fx-background-radius: 8; " +
                     "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2); " +
                     "-fx-padding: 15;");
        
        // 标题栏
        HBox titleBar = new HBox(8);
        titleBar.setAlignment(Pos.CENTER_LEFT);
        
        FontIcon titleIcon = new FontIcon(icon);
        titleIcon.setIconSize(20);
        titleIcon.setStyle("-fx-icon-color: #4285F4;");
        
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #202124;");
        
        titleBar.getChildren().addAll(titleIcon, titleLabel);
        
        Separator separator = new Separator();
        
        card.getChildren().addAll(titleBar, separator);
        
        return card;
    }
    
    private void setupDragAndDrop() {
        // 整个视图的拖放支持
        this.setOnDragOver(event -> {
            if (event.getGestureSource() != this && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });
        
        this.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles()) {
                List<File> files = db.getFiles();
                if (!files.isEmpty()) {
                    setPackageFile(files.get(0));
                    success = true;
                }
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }
    
    private void setupAnimations() {
        // 添加淡入动画
        FadeTransition fadeTransition = new FadeTransition(Duration.millis(500), this);
        fadeTransition.setFromValue(0.0);
        fadeTransition.setToValue(1.0);
        fadeTransition.play();
    }
    
    // 现有的业务方法保持不变
    private void selectPackageFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择程序包");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("所有支持的格式", "*.zip", "*.tar.gz", "*.tgz", "*.jar", "*.war"),
            new FileChooser.ExtensionFilter("压缩包", "*.zip", "*.tar.gz", "*.tgz"),
            new FileChooser.ExtensionFilter("Java程序包", "*.jar", "*.war"),
            new FileChooser.ExtensionFilter("所有文件", "*.*")
        );
        
        File file = fileChooser.showOpenDialog(getScene().getWindow());
        if (file != null) {
            setPackageFile(file);
        }
    }
    
    private void setPackageFile(File file) {
        this.selectedPackage = file;
        packageLabel.setText(file.getName() + "\n" + 
            String.format("%.2f MB", file.length() / 1024.0 / 1024.0));
        packageLabel.setStyle("-fx-text-fill: #202124; -fx-font-weight: bold; " +
                             "-fx-background-color: #e8f0fe; " +
                             "-fx-padding: 10; " +
                             "-fx-border-radius: 4;");
        
        updateDeployButton();
        animateSuccessFeedback();
    }
    
    private void updateDeployButton() {
        boolean hasServer = serverConfigDTOS.stream().anyMatch(ServerConfigDTO::getEnabled);
        boolean hasPackage = selectedPackage != null;
        deployButton.setDisable(!(hasServer && hasPackage));
    }
    
    private void animateSuccessFeedback() {
        ScaleTransition scaleTransition = new ScaleTransition(Duration.millis(200), packageLabel);
        scaleTransition.setFromX(1.0);
        scaleTransition.setFromY(1.0);
        scaleTransition.setToX(1.05);
        scaleTransition.setToY(1.05);
        scaleTransition.setAutoReverse(true);
        scaleTransition.setCycleCount(2);
        scaleTransition.play();
    }
    
    private void showAddServerDialog() {
        ServerConfigDialog dialog = new ServerConfigDialog();
        dialog.showAndWait().ifPresent(config -> {
            try {
                serverConfigDao.save(convertToDao(config));
                serverConfigDTOS.add(config);
                saveServerConfigs();
                showNotification("服务器添加成功", "已成功添加新的服务器配置", NotificationType.SUCCESS);
            } catch (Exception e) {
                logger.error("保存服务器配置失败", e);
                showNotification("操作失败", "保存失败: " + e.getMessage(), NotificationType.ERROR);
            }
        });
    }
    
    private void showEditServerDialog() {
        ServerConfigDTO selected = serverTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showNotification("提示", "请先选择一个服务器", NotificationType.INFO);
            return;
        }
        
        ServerConfigDialog dialog = new ServerConfigDialog(selected);
        dialog.showAndWait().ifPresent(config -> {
            try {
                serverConfigDao.save(convertToDao(config));
                int index = serverConfigDTOS.indexOf(selected);
                serverConfigDTOS.set(index, config);
                saveServerConfigs();
                showNotification("更新成功", "服务器配置已更新", NotificationType.SUCCESS);
            } catch (Exception e) {
                logger.error("更新服务器配置失败", e);
                showNotification("操作失败", "更新失败: " + e.getMessage(), NotificationType.ERROR);
            }
        });
    }
    
    private void removeSelectedServer() {
        ServerConfigDTO selected = serverTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showNotification("提示", "请先选择一个服务器", NotificationType.INFO);
            return;
        }
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("确认删除");
        confirm.setHeaderText("删除服务器配置");
        confirm.setContentText("确定要删除服务器 \"" + selected.getName() + "\" 吗？");
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    if (selected.getId() != null) {
                        serverConfigDao.deleteById(selected.getId());
                    }
                    serverConfigDTOS.remove(selected);
                    saveServerConfigs();
                    showNotification("删除成功", "服务器配置已删除", NotificationType.SUCCESS);
                } catch (Exception e) {
                    logger.error("删除服务器配置失败", e);
                    showNotification("操作失败", "删除失败: " + e.getMessage(), NotificationType.ERROR);
                }
            }
        });
    }
    
    private void testSelectedConnection() {
        ServerConfigDTO selected = serverTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showNotification("提示", "请先选择一个服务器", NotificationType.INFO);
            return;
        }
        
        statusLabel.setText("正在测试连接...");
        progressBar.setVisible(true);
        progressBar.setProgress(-1); // 不确定进度
        
        new Thread(() -> {
            boolean success = sshService.testConnection(selected);
            Platform.runLater(() -> {
                progressBar.setVisible(false);
                if (success) {
                    statusLabel.setText("连接测试成功");
                    showNotification("连接测试", "连接成功！", NotificationType.SUCCESS);
                } else {
                    statusLabel.setText("连接测试失败");
                    showNotification("连接测试", "连接失败，请检查配置", NotificationType.ERROR);
                }
            });
        }).start();
    }
    
    private void startDeployment() {
        List<ServerConfigDTO> selectedServers = serverConfigDTOS.stream()
            .filter(ServerConfigDTO::getEnabled)
            .toList();
        
        if (selectedServers.isEmpty()) {
            showNotification("错误", "请至少选择一个服务器", NotificationType.ERROR);
            return;
        }
        
        if (selectedPackage == null) {
            showNotification("错误", "请选择程序包", NotificationType.ERROR);
            return;
        }
        
        // 确认对话框
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("确认部署");
        confirm.setHeaderText("即将开始部署");
        confirm.setContentText(String.format(
            "目标服务器: %d 台\n程序包: %s\n\n是否继续？",
            selectedServers.size(), selectedPackage.getName()
        ));
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                executeDeployment(selectedServers);
            }
        });
    }
    
    private void executeDeployment(List<ServerConfigDTO> servers) {
        BackupOptions backupOptions = backupOptionsPanel.getOptions();
        UploadOptions uploadOptions = uploadOptionsPanel.getOptions();
        
        // 清空之前的日志Tab
        logTabPane.getTabs().clear();
        logTabs.clear();
        runningPipelines.clear();
        
        progressBar.setVisible(true);
        progressBar.setProgress(0);
        statusLabel.setText("开始部署...");
        
        for (int i = 0; i < servers.size(); i++) {
            ServerConfigDTO server = servers.get(i);
            // 创建日志Tab
            LogTab logTab = new LogTab(server);
            logTabPane.getTabs().add(logTab);
            logTabs.put(server.getName(), logTab);
            
            // 创建部署流水线
            TaskPipeline pipeline = new TaskPipeline("Deploy-" + server.getName())
                .onLog(logTab::appendLog)
                .onStepComplete((step, result) -> {
                    Platform.runLater(() -> {
                        logTab.updateStatus(step.getName() + ": " + 
                            (result.isSuccess() ? "完成" : "失败"));
                    });
                })
                .onComplete(result -> {
                    Platform.runLater(() -> {
                        logTab.setCompleted(result.isSuccess());
                        if (!result.isSuccess()) {
                            logTab.appendLog("\n部署失败: " + result.getMessage());
                        }
                        runningPipelines.remove(server.getName());
                        
                        // 更新整体进度
                        updateOverallProgress(servers.size() - runningPipelines.size(), servers.size());
                        
                        // 如果所有任务完成
                        if (runningPipelines.isEmpty()) {
                            progressBar.setVisible(false);
                            statusLabel.setText("部署完成");
                            showNotification("部署完成", 
                                result.isSuccess() ? "所有部署任务已完成" : "部分部署任务失败", 
                                result.isSuccess() ? NotificationType.SUCCESS : NotificationType.WARNING);
                        }
                    });
                });
            
            // 设置取消回调
            logTab.setOnCancel(() -> pipeline.cancel());
            
            // 添加部署步骤
            pipeline.getContext().setAttribute("sshService", sshService);
            
            // 添加标准部署步骤
            pipeline.addStep(DeploySteps.createConnectStep(sshService, server))
                   .addStep(DeploySteps.createStopServiceStep(sshService, server))
                   .addStep(DeploySteps.createBackupStep(sshService, server, backupOptions));
            
            // 添加自定义步骤
            List<TaskStep> customSteps = customStepService.createTaskSteps(customStepService.getEnabledSteps());
            for (TaskStep customStep : customSteps) {
                pipeline.addStep(customStep);
            }
            
            // 添加剩余的标准步骤
            pipeline.addStep(DeploySteps.createUploadStep(sftpService, server, 
                       selectedPackage.getAbsolutePath(), uploadOptions))
                   .addStep(DeploySteps.createStartServiceStep(sshService, server));
            
            runningPipelines.put(server.getName(), pipeline);
            
            // 开始执行
            logTab.setRunning(true);
            pipeline.execute();
        }
        
        // 选中第一个Tab
        if (!logTabPane.getTabs().isEmpty()) {
            logTabPane.getSelectionModel().select(0);
        }
    }
    
    private void updateOverallProgress(int completed, int total) {
        double progress = (double) completed / total;
        progressBar.setProgress(progress);
        statusLabel.setText(String.format("部署进度: %d/%d (%.1f%%)", completed, total, progress * 100));
    }
    
    private void loadServerConfigs() {
        try {
            List<ServerConfigDTO> servers =
                serverConfigDao.findEnabledConfigs().stream()
                    .map(this::convertToModel)
                    .toList();
            serverConfigDTOS.clear();
            serverConfigDTOS.addAll(servers);
            logger.info("加载了 {} 个服务器配置", servers.size());
            statusLabel.setText("已加载 " + servers.size() + " 个服务器配置");
        } catch (Exception e) {
            logger.error("加载服务器配置失败", e);
            showNotification("错误", "加载服务器配置失败: " + e.getMessage(), NotificationType.ERROR);
        }
    }
    
    /**
     * 将DAO实体转换为模型对象
     */
    private ServerConfigDTO convertToModel(ServerConfig daoEntity) {
        ServerConfigDTO model = new ServerConfigDTO();
        
        model.setId(daoEntity.getId());
        model.setName(daoEntity.getName());
        model.setHost(daoEntity.getHost());
        model.setPort(daoEntity.getPort());
        model.setUsername(daoEntity.getUsername());
        model.setPassword(daoEntity.getPassword());
        model.setAppDirectory(daoEntity.getAppDirectory());
        model.setBackupDirectory(daoEntity.getBackupDirectory());
        model.setBinDirectory(daoEntity.getBinDirectory());
        model.setStopScript(daoEntity.getStopScript());
        model.setStartScript(daoEntity.getStartScript());
        model.setDescription(daoEntity.getDescription());
        model.setEnabled(daoEntity.getEnabled());
        
        return model;
    }
    
    /**
     * 将模型对象转换为DAO实体
     */
    private io.github.lemostic.toolsuite.modules.devops.deploy.dao.ServerConfig convertToDao(
            ServerConfigDTO model) {
        io.github.lemostic.toolsuite.modules.devops.deploy.dao.ServerConfig daoEntity = 
            new io.github.lemostic.toolsuite.modules.devops.deploy.dao.ServerConfig();
        
        if (model.getId() != null) {
            daoEntity.setId(model.getId());
        }
        daoEntity.setName(model.getName());
        daoEntity.setHost(model.getHost());
        daoEntity.setPort(model.getPort());
        daoEntity.setUsername(model.getUsername());
        daoEntity.setPassword(model.getPassword());
        daoEntity.setAppDirectory(model.getAppDirectory());
        daoEntity.setBackupDirectory(model.getBackupDirectory());
        daoEntity.setBinDirectory(model.getBinDirectory());
        daoEntity.setStopScript(model.getStopScript());
        daoEntity.setStartScript(model.getStartScript());
        daoEntity.setDescription(model.getDescription());
        daoEntity.setEnabled(model.getEnabled());
        
        return daoEntity;
    }
    
    private void saveServerConfigs() {
        updateDeployButton();
    }
    
    private void saveCustomSteps(String steps) {
        // 解析并保存自定义步骤
        try {
            // 这里可以解析文本输入并转换为CustomStepConfig对象
            // 目前简化处理
            showNotification("保存成功", "自定义步骤已保存", NotificationType.SUCCESS);
        } catch (Exception e) {
            showNotification("保存失败", "解析步骤配置时出错: " + e.getMessage(), NotificationType.ERROR);
        }
    }
    
    private void showSettingsDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("设置");
        alert.setHeaderText("部署工具设置");
        alert.setContentText("这里可以配置全局部署设置、默认选项等");
        alert.showAndWait();
    }
    
    private void showHelp() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("使用说明");
        alert.setHeaderText("应用部署工具 - 使用指南");
        alert.setContentText(
            "1. 添加服务器：点击上方「添加服务器」按钮配置目标服务器\n" +
            "2. 选择服务器：在服务器列表中勾选要部署的目标\n" +
            "3. 选择程序包：拖放文件或点击「选择文件」按钮\n" +
            "4. 配置选项：在右侧选项卡中设置备份和上传选项\n" +
            "5. 开始部署：点击「开始部署」按钮执行部署\n\n" +
            "部署流程：连接服务器 → 停止服务 → 备份 → 上传 → 启动服务"
        );
        alert.showAndWait();
    }
    
    private void showNotification(String title, String content, NotificationType type) {
        // 简单的通知实现
        Alert alert = new Alert(type.getAlertType());
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
    
    // 通知类型枚举
    private enum NotificationType {
        SUCCESS(Alert.AlertType.INFORMATION),
        ERROR(Alert.AlertType.ERROR),
        WARNING(Alert.AlertType.WARNING),
        INFO(Alert.AlertType.INFORMATION);
        
        private final Alert.AlertType alertType;
        
        NotificationType(Alert.AlertType alertType) {
            this.alertType = alertType;
        }
        
        public Alert.AlertType getAlertType() {
            return alertType;
        }
    }
}