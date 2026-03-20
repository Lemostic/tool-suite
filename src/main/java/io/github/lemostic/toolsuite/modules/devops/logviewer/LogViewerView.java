package io.github.lemostic.toolsuite.modules.devops.logviewer;

import io.github.lemostic.toolsuite.modules.devops.logviewer.model.LogDirectory;
import io.github.lemostic.toolsuite.modules.devops.logviewer.model.LogFile;
import io.github.lemostic.toolsuite.modules.devops.logviewer.model.ServerConfig;
import io.github.lemostic.toolsuite.modules.devops.logviewer.service.LogViewerService;
import io.github.lemostic.toolsuite.util.ResourceLoader;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign.MaterialDesign;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * 日志查看器主视图
 * 支持服务器管理、多目录Tab切换、日志文件列表和日志内容展示
 */
public class LogViewerView extends BorderPane {
    
    private final LogViewerService service = new LogViewerService();
    private final Preferences prefs = Preferences.userNodeForPackage(LogViewerView.class);
    
    // 服务器管理
    private ComboBox<ServerConfig> serverComboBox;
    private ObservableList<ServerConfig> serverList = FXCollections.observableArrayList();
    private Map<String, ServerConfig> serverMap = new HashMap<>();
    
    // 目录Tab
    private TabPane directoryTabPane;
    private Map<String, TabContent> tabContentMap = new HashMap<>();
    
    // 状态栏
    private Label statusLabel;
    private ProgressBar progressBar;
    private Button connectBtn;
    
    // 行数选择
    private static final String[] LINE_OPTIONS = {"尾200行", "尾500行", "尾1000行", "头200行", "头500行", "头1000行"};
    private static final int[] LINE_VALUES = {-200, -500, -1000, 200, 500, 1000};
    
    public LogViewerView() {
        initializeUI();
        bindProperties();
        loadSavedServers();
    }
    
    private void initializeUI() {
        setStyle("-fx-background-color: #f5f5f5;");
        
        // 顶部工具栏
        setTop(createToolbar());
        
        // 中心内容区
        setCenter(createMainContent());
        
        // 底部状态栏
        setBottom(createStatusBar());
    }
    
