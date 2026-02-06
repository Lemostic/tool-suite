package io.github.lemostic.toolsuite.modules.devops.deploy.view;

import io.github.lemostic.toolsuite.modules.devops.deploy.model.*;
import io.github.lemostic.toolsuite.modules.devops.deploy.service.*;
import io.github.lemostic.toolsuite.modules.devops.deploy.task.*;
import io.github.lemostic.toolsuite.modules.devops.deploy.view.components.*;
import io.github.lemostic.toolsuite.modules.devops.deploy.view.dialogs.*;
import io.github.lemostic.toolsuite.util.UIUtil;
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
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign.MaterialDesign;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

public class DeployView extends BorderPane {
    
    private static final Logger logger = LoggerFactory.getLogger(DeployView.class);
    
    // 服务层
    private final SshConnectionPool connectionPool = new SshConnectionPool();
    private final SshCommandService sshService = new SshCommandService(connectionPool);
    private final SftpTransferService sftpService = new SftpTransferService(connectionPool);
    private final DeployDataService dataService = new DeployDataService();
    
    // 数据
    private final ObservableList<ServerConfig> serverConfigs = FXCollections.observableArrayList();
    private File selectedPackage;
    
    // UI 组件
    private TableView<ServerConfig> serverTable;
    private Label packageLabel;
    private TabPane logTabPane;
    private BackupOptionsPanel backupOptionsPanel;
    private UploadOptionsPanel uploadOptionsPanel;
    private Button deployButton;
    private Button selectPackageButton;
    
    // 部署任务跟踪
    private final Map<String, TaskPipeline> runningPipelines = new HashMap<>();
    private final Map<String, LogTab> logTabs = new HashMap<>();
    
    public DeployView() {
        initializeUI();
        loadServerConfigs();
        setupDragAndDrop();
    }
    
    private void initializeUI() {
        setStyle("-fx-background-color: #f5f5f5;");
        
        // 顶部工具栏
        setTop(createToolbar());
        
        // 主内容区 - 分割面板
        SplitPane splitPane = new SplitPane();
        splitPane.setOrientation(javafx.geometry.Orientation.VERTICAL);
        splitPane.setDividerPositions(0.5);
        
        // 上半部分：服务器和包选择
        splitPane.getItems().add(createTopSection());
        
        // 下半部分：日志Tab
        logTabPane = new TabPane();
        logTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        splitPane.getItems().add(logTabPane);
        
        setCenter(splitPane);
        
        // 底部状态栏
        setBottom(createStatusBar());
    }
    
    private Node createToolbar() {
        ToolBar toolbar = new ToolBar();
        toolbar.setStyle("-fx-background-color: linear-gradient(to bottom, #ffffff, #e8e8e8); " +
                        "-fx-border-color: #d0d0d0; -fx-border-width: 0 0 1 0;");
        
        Button addServerBtn = new Button("添加服务器", new FontIcon(MaterialDesign.MDI_SERVER_PLUS));
        addServerBtn.setOnAction(e -> showAddServerDialog());
        
        Button editServerBtn = new Button("编辑", new FontIcon(MaterialDesign.MDI_PENCIL));
        editServerBtn.setOnAction(e -> showEditServerDialog());
        
        Button removeServerBtn = new Button("删除", new FontIcon(MaterialDesign.MDI_DELETE));
        removeServerBtn.setOnAction(e -> removeSelectedServer());
        
        Button testConnBtn = new Button("测试连接", new FontIcon(MaterialDesign.MDI_LAN_CONNECT));
        testConnBtn.setOnAction(e -> testSelectedConnection());
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Button helpBtn = new Button("使用说明", new FontIcon(MaterialDesign.MDI_HELP_CIRCLE));
        helpBtn.setOnAction(e -> showHelp());
        
        toolbar.getItems().addAll(addServerBtn, editServerBtn, removeServerBtn, 
            new Separator(), testConnBtn, spacer, helpBtn);
        
        return toolbar;
    }
    
    private Node createTopSection() {
        HBox topSection = new HBox(10);
        topSection.setPadding(new Insets(10));
        
        // 左侧：服务器列表
        VBox serverBox = new VBox(10);
        serverBox.setPrefWidth(400);
        
        Label serverTitle = new Label("服务器列表");
        serverTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        serverTable = new TableView<>();
        serverTable.setItems(serverConfigs);
        serverTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        // 选择列
        TableColumn<ServerConfig, Boolean> selectCol = new TableColumn<>("部署");
        selectCol.setCellValueFactory(cellData -> cellData.getValue().enabledProperty());
        selectCol.setCellFactory(CheckBoxTableCell.forTableColumn(selectCol));
        selectCol.setEditable(true);
        selectCol.setMaxWidth(60);
        
        // 名称列
        TableColumn<ServerConfig, String> nameCol = new TableColumn<>("名称");
        nameCol.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        nameCol.setPrefWidth(120);
        
        // 主机列
        TableColumn<ServerConfig, String> hostCol = new TableColumn<>("主机");
        hostCol.setCellValueFactory(cellData -> cellData.getValue().hostProperty());
        hostCol.setPrefWidth(150);
        
        serverTable.getColumns().addAll(selectCol, nameCol, hostCol);
        serverTable.setEditable(true);
        serverTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        
        UIUtil.setEmptyDataPlaceholder(serverTable, "暂无服务器配置，请点击上方「添加服务器」按钮");
        
        VBox.setVgrow(serverTable, Priority.ALWAYS);
        serverBox.getChildren().addAll(serverTitle, serverTable);
        
        // 中间：程序包选择
        VBox packageBox = createPackageSelectionCard();
        
        // 右侧：选项面板
        VBox optionsBox = createOptionsCard();
        
        HBox.setHgrow(serverBox, Priority.ALWAYS);
        HBox.setHgrow(packageBox, Priority.NEVER);
        HBox.setHgrow(optionsBox, Priority.NEVER);
        
        topSection.getChildren().addAll(serverBox, packageBox, optionsBox);
        
        return topSection;
    }
    