    private Node createToolbar() {
        ToolBar toolbar = new ToolBar();
        toolbar.setStyle("-fx-background-color: linear-gradient(to bottom, #ffffff, #e8e8e8); " +
                        "-fx-border-color: #d0d0d0; -fx-border-width: 0 0 1 0;");
        toolbar.setPadding(new Insets(8, 15, 8, 15));
        
        // 服务器选择区域
        HBox serverBox = new HBox(8);
        serverBox.setAlignment(Pos.CENTER_LEFT);
        
        Label serverLabel = new Label("服务器:");
        serverLabel.setStyle("-fx-font-weight: bold;");
        
        serverComboBox = new ComboBox<>(serverList);
        serverComboBox.setPrefWidth(250);
        serverComboBox.setPromptText("选择服务器...");
        serverComboBox.setCellFactory(lv -> new ListCell<ServerConfig>() {
            @Override
            protected void updateItem(ServerConfig item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getDisplayName());
            }
        });
        serverComboBox.setButtonCell(new ListCell<ServerConfig>() {
            @Override
            protected void updateItem(ServerConfig item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getDisplayName());
            }
        });
        serverComboBox.setOnAction(e -> onServerSelected());
        
        // 连接按钮
        connectBtn = new Button("连接", new FontIcon(MaterialDesign.MDI_LAN_CONNECT));
        connectBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
        connectBtn.setOnAction(e -> connectToServer());
        
        // 服务器管理按钮
        Button manageBtn = new Button("管理", new FontIcon(MaterialDesign.MDI_SERVER));
        manageBtn.setStyle("-fx-background-color: #607D8B; -fx-text-fill: white;");
        manageBtn.setOnAction(e -> showServerManager());
        
        serverBox.getChildren().addAll(serverLabel, serverComboBox, connectBtn, manageBtn);
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        // 帮助按钮
        Button helpBtn = new Button("帮助", new FontIcon(MaterialDesign.MDI_HELP_CIRCLE));
        helpBtn.setOnAction(e -> showHelp());
        
        Button aboutBtn = new Button("关于", new FontIcon(MaterialDesign.MDI_INFORMATION));
        aboutBtn.setOnAction(e -> showAbout());
        
        toolbar.getItems().addAll(serverBox, spacer, helpBtn, aboutBtn);
        
        return toolbar;
    }
    
    private Node createMainContent() {
        VBox mainContent = new VBox(10);
        mainContent.setPadding(new Insets(15));
        
        // 目录TabPane
        directoryTabPane = new TabPane();
        directoryTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        directoryTabPane.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(directoryTabPane, Priority.ALWAYS);
        
        // 默认提示
        Tab defaultTab = new Tab("日志目录");
        defaultTab.setContent(createEmptyContent("请先选择并连接服务器"));
        directoryTabPane.getTabs().add(defaultTab);
        
        mainContent.getChildren().add(directoryTabPane);
        
        return mainContent;
    }
    
    private Node createEmptyContent(String message) {
        VBox emptyBox = new VBox(15);
        emptyBox.setAlignment(Pos.CENTER);
        emptyBox.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-radius: 5;");
        
        FontIcon icon = new FontIcon(MaterialDesign.MDI_FOLDER_OUTLINE);
        icon.setIconSize(64);
        icon.setStyle("-fx-icon-color: #bbb;");
        
        Label label = new Label(message);
        label.setStyle("-fx-font-size: 16px; -fx-text-fill: #888;");
        
        emptyBox.getChildren().addAll(icon, label);
        return emptyBox;
    }
    
    /**
     * 创建目录Tab的内容
     */
    private Node createDirectoryTabContent(LogDirectory directory) {
        SplitPane splitPane = new SplitPane();
        splitPane.setOrientation(Orientation.VERTICAL);
        splitPane.setDividerPositions(0.35);
        
        // 上半部分：文件列表
        Node fileListPanel = createFileListPanel(directory);
        
        // 下半部分：日志内容
        Node logContentPanel = createLogContentPanel(directory);
        
        splitPane.getItems().addAll(fileListPanel, logContentPanel);
        
        return splitPane;
    }
    
    private Node createFileListPanel(LogDirectory directory) {
        VBox panel = new VBox(8);
        panel.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-radius: 5;");
        panel.setPadding(new Insets(10));
        
        // 标题栏
        HBox titleBox = new HBox(8);
        titleBox.setAlignment(Pos.CENTER_LEFT);
        
        FontIcon icon = new FontIcon(MaterialDesign.MDI_FILE_DOCUMENT);
        icon.setIconSize(18);
        icon.setStyle("-fx-icon-color: #2196F3;");
        
        Label titleLabel = new Label("日志文件列表");
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #333;");
        
        Label pathLabel = new Label(directory.getPath());
        pathLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666; -fx-background-color: #e3f2fd; " +
                          "-fx-padding: 2 8 2 8; -fx-background-radius: 10;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Button refreshBtn = new Button("刷新", new FontIcon(MaterialDesign.MDI_REFRESH));
        refreshBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
        refreshBtn.setOnAction(e -> refreshFileList(directory));
        
        titleBox.getChildren().addAll(icon, titleLabel, pathLabel, spacer, refreshBtn);
        
        // 文件列表表格
        TableView<LogFile> fileTable = new TableView<>();
        fileTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        TableColumn<LogFile, String> nameCol = new TableColumn<>("文件名");
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        nameCol.setPrefWidth(300);
        
        TableColumn<LogFile, String> sizeCol = new TableColumn<>("大小");
        sizeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFormattedSize()));
        sizeCol.setPrefWidth(100);
        
        TableColumn<LogFile, String> timeCol = new TableColumn<>("修改时间");
        timeCol.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().getLastModified().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        ));
        timeCol.setPrefWidth(150);
        
        fileTable.getColumns().addAll(nameCol, sizeCol, timeCol);
        
        // 存储引用以便刷新
        TabContent tabContent = tabContentMap.get(directory.getId());
        if (tabContent != null) {
            tabContent.fileTable = fileTable;
        }
        
        // 双击打开文件
        fileTable.setRowFactory(tv -> {
            TableRow<LogFile> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    loadLogFile(row.getItem(), directory);
                }
            });
            return row;
        });
        
        VBox.setVgrow(fileTable, Priority.ALWAYS);
        panel.getChildren().addAll(titleBox, new Separator(), fileTable);
        
        return panel;
    }
    
    private Node createLogContentPanel(LogDirectory directory) {
        VBox panel = new VBox(8);
        panel.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-radius: 5;");
        panel.setPadding(new Insets(10));
        
        // 工具栏
        HBox toolbar = new HBox(10);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        
        FontIcon icon = new FontIcon(MaterialDesign.MDI_FILE_DOCUMENT);
        icon.setIconSize(18);
        icon.setStyle("-fx-icon-color: #4CAF50;");
        
        Label titleLabel = new Label("日志内容");
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #333;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        // 行数选择
        ComboBox<String> linesCombo = new ComboBox<>();
        linesCombo.getItems().addAll(LINE_OPTIONS);
        linesCombo.getSelectionModel().select(0); // 默认尾200行
        linesCombo.setPrefWidth(100);
        
        Label linesLabel = new Label("加载行数:");
        
        // 搜索框
        TextField searchField = new TextField();
        searchField.setPromptText("搜索日志内容...");
        searchField.setPrefWidth(200);
        
        Button searchBtn = new Button("搜索", new FontIcon(MaterialDesign.MDI_MAGNIFY));
        searchBtn.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white;");
        searchBtn.setOnAction(e -> searchInLog(searchField.getText(), directory));
        
        searchField.setOnAction(e -> searchInLog(searchField.getText(), directory));
        
        // 复制按钮
        Button copyBtn = new Button("复制", new FontIcon(MaterialDesign.MDI_CONTENT_COPY));
        copyBtn.setStyle("-fx-background-color: #9C27B0; -fx-text-fill: white;");
        copyBtn.setOnAction(e -> copyLogContent(directory));
        
        // 下载按钮
        Button downloadBtn = new Button("下载", new FontIcon(MaterialDesign.MDI_DOWNLOAD));
        downloadBtn.setStyle("-fx-background-color: #00BCD4; -fx-text-fill: white;");
        downloadBtn.setOnAction(e -> downloadLogFile(directory));
        
        // 清空按钮
        Button clearBtn = new Button("清空", new FontIcon(MaterialDesign.MDI_DELETE));
        clearBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
        clearBtn.setOnAction(e -> clearLogContent(directory));
        
        // 自动滚动选项
        CheckBox autoScrollCheck = new CheckBox("自动滚动");
        autoScrollCheck.setSelected(true);
        autoScrollCheck.setTooltip(new Tooltip("加载日志后自动滚动到末尾"));
        
        // 显示行号选项
        CheckBox showLineNumbersCheck = new CheckBox("显示行号");
        showLineNumbersCheck.setSelected(true);
        showLineNumbersCheck.setTooltip(new Tooltip("在日志行前显示行号"));
        
        toolbar.getChildren().addAll(icon, titleLabel, spacer, linesLabel, linesCombo, 
                                     searchField, searchBtn, copyBtn, downloadBtn, clearBtn, 
                                     autoScrollCheck, showLineNumbersCheck);
        
        // 日志内容区域 - 使用 ListView 支持语法高亮
        ListView<String> logListView = new ListView<>();
        logListView.setStyle("-fx-background-color: #1e1e1e; " +
                            "-fx-control-inner-background: #1e1e1e; " +
                            "-fx-control-inner-background-alt: #252526;");
        
        // 创建自定义单元格工厂，支持语法高亮和行号
        logListView.setCellFactory(lv -> new LogLineCell(showLineNumbersCheck));
        
        // 存储引用
        TabContent tabContent = tabContentMap.get(directory.getId());
        if (tabContent != null) {
            tabContent.logListView = logListView;
            tabContent.logLines = FXCollections.observableArrayList();
            logListView.setItems(tabContent.logLines);
            tabContent.linesCombo = linesCombo;
            tabContent.autoScrollCheck = autoScrollCheck;
            tabContent.showLineNumbersCheck = showLineNumbersCheck;
        }
        
        VBox.setVgrow(logListView, Priority.ALWAYS);
        panel.getChildren().addAll(toolbar, logListView);
        
        return panel;
    }
    
    private Node createStatusBar() {
        VBox statusBox = new VBox(5);
        statusBox.setPadding(new Insets(10));
        statusBox.setStyle("-fx-background-color: linear-gradient(to top, #ffffff, #f0f0f0); " +
                          "-fx-border-color: #d0d0d0; -fx-border-width: 1 0 0 0;");
        
        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(Double.MAX_VALUE);
        progressBar.setVisible(false);
        
        HBox statusLine = new HBox(10);
        statusLine.setAlignment(Pos.CENTER_LEFT);
        
        FontIcon statusIcon = new FontIcon(MaterialDesign.MDI_INFORMATION_OUTLINE);
        statusIcon.setIconSize(16);
        statusIcon.setStyle("-fx-icon-color: #666;");
        
        statusLabel = new Label("就绪");
        statusLabel.setStyle("-fx-text-fill: #666;");
        
        statusLine.getChildren().addAll(statusIcon, statusLabel);
        
        statusBox.getChildren().addAll(progressBar, statusLine);
        
        return statusBox;
    }
    
    private void bindProperties() {
        statusLabel.textProperty().bind(service.statusMessageProperty());
        
        service.connectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                connectBtn.setText("断开");
                connectBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-weight: bold;");
                connectBtn.setGraphic(new FontIcon(MaterialDesign.MDI_LAN_DISCONNECT));
            } else {
                connectBtn.setText("连接");
                connectBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
                connectBtn.setGraphic(new FontIcon(MaterialDesign.MDI_LAN_CONNECT));
            }
        });
    }
    
    private void onServerSelected() {
        ServerConfig server = serverComboBox.getValue();
        if (server != null && service.isConnected()) {
            // 如果已连接，先断开
            service.disconnect();
            clearTabs();
        }
    }
    
    private void connectToServer() {
        if (service.isConnected()) {
            service.disconnect();
            clearTabs();
            return;
        }
        
        ServerConfig server = serverComboBox.getValue();
        if (server == null) {
            showAlert("提示", "请先选择一个服务器", Alert.AlertType.WARNING);
            return;
        }
        
        progressBar.setVisible(true);
        
        service.connectAsync(server)
            .thenAccept(success -> Platform.runLater(() -> {
                if (success) {
                    createDirectoryTabs(server);
                }
                progressBar.setVisible(false);
            }))
            .exceptionally(throwable -> {
                Platform.runLater(() -> {
                    showAlert("连接失败", throwable.getMessage(), Alert.AlertType.ERROR);
                    progressBar.setVisible(false);
                });
                return null;
            });
    }
    
    private void createDirectoryTabs(ServerConfig server) {
        directoryTabPane.getTabs().clear();
        tabContentMap.clear();
        
        if (server.getLogDirectories() == null || server.getLogDirectories().isEmpty()) {
            Tab tab = new Tab("无目录");
            tab.setContent(createEmptyContent("该服务器未配置日志目录"));
            directoryTabPane.getTabs().add(tab);
            return;
        }
        
        for (LogDirectory directory : server.getLogDirectories()) {
            TabContent content = new TabContent();
            tabContentMap.put(directory.getId(), content);
            
            Tab tab = new Tab(directory.getDisplayName());
            tab.setContent(createDirectoryTabContent(directory));
            tab.setUserData(directory);
            
            // 设置Tab图标
            FontIcon tabIcon = new FontIcon(MaterialDesign.MDI_FOLDER);
            tabIcon.setIconSize(16);
            tab.setGraphic(tabIcon);
            
            directoryTabPane.getTabs().add(tab);
            
            // 自动加载文件列表
            refreshFileList(directory);
        }
    }
    
    private void clearTabs() {
        directoryTabPane.getTabs().clear();
        tabContentMap.clear();
        
        Tab defaultTab = new Tab("日志目录");
        defaultTab.setContent(createEmptyContent("请先选择并连接服务器"));
        directoryTabPane.getTabs().add(defaultTab);
    }
    
    private void refreshFileList(LogDirectory directory) {
        TabContent content = tabContentMap.get(directory.getId());
        if (content == null || content.fileTable == null) return;
        
        progressBar.setVisible(true);
        
        service.listLogFilesAsync(directory)
            .thenAccept(files -> Platform.runLater(() -> {
                content.fileTable.getItems().clear();
                content.fileTable.getItems().addAll(files);
                progressBar.setVisible(false);
            }))
            .exceptionally(throwable -> {
                Platform.runLater(() -> {
                    showAlert("读取失败", throwable.getMessage(), Alert.AlertType.ERROR);
                    progressBar.setVisible(false);
                });
                return null;
            });
    }
    
    private void loadLogFile(LogFile logFile, LogDirectory directory) {
        TabContent content = tabContentMap.get(directory.getId());
        if (content == null || content.logListView == null) return;
        
        int linesIndex = content.linesCombo.getSelectionModel().getSelectedIndex();
        int lines = linesIndex >= 0 ? LINE_VALUES[linesIndex] : -200;
        
        content.currentFile = logFile;
        content.logLines.clear();
        
        progressBar.setVisible(true);
        
        service.readLogFileAsync(logFile, lines)
            .thenAccept(logContent -> Platform.runLater(() -> {
                // 将日志内容按行分割
                String[] linesArray = logContent.split("\\r?\\n");
                content.logLines.addAll(Arrays.asList(linesArray));
                
                // 根据自动滚动选项决定是否滚动到底部
                if (content.autoScrollCheck != null && content.autoScrollCheck.isSelected() && lines < 0) {
                    // 如果是加载尾部行数，滚动到底部
                    content.logListView.scrollTo(content.logLines.size() - 1);
                } else {
                    content.logListView.scrollTo(0);
                }
                progressBar.setVisible(false);
            }))
            .exceptionally(throwable -> {
                Platform.runLater(() -> {
                    content.logLines.add("读取失败: " + throwable.getMessage());
                    progressBar.setVisible(false);
                });
                return null;
            });
    }
    
    /**
     * 下载日志文件
     */
    private void downloadLogFile(LogDirectory directory) {
        TabContent content = tabContentMap.get(directory.getId());
        if (content == null || content.currentFile == null) {
            showAlert("提示", "请先选择一个日志文件", Alert.AlertType.WARNING);
            return;
        }
        
        LogFile logFile = content.currentFile;
        
        // 创建文件选择对话框
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("保存日志文件");
        fileChooser.setInitialFileName(logFile.getName());
        fileChooser.getExtensionFilters().add(
            new javafx.stage.FileChooser.ExtensionFilter("日志文件", "*.log", "*.txt")
        );
        fileChooser.getExtensionFilters().add(
            new javafx.stage.FileChooser.ExtensionFilter("所有文件", "*.*")
        );
        
        java.io.File saveFile = fileChooser.showSaveDialog(getScene().getWindow());
        if (saveFile != null) {
            progressBar.setVisible(true);
            
            // 读取完整文件内容并保存
            service.readLogFileAsync(logFile, Integer.MAX_VALUE)
                .thenAccept(fileContent -> Platform.runLater(() -> {
                    try {
                        java.nio.file.Files.writeString(saveFile.toPath(), fileContent, 
                            java.nio.charset.StandardCharsets.UTF_8);
                        statusLabel.setText("文件已保存: " + saveFile.getName());
                    } catch (Exception e) {
                        showAlert("保存失败", "保存文件时出错: " + e.getMessage(), Alert.AlertType.ERROR);
                    }
                    progressBar.setVisible(false);
                }))
                .exceptionally(throwable -> {
                    Platform.runLater(() -> {
                        showAlert("下载失败", throwable.getMessage(), Alert.AlertType.ERROR);
                        progressBar.setVisible(false);
                    });
                    return null;
                });
        }
    }
    
    private void searchInLog(String keyword, LogDirectory directory) {
        TabContent content = tabContentMap.get(directory.getId());
        if (content == null || content.logListView == null || keyword == null || keyword.isEmpty()) return;
        
        int currentSelection = content.logListView.getSelectionModel().getSelectedIndex();
        int startIndex = currentSelection + 1;
        
        // 从当前选中位置开始搜索
        for (int i = startIndex; i < content.logLines.size(); i++) {
            String line = content.logLines.get(i);
            if (line.toLowerCase().contains(keyword.toLowerCase())) {
                content.logListView.getSelectionModel().select(i);
                content.logListView.scrollTo(i);
                return;
            }
        }
        
        // 从头开始搜索
        for (int i = 0; i < startIndex; i++) {
            String line = content.logLines.get(i);
            if (line.toLowerCase().contains(keyword.toLowerCase())) {
                content.logListView.getSelectionModel().select(i);
                content.logListView.scrollTo(i);
                return;
            }
        }
        
        showAlert("搜索", "未找到 \"" + keyword + "\"", Alert.AlertType.INFORMATION);
    }
    
    private void copyLogContent(LogDirectory directory) {
        TabContent content = tabContentMap.get(directory.getId());
        if (content == null || content.logListView == null) return;
        
        // 获取选中的行
        ObservableList<Integer> selectedIndices = content.logListView.getSelectionModel().getSelectedIndices();
        String text;
        
        if (selectedIndices.isEmpty()) {
            // 没有选中则复制全部
            text = String.join("\n", content.logLines);
        } else {
            // 复制选中的行
            StringBuilder sb = new StringBuilder();
            for (int i : selectedIndices) {
                if (i >= 0 && i < content.logLines.size()) {
                    if (sb.length() > 0) sb.append("\n");
                    sb.append(content.logLines.get(i));
                }
            }
            text = sb.toString();
        }
        
        javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
        javafx.scene.input.ClipboardContent clipboardContent = new javafx.scene.input.ClipboardContent();
        clipboardContent.putString(text);
        clipboard.setContent(clipboardContent);
        
        statusLabel.setText("已复制到剪贴板");
    }
    
    private void clearLogContent(LogDirectory directory) {
        TabContent content = tabContentMap.get(directory.getId());
        if (content == null || content.logListView == null) return;
        
        content.logLines.clear();
        content.currentFile = null;
    }
    
    private void showServerManager() {
        ServerManagerListDialog dialog = new ServerManagerListDialog(serverList);
        dialog.showAndWait().ifPresent(updatedList -> {
            serverList.clear();
            serverList.addAll(updatedList);
            saveServers();
        });
    }
    
    private void showHelp() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("使用说明");
        alert.setHeaderText("服务器日志查看器 - 使用指南");
        
        String help = ResourceLoader.loadResourceFileForClass(getClass(), "help.txt");
        
        TextArea textArea = new TextArea(help);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPrefHeight(400);
        
        alert.getDialogPane().setContent(textArea);
        alert.getDialogPane().setPrefWidth(600);
        alert.showAndWait();
    }
    
    private void showAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("关于");
        alert.setHeaderText("服务器日志查看器");
        String about = ResourceLoader.loadResourceFileForClass(getClass(), "about.txt");
        alert.setContentText(about);
        alert.showAndWait();
    }
    
    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
    
    // ==================== 服务器数据持久化 ====================
    
    private void loadSavedServers() {
        try {
            String serversData = prefs.get("servers_v2", "");
            if (serversData.isEmpty()) {
                // 尝试加载旧版本数据
                serversData = prefs.get("servers", "");
            }
            
            if (!serversData.isEmpty()) {
                List<ServerConfig> loadedServers = deserializeServers(serversData);
                for (ServerConfig server : loadedServers) {
                    serverList.add(server);
                    serverMap.put(server.getId(), server);
                }
                System.out.println("[LogViewer] 成功加载 " + loadedServers.size() + " 个服务器配置");
            } else {
                System.out.println("[LogViewer] 没有找到保存的服务器配置");
            }
        } catch (Exception e) {
            System.err.println("[LogViewer] 加载服务器配置失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void saveServers() {
        try {
            String data = serializeServers(serverList);
            prefs.put("servers_v2", data);
            System.out.println("[LogViewer] 成功保存 " + serverList.size() + " 个服务器配置");
        } catch (Exception e) {
            System.err.println("[LogViewer] 保存服务器配置失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 序列化服务器列表为 JSON 格式字符串
     */
    private String serializeServers(List<ServerConfig> servers) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < servers.size(); i++) {
            ServerConfig s = servers.get(i);
            if (i > 0) sb.append(",");
            sb.append("{");
            sb.append("\"id\":\"").append(escapeJson(s.getId())).append("\",");
            sb.append("\"name\":\"").append(escapeJson(s.getName())).append("\",");
            sb.append("\"host\":\"").append(escapeJson(s.getHost())).append("\",");
            sb.append("\"port\":").append(s.getPort()).append(",");
            sb.append("\"username\":\"").append(escapeJson(s.getUsername())).append("\",");
            sb.append("\"password\":\"").append(escapeJson(s.getPassword())).append("\",");
            sb.append("\"usePasswordAuth\":").append(s.isUsePasswordAuth()).append(",");
            sb.append("\"privateKeyPath\":\"").append(escapeJson(s.getPrivateKeyPath())).append("\",");
            sb.append("\"privateKeyPassphrase\":\"").append(escapeJson(s.getPrivateKeyPassphrase())).append("\",");
            sb.append("\"remark\":\"").append(escapeJson(s.getRemark())).append("\",");
            sb.append("\"dirs\":[");
            
            List<LogDirectory> dirs = s.getLogDirectories();
            if (dirs != null) {
                for (int j = 0; j < dirs.size(); j++) {
                    LogDirectory d = dirs.get(j);
                    if (j > 0) sb.append(",");
                    sb.append("{");
                    sb.append("\"id\":\"").append(escapeJson(d.getId())).append("\",");
                    sb.append("\"name\":\"").append(escapeJson(d.getName())).append("\",");
                    sb.append("\"path\":\"").append(escapeJson(d.getPath())).append("\",");
                    sb.append("\"pattern\":\"").append(escapeJson(d.getFilePattern())).append("\"");
                    sb.append("}");
                }
            }
            sb.append("]}");
        }
        sb.append("]");
        return sb.toString();
    }
    
    /**
     * 反序列化 JSON 格式字符串为服务器列表
     */
    private List<ServerConfig> deserializeServers(String json) {
        List<ServerConfig> result = new ArrayList<>();
        if (json == null || json.trim().isEmpty() || !json.startsWith("[")) {
            // 尝试旧格式解析
            return deserializeServersLegacy(json);
        }
        
        try {
            // 简单 JSON 解析（不使用第三方库）
            json = json.trim();
            if (json.startsWith("[") && json.endsWith("]")) {
                json = json.substring(1, json.length() - 1);
            }
            
            List<String> serverJsons = splitJsonObjects(json);
            for (String serverJson : serverJsons) {
                ServerConfig server = parseServerJson(serverJson);
                if (server != null) {
                    result.add(server);
                }
            }
        } catch (Exception e) {
            System.err.println("[LogViewer] JSON 解析失败，尝试旧格式: " + e.getMessage());
            return deserializeServersLegacy(json);
        }
        
        return result;
    }
    
    /**
     * 解析单个服务器 JSON
     */
    private ServerConfig parseServerJson(String json) {
        try {
            String id = extractJsonValue(json, "id");
            String name = extractJsonValue(json, "name");
            String host = extractJsonValue(json, "host");
            int port = Integer.parseInt(extractJsonValue(json, "port", "22"));
            String username = extractJsonValue(json, "username");
            String password = extractJsonValue(json, "password");
            boolean usePasswordAuth = Boolean.parseBoolean(extractJsonValue(json, "usePasswordAuth", "true"));
            String privateKeyPath = extractJsonValue(json, "privateKeyPath");
            String privateKeyPassphrase = extractJsonValue(json, "privateKeyPassphrase");
            String remark = extractJsonValue(json, "remark");
            
            // 解析目录
            List<LogDirectory> directories = new ArrayList<>();
            String dirsJson = extractJsonArray(json, "dirs");
            if (dirsJson != null && !dirsJson.isEmpty()) {
                List<String> dirJsons = splitJsonObjects(dirsJson);
                for (String dirJson : dirJsons) {
                    String dirId = extractJsonValue(dirJson, "id");
                    String dirName = extractJsonValue(dirJson, "name");
                    String dirPath = extractJsonValue(dirJson, "path");
                    String dirPattern = extractJsonValue(dirJson, "pattern");
                    if (dirId != null && dirPath != null) {
                        directories.add(LogDirectory.builder()
                            .id(dirId)
                            .name(dirName)
                            .path(dirPath)
                            .filePattern(dirPattern != null ? dirPattern : "*.log")
                            .build());
                    }
                }
            }
            
            return ServerConfig.builder()
                .id(id != null ? id : UUID.randomUUID().toString())
                .name(name)
                .host(host)
                .port(port)
                .username(username)
                .password(password)
                .usePasswordAuth(usePasswordAuth)
                .privateKeyPath(privateKeyPath)
                .privateKeyPassphrase(privateKeyPassphrase)
                .remark(remark)
                .logDirectories(directories)
                .build();
        } catch (Exception e) {
            System.err.println("[LogViewer] 解析服务器 JSON 失败: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 从 JSON 字符串中提取字段值
     */
    private String extractJsonValue(String json, String key) {
        return extractJsonValue(json, key, null);
    }
    
    private String extractJsonValue(String json, String key, String defaultValue) {
        String pattern = "\"" + key + "\":\"";
        int start = json.indexOf(pattern);
        if (start < 0) {
            // 尝试数字或布尔值
            pattern = "\"" + key + "\":";
            start = json.indexOf(pattern);
            if (start < 0) return defaultValue;
            start += pattern.length();
            int end = json.indexOf(",", start);
            if (end < 0) end = json.indexOf("}", start);
            if (end < 0) return defaultValue;
            return json.substring(start, end).trim();
        }
        start += pattern.length();
        int end = json.indexOf("\"", start);
        if (end < 0) return defaultValue;
        String value = json.substring(start, end);
        return unescapeJson(value);
    }
    
    /**
     * 从 JSON 字符串中提取数组
     */
    private String extractJsonArray(String json, String key) {
        String pattern = "\"" + key + "\":[";
        int start = json.indexOf(pattern);
        if (start < 0) return null;
        start += pattern.length();
        int end = json.indexOf("]", start);
        if (end < 0) return null;
        return json.substring(start, end);
    }
    
    /**
     * 分割 JSON 对象数组
     */
    private List<String> splitJsonObjects(String json) {
        List<String> result = new ArrayList<>();
        int depth = 0;
        int start = 0;
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') {
                if (depth == 0) start = i;
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    result.add(json.substring(start, i + 1));
                }
            }
        }
        return result;
    }
    
    /**
     * JSON 字符串转义
     */
    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
    
    /**
     * JSON 字符串反转义
     */
    private String unescapeJson(String s) {
        if (s == null) return null;
        return s.replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\\", "\\")
                .replace("\\\"", "\"");
    }
    
    /**
     * 旧格式反序列化（兼容旧版本数据）
     */
    private List<ServerConfig> deserializeServersLegacy(String data) {
        List<ServerConfig> result = new ArrayList<>();
        if (data == null || data.isEmpty()) return result;
        
        try {
            String[] serverEntries = data.split("\\|\\|");
            for (String entry : serverEntries) {
                ServerConfig server = parseServerFromStringLegacy(entry);
                if (server != null) {
                    result.add(server);
                }
            }
        } catch (Exception e) {
            System.err.println("[LogViewer] 旧格式解析失败: " + e.getMessage());
        }
        return result;
    }
    
    private ServerConfig parseServerFromStringLegacy(String str) {
        try {
            String[] parts = str.split("##DIRS##");
            String[] mainParts = parts[0].split("##");
            
            if (mainParts.length < 9) return null;
            
            List<LogDirectory> directories = new ArrayList<>();
            if (parts.length > 1) {
                String[] dirEntries = parts[1].split(";;");
                for (String dirEntry : dirEntries) {
                    String[] dirParts = dirEntry.split("::");
                    if (dirParts.length >= 4) {
                        directories.add(LogDirectory.builder()
                            .id(dirParts[0])
                            .name(dirParts[1])
                            .path(dirParts[2])
                            .filePattern(dirParts[3])
                            .build());
                    }
                }
            }
            
            return ServerConfig.builder()
                .id(mainParts[0])
                .name(mainParts[1])
                .host(mainParts[2])
                .port(Integer.parseInt(mainParts[3]))
                .username(mainParts[4])
                .password(mainParts[5])
                .usePasswordAuth(Boolean.parseBoolean(mainParts[6]))
                .privateKeyPath(mainParts[7])
                .privateKeyPassphrase(mainParts[8])
                .remark(mainParts.length > 9 ? mainParts[9] : "")
                .logDirectories(directories)
                .build();
                
        } catch (Exception e) {
            System.err.println("[LogViewer] 解析旧格式服务器配置失败: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Tab内容包装类
     */
    private static class TabContent {
        TableView<LogFile> fileTable;
        ListView<String> logListView;
        ObservableList<String> logLines = FXCollections.observableArrayList();
        ComboBox<String> linesCombo;
        CheckBox autoScrollCheck;
        CheckBox showLineNumbersCheck;
        LogFile currentFile;
    }
    
    /**
     * 服务器管理列表对话框
     */
    private static class ServerManagerListDialog extends Dialog<List<ServerConfig>> {
        
        private final ObservableList<ServerConfig> serverList;
        private ListView<ServerConfig> listView;
        
        public ServerManagerListDialog(ObservableList<ServerConfig> servers) {
            this.serverList = FXCollections.observableArrayList(servers);
            
            setTitle("服务器管理");
            setHeaderText("管理日志服务器配置");
            
            VBox content = new VBox(10);
            content.setPadding(new Insets(20));
            content.setPrefWidth(500);
            content.setPrefHeight(400);
            
            // 服务器列表
            listView = new ListView<>(serverList);
            listView.setCellFactory(lv -> new ListCell<ServerConfig>() {
                @Override
                protected void updateItem(ServerConfig item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        setText(item.getDisplayName());
                        FontIcon icon = new FontIcon(MaterialDesign.MDI_SERVER);
                        icon.setIconSize(16);
                        setGraphic(icon);
                    }
                }
            });
            VBox.setVgrow(listView, Priority.ALWAYS);
            
            // 按钮区域
            HBox buttonBox = new HBox(10);
            buttonBox.setAlignment(Pos.CENTER_LEFT);
            
            Button addBtn = new Button("添加", new FontIcon(MaterialDesign.MDI_PLUS));
            addBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
            addBtn.setOnAction(e -> addServer());
            
            Button editBtn = new Button("编辑", new FontIcon(MaterialDesign.MDI_PENCIL));
            editBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
            editBtn.setOnAction(e -> editServer());
            
            Button deleteBtn = new Button("删除", new FontIcon(MaterialDesign.MDI_DELETE));
            deleteBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
            deleteBtn.setOnAction(e -> deleteServer());
            
            buttonBox.getChildren().addAll(addBtn, editBtn, deleteBtn);
            
            content.getChildren().addAll(listView, buttonBox);
            
            getDialogPane().setContent(content);
            getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            
            setResultConverter(button -> {
                if (button == ButtonType.OK) {
                    return new ArrayList<>(serverList);
                }
                return null;
            });
        }
        
        private void addServer() {
            ServerManagerDialog dialog = new ServerManagerDialog();
            dialog.showAndWait().ifPresent(server -> {
                serverList.add(server);
            });
        }
        
        private void editServer() {
            ServerConfig selected = listView.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert("提示", "请先选择一个服务器");
                return;
            }
            
            ServerManagerDialog dialog = new ServerManagerDialog(selected);
            dialog.showAndWait().ifPresent(updated -> {
                int index = serverList.indexOf(selected);
                serverList.set(index, updated);
            });
        }
        
        private void deleteServer() {
            ServerConfig selected = listView.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert("提示", "请先选择一个服务器");
                return;
            }
            
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("确认删除");
            confirm.setHeaderText("删除服务器");
            confirm.setContentText("确定要删除服务器 \"" + selected.getName() + "\" 吗？");
            
            confirm.showAndWait().ifPresent(result -> {
                if (result == ButtonType.OK) {
                    serverList.remove(selected);
                }
            });
        }
        
        private void showAlert(String title, String content) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        }
    }
    
    /**
     * 日志行单元格 - 支持语法高亮和行号显示
     */
    private static class LogLineCell extends ListCell<String> {
        
        private final HBox container;
        private final Label lineNumberLabel;
        private final Label contentLabel;
        private final CheckBox showLineNumbersCheck;
        
        // 日志级别正则表达式
        private static final Pattern LOG_LEVEL_PATTERN = Pattern.compile(
            "\\b(?i)(TRACE|DEBUG|INFO|WARN|WARNING|ERROR|FATAL|SEVERE)\\b|" +
            "\\[(TRACE|DEBUG|INFO|WARN|WARNING|ERROR|FATAL|SEVERE)\\]",
            Pattern.CASE_INSENSITIVE
        );
        
        // 时间戳正则
        private static final Pattern TIMESTAMP_PATTERN = Pattern.compile(
            "\\d{4}[-/]\\d{2}[-/]\\d{2}[T ]\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?(Z|[+-]\\d{2}:?\\d{2})?"
        );
        
        // 异常类名正则
        private static final Pattern EXCEPTION_PATTERN = Pattern.compile(
            "^[\\t ]*(at\\s+[\\w.$]+\\.[\\w]+\\([^)]*\\))|" +
            "(^[\\w.$]+(Exception|Error|Throwable)[\\w.$]*:.*)"
        );
        
        public LogLineCell(CheckBox showLineNumbersCheck) {
            this.showLineNumbersCheck = showLineNumbersCheck;
            
            container = new HBox();
            container.setAlignment(Pos.CENTER_LEFT);
            container.setSpacing(8);
            
            // 行号标签
            lineNumberLabel = new Label();
            lineNumberLabel.setStyle("-fx-font-family: 'Consolas', 'Monaco', 'Courier New', monospace; " +
                                   "-fx-font-size: 12px; " +
                                   "-fx-text-fill: #858585; " +
                                   "-fx-min-width: 50px; " +
                                   "-fx-alignment: CENTER-RIGHT;");
            lineNumberLabel.setVisible(showLineNumbersCheck.isSelected());
            lineNumberLabel.setManaged(showLineNumbersCheck.isSelected());
            
            // 监听行号显示开关
            showLineNumbersCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
                lineNumberLabel.setVisible(newVal);
                lineNumberLabel.setManaged(newVal);
            });
            
            // 内容标签
            contentLabel = new Label();
            contentLabel.setStyle("-fx-font-family: 'Consolas', 'Monaco', 'Courier New', monospace; " +
                                "-fx-font-size: 12px;");
            contentLabel.setWrapText(false);
            contentLabel.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
            
            container.getChildren().addAll(lineNumberLabel, contentLabel);
            
            // 设置单元格样式
            setStyle("-fx-background-color: transparent; -fx-padding: 2 5 2 5;");
        }
        
        @Override
        protected void updateItem(String line, boolean empty) {
            super.updateItem(line, empty);
            
            if (empty || line == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            
            // 更新行号
            int lineNumber = getIndex() + 1;
            lineNumberLabel.setText(String.valueOf(lineNumber));
            
            // 分析日志行并设置颜色
            String style = analyzeLogLine(line);
            contentLabel.setText(line);
            contentLabel.setStyle("-fx-font-family: 'Consolas', 'Monaco', 'Courier New', monospace; " +
                                "-fx-font-size: 12px; " + style);
            
            setGraphic(container);
        }
        
        /**
         * 分析日志行并返回对应的样式
         */
        private String analyzeLogLine(String line) {
            // 默认样式 - 浅灰色文本
            String textStyle = "-fx-text-fill: #d4d4d4;";
            
            // 检测日志级别
            Matcher levelMatcher = LOG_LEVEL_PATTERN.matcher(line);
            if (levelMatcher.find()) {
                String level = levelMatcher.group(1) != null ? 
                    levelMatcher.group(1).toUpperCase() : 
                    (levelMatcher.group(2) != null ? levelMatcher.group(2).toUpperCase() : "");
                
                switch (level) {
                    case "TRACE":
                    case "DEBUG":
                        textStyle = "-fx-text-fill: #608b4e;"; // 绿色
                        break;
                    case "INFO":
                        textStyle = "-fx-text-fill: #4ec9b0;"; // 青色
                        break;
                    case "WARN":
                    case "WARNING":
                        textStyle = "-fx-text-fill: #dcdcaa;"; // 黄色
                        break;
                    case "ERROR":
                    case "FATAL":
                    case "SEVERE":
                        textStyle = "-fx-text-fill: #f14c4c;"; // 红色
                        break;
                }
                return textStyle;
            }
            
            // 检测异常堆栈
            Matcher exceptionMatcher = EXCEPTION_PATTERN.matcher(line);
            if (exceptionMatcher.find()) {
                return "-fx-text-fill: #ce9178;"; // 橙色
            }
            
            // 检测时间戳
            Matcher timestampMatcher = TIMESTAMP_PATTERN.matcher(line);
            if (timestampMatcher.find()) {
                // 时间戳高亮但不改变整体颜色
                return "-fx-text-fill: #d4d4d4;"; // 默认颜色
            }
            
            return textStyle;
        }
    }
}