    private VBox createPackageSelectionCard() {
        VBox card = new VBox(15);
        card.setStyle("-fx-background-color: white; " +
                     "-fx-border-color: #e0e0e0; " +
                     "-fx-border-radius: 5; " +
                     "-fx-background-radius: 5; " +
                     "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);");
        card.setPadding(new Insets(15));
        card.setPrefWidth(250);
        card.setAlignment(Pos.TOP_CENTER);
        
        // 标题
        HBox titleBox = new HBox(8);
        titleBox.setAlignment(Pos.CENTER_LEFT);
        FontIcon titleIcon = new FontIcon(MaterialDesign.MDI_PACKAGE_VARIANT);
        titleIcon.setIconSize(20);
        titleIcon.setStyle("-fx-icon-color: #2196F3;");
        Label titleLabel = new Label("程序包");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #333;");
        titleBox.getChildren().addAll(titleIcon, titleLabel);
        
        Separator separator = new Separator();
        
        // 拖放区域
        VBox dropZone = new VBox(10);
        dropZone.setAlignment(Pos.CENTER);
        dropZone.setStyle("-fx-background-color: #f8f9fa; " +
                         "-fx-border-color: #dee2e6; " +
                         "-fx-border-style: dashed; " +
                         "-fx-border-width: 2; " +
                         "-fx-border-radius: 5; " +
                         "-fx-background-radius: 5;");
        dropZone.setPadding(new Insets(30));
        
        FontIcon dropIcon = new FontIcon(MaterialDesign.MDI_CLOUD_UPLOAD);
        dropIcon.setIconSize(48);
        dropIcon.setStyle("-fx-icon-color: #6c757d;");
        
        Label dropLabel = new Label("拖放文件到此处");
        dropLabel.setStyle("-fx-text-fill: #6c757d;");
        
        Label orLabel = new Label("或");
        orLabel.setStyle("-fx-text-fill: #adb5bd;");
        
        selectPackageButton = new Button("选择文件");
        selectPackageButton.setOnAction(e -> selectPackageFile());
        
        dropZone.getChildren().addAll(dropIcon, dropLabel, orLabel, selectPackageButton);
        
        // 已选文件显示
        packageLabel = new Label("未选择文件");
        packageLabel.setWrapText(true);
        packageLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");
        packageLabel.setAlignment(Pos.CENTER);
        
        // 部署按钮
        deployButton = new Button("开始部署", new FontIcon(MaterialDesign.MDI_ROCKET));
        deployButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; " +
                             "-fx-font-size: 14px; -fx-font-weight: bold;");
        deployButton.setPrefWidth(Double.MAX_VALUE);
        deployButton.setDisable(true);
        deployButton.setOnAction(e -> startDeployment());
        
        card.getChildren().addAll(titleBox, separator, dropZone, packageLabel, deployButton);
        
        return card;
    }
    
    private VBox createOptionsCard() {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: white; " +
                     "-fx-border-color: #e0e0e0; " +
                     "-fx-border-radius: 5; " +
                     "-fx-background-radius: 5; " +
                     "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);");
        card.setPadding(new Insets(15));
        card.setPrefWidth(300);
        
        // 标题
        HBox titleBox = new HBox(8);
        titleBox.setAlignment(Pos.CENTER_LEFT);
        FontIcon titleIcon = new FontIcon(MaterialDesign.MDI_SETTINGS);
        titleIcon.setIconSize(20);
        titleIcon.setStyle("-fx-icon-color: #2196F3;");
        Label titleLabel = new Label("部署选项");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #333;");
        titleBox.getChildren().addAll(titleIcon, titleLabel);
        
        Separator separator = new Separator();
        
        // 选项面板 - 使用TabPane
        TabPane optionsTabPane = new TabPane();
        optionsTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        optionsTabPane.setStyle("-fx-tab-min-width: 80;");
        
        // 备份选项Tab
        backupOptionsPanel = new BackupOptionsPanel();
        Tab backupTab = new Tab("备份", backupOptionsPanel);
        
        // 上传选项Tab
        uploadOptionsPanel = new UploadOptionsPanel();
        Tab uploadTab = new Tab("上传", uploadOptionsPanel);
        
        optionsTabPane.getTabs().addAll(backupTab, uploadTab);
        VBox.setVgrow(optionsTabPane, Priority.ALWAYS);
        
        card.getChildren().addAll(titleBox, separator, optionsTabPane);
        
        return card;
    }
    
    private Node createStatusBar() {
        HBox statusBar = new HBox(10);
        statusBar.setPadding(new Insets(5, 10, 5, 10));
        statusBar.setStyle("-fx-background-color: linear-gradient(to top, #ffffff, #f0f0f0); " +
                          "-fx-border-color: #d0d0d0; -fx-border-width: 1 0 0 0;");
        statusBar.setAlignment(Pos.CENTER_LEFT);
        
        Label statusLabel = new Label("就绪");
        statusLabel.setStyle("-fx-text-fill: #666;");
        
        statusBar.getChildren().add(statusLabel);
        
        return statusBar;
    }
    
    private void setupDragAndDrop() {
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
        packageLabel.setStyle("-fx-text-fill: #333; -fx-font-weight: bold;");
        
        updateDeployButton();
    }
    
    private void updateDeployButton() {
        boolean hasServer = serverConfigs.stream().anyMatch(ServerConfig::getEnabled);
        boolean hasPackage = selectedPackage != null;
        deployButton.setDisable(!(hasServer && hasPackage));
    }
    
    private void showAddServerDialog() {
        ServerConfigDialog dialog = new ServerConfigDialog();
        dialog.showAndWait().ifPresent(config -> {
            try {
                dataService.saveServer(config);
                serverConfigs.add(config);
                saveServerConfigs();
            } catch (Exception e) {
                logger.error("保存服务器配置失败", e);
                showAlert("错误", "保存失败: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }
    
    private void showEditServerDialog() {
        ServerConfig selected = serverTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("提示", "请先选择一个服务器", Alert.AlertType.INFORMATION);
            return;
        }
        
        ServerConfigDialog dialog = new ServerConfigDialog(selected);
        dialog.showAndWait().ifPresent(config -> {
            try {
                dataService.saveServer(config);
                int index = serverConfigs.indexOf(selected);
                serverConfigs.set(index, config);
                saveServerConfigs();
            } catch (Exception e) {
                logger.error("更新服务器配置失败", e);
                showAlert("错误", "更新失败: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }
    
    private void removeSelectedServer() {
        ServerConfig selected = serverTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("提示", "请先选择一个服务器", Alert.AlertType.INFORMATION);
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
                        dataService.deleteServer(selected.getId());
                    }
                    serverConfigs.remove(selected);
                    saveServerConfigs();
                } catch (Exception e) {
                    logger.error("删除服务器配置失败", e);
                    showAlert("错误", "删除失败: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }
    
    private void testSelectedConnection() {
        ServerConfig selected = serverTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("提示", "请先选择一个服务器", Alert.AlertType.INFORMATION);
            return;
        }
        
        new Thread(() -> {
            boolean success = sshService.testConnection(selected);
            Platform.runLater(() -> {
                if (success) {
                    showAlert("连接测试", "连接成功！", Alert.AlertType.INFORMATION);
                } else {
                    showAlert("连接测试", "连接失败，请检查配置", Alert.AlertType.ERROR);
                }
            });
        }).start();
    }
    
    private void startDeployment() {
        List<ServerConfig> selectedServers = serverConfigs.stream()
            .filter(ServerConfig::getEnabled)
            .toList();
        
        if (selectedServers.isEmpty()) {
            showAlert("错误", "请至少选择一个服务器", Alert.AlertType.ERROR);
            return;
        }
        
        if (selectedPackage == null) {
            showAlert("错误", "请选择程序包", Alert.AlertType.ERROR);
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
    
    private void executeDeployment(List<ServerConfig> servers) {
        BackupOptions backupOptions = backupOptionsPanel.getOptions();
        UploadOptions uploadOptions = uploadOptionsPanel.getOptions();
        
        // 清空之前的日志Tab
        logTabPane.getTabs().clear();
        logTabs.clear();
        runningPipelines.clear();
        
        for (ServerConfig server : servers) {
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
                    });
                });
            
            // 设置取消回调
            logTab.setOnCancel(() -> pipeline.cancel());
            
            // 添加部署步骤
            pipeline.getContext().setAttribute("sshService", sshService);
            
            pipeline.addStep(DeploySteps.createConnectStep(sshService, server))
                   .addStep(DeploySteps.createStopServiceStep(sshService, server))
                   .addStep(DeploySteps.createBackupStep(sshService, server, backupOptions))
                   .addStep(DeploySteps.createUploadStep(sftpService, server, 
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
    
    private void loadServerConfigs() {
        try {
            List<ServerConfig> servers = dataService.loadAllServers();
            serverConfigs.addAll(servers);
            logger.info("加载了 {} 个服务器配置", servers.size());
        } catch (Exception e) {
            logger.error("加载服务器配置失败", e);
            showAlert("错误", "加载服务器配置失败: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
    
    private void saveServerConfigs() {
        updateDeployButton();
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
    
    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
