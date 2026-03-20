package io.github.lemostic.toolsuite.modules.devops.jenkinsdownloader;

import io.github.lemostic.toolsuite.modules.devops.jenkinsdownloader.model.FileInfo;
import io.github.lemostic.toolsuite.modules.devops.jenkinsdownloader.model.JenkinsConfig;
import io.github.lemostic.toolsuite.modules.devops.jenkinsdownloader.model.MatchType;
import io.github.lemostic.toolsuite.modules.devops.jenkinsdownloader.service.ConfigStorageService;
import io.github.lemostic.toolsuite.modules.devops.jenkinsdownloader.service.JenkinsDownloadService;
import io.github.lemostic.toolsuite.util.ResourceLoader;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign.MaterialDesign;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Jenkins文件下载工具界面
 */
public class JenkinsDownloaderView extends BorderPane {
    
    private final JenkinsDownloadService downloadService;
    private final ConfigStorageService configStorage;
    
    // 配置相关
    private ComboBox<JenkinsConfig> configComboBox;
    private JenkinsConfig currentConfig;
    
    // URL输入
    private TextField urlField;
    
    // 过滤相关
    private TextField filterPatternField;
    private ComboBox<MatchType> matchTypeCombo;
    private Button filterBtn;
    private Button clearFilterBtn;
    
    // 文件列表
    private TableView<FileInfo> fileTable;
    private ObservableList<FileInfo> allFiles;
    private FilteredList<FileInfo> filteredFiles;
    private Label fileCountLabel;
    private Label selectedCountLabel;
    
    // 下载相关
    private TextField downloadPathField;
    private ProgressBar progressBar;
    private Label statusLabel;
    private Button downloadBtn;
    
    // 当前过滤结果
    private List<FileInfo> currentFilteredFiles = new ArrayList<>();
    
    public JenkinsDownloaderView() {
        this.downloadService = new JenkinsDownloadService();
        this.configStorage = new ConfigStorageService();
        this.allFiles = FXCollections.observableArrayList();
        this.filteredFiles = new FilteredList<>(allFiles, p -> true);
        
        initializeUI();
        bindProperties();
        loadSavedConfigs();
    }
    
    private void initializeUI() {
        setStyle("-fx-background-color: #f5f5f5;");
        
        // 顶部工具栏
        setTop(createToolbar());
        
        // 中间主内容
        setCenter(createMainContent());
        
        // 底部状态栏
        setBottom(createStatusBar());
    }
    
    private Node createToolbar() {
        ToolBar toolbar = new ToolBar();
        toolbar.setStyle("-fx-background-color: linear-gradient(to bottom, #ffffff, #e8e8e8); " +
                        "-fx-border-color: #d0d0d0; -fx-border-width: 0 0 1 0;");
        
        Button helpBtn = new Button("使用说明", new FontIcon(MaterialDesign.MDI_HELP_CIRCLE));
        helpBtn.setOnAction(e -> showHelp());
        
        Button aboutBtn = new Button("关于", new FontIcon(MaterialDesign.MDI_INFORMATION));
        aboutBtn.setOnAction(e -> showAbout());
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        toolbar.getItems().addAll(helpBtn, spacer, aboutBtn);
        
        return toolbar;
    }
    
    private Node createMainContent() {
        VBox mainBox = new VBox(15);
        mainBox.setPadding(new Insets(20));
        
        // 配置卡片
        mainBox.getChildren().add(createConfigCard());
        
        // URL输入卡片
        mainBox.getChildren().add(createUrlCard());
        
        // 过滤卡片
        mainBox.getChildren().add(createFilterCard());
        
        // 文件列表卡片
        mainBox.getChildren().add(createFileListCard());
        
        // 下载卡片
        mainBox.getChildren().add(createDownloadCard());
        
        ScrollPane scrollPane = new ScrollPane(mainBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #f5f5f5;");
        
        return scrollPane;
    }
    
    private Node createConfigCard() {
        VBox card = createCard("Jenkins配置", MaterialDesign.MDI_SERVER);
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
        grid.setPadding(new Insets(10));
        
        // 配置选择
        Label configLabel = new Label("选择配置:");
        configLabel.setStyle("-fx-font-weight: bold;");
        
        configComboBox = new ComboBox<>();
        configComboBox.setPromptText("请选择或创建Jenkins配置...");
        configComboBox.setPrefWidth(350);
        configComboBox.setCellFactory(param -> new ListCell<JenkinsConfig>() {
            @Override
            protected void updateItem(JenkinsConfig item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getConfigName() + " (" + item.getBaseUrl() + ")");
                }
            }
        });
        configComboBox.setButtonCell(new ListCell<JenkinsConfig>() {
            @Override
            protected void updateItem(JenkinsConfig item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getConfigName());
                }
            }
        });
        configComboBox.setOnAction(e -> {
            currentConfig = configComboBox.getValue();
        });
        
        Button newConfigBtn = new Button("新建", new FontIcon(MaterialDesign.MDI_PLUS));
        newConfigBtn.setOnAction(e -> showConfigDialog(null));
        
        Button editConfigBtn = new Button("编辑", new FontIcon(MaterialDesign.MDI_PENCIL));
        editConfigBtn.setOnAction(e -> {
            if (currentConfig != null) {
                showConfigDialog(currentConfig);
            } else {
                showAlert("提示", "请先选择一个配置", Alert.AlertType.INFORMATION);
            }
        });
        
        Button deleteConfigBtn = new Button("删除", new FontIcon(MaterialDesign.MDI_DELETE));
        deleteConfigBtn.setStyle("-fx-text-fill: #d32f2f;");
        deleteConfigBtn.setOnAction(e -> {
            if (currentConfig != null) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("确认删除");
                confirm.setHeaderText("删除配置: " + currentConfig.getConfigName());
                confirm.setContentText("确定要删除这个配置吗？");
                
                Optional<ButtonType> result = confirm.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                    configStorage.deleteConfig(currentConfig.getConfigName());
                    loadSavedConfigs();
                }
            }
        });
        
        grid.add(configLabel, 0, 0);
        grid.add(configComboBox, 1, 0);
        grid.add(newConfigBtn, 2, 0);
        grid.add(editConfigBtn, 3, 0);
        grid.add(deleteConfigBtn, 4, 0);
        
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setMinWidth(100);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);
        col2.setMinWidth(200);
        grid.getColumnConstraints().addAll(col1, col2);
        
        card.getChildren().add(grid);
        
        return card;
    }
    
    private Node createUrlCard() {
        VBox card = createCard("工作区URL", MaterialDesign.MDI_LINK_VARIANT);
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
        grid.setPadding(new Insets(10));
        
        Label urlLabel = new Label("工作区URL:");
        urlLabel.setStyle("-fx-font-weight: bold;");
        
        urlField = new TextField();
        urlField.setPromptText("请输入Jenkins工作区URL，例如: http://jenkins/job/myjob/ws/target/lib/");
        
        Button fetchBtn = new Button("获取文件列表", new FontIcon(MaterialDesign.MDI_REFRESH));
        fetchBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
        fetchBtn.setOnAction(e -> fetchFileList());
        
        grid.add(urlLabel, 0, 0);
        grid.add(urlField, 1, 0);
        grid.add(fetchBtn, 2, 0);
        
        // 提示信息
        Label tipLabel = new Label("提示: URL格式应为 http://jenkins-server/job/jobname/ws/path/");
        tipLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666; -fx-font-style: italic;");
        grid.add(tipLabel, 1, 1, 2, 1);
        
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setMinWidth(100);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(col1, col2);
        
        card.getChildren().add(grid);
        
        return card;
    }
    
    private Node createFilterCard() {
        VBox card = createCard("文件过滤", MaterialDesign.MDI_FILTER_VARIANT);
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
        grid.setPadding(new Insets(10));
        
        // 匹配类型
        Label typeLabel = new Label("匹配方式:");
        typeLabel.setStyle("-fx-font-weight: bold;");
        
        matchTypeCombo = new ComboBox<>();
        matchTypeCombo.getItems().addAll(MatchType.values());
        matchTypeCombo.setValue(MatchType.PREFIX);
        matchTypeCombo.setPrefWidth(150);
        
        // 过滤模式
        Label patternLabel = new Label("匹配规则:");
        patternLabel.setStyle("-fx-font-weight: bold;");
        
        filterPatternField = new TextField();
        filterPatternField.setPromptText("输入匹配规则，例如: myapp-");
        
        filterBtn = new Button("应用过滤", new FontIcon(MaterialDesign.MDI_FILTER));
        filterBtn.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white;");
        filterBtn.setOnAction(e -> applyFilter());
        
        clearFilterBtn = new Button("清除", new FontIcon(MaterialDesign.MDI_CLOSE));
        clearFilterBtn.setOnAction(e -> clearFilter());
        
        // 全选/取消全选
        Button selectAllBtn = new Button("全选", new FontIcon(MaterialDesign.MDI_CHECKBOX_MULTIPLE_MARKED));
        selectAllBtn.setOnAction(e -> setAllSelected(true));
        
        Button deselectAllBtn = new Button("取消全选", new FontIcon(MaterialDesign.MDI_CHECKBOX_MULTIPLE_BLANK_OUTLINE));
        deselectAllBtn.setOnAction(e -> setAllSelected(false));
        
        grid.add(typeLabel, 0, 0);
        grid.add(matchTypeCombo, 1, 0);
        grid.add(patternLabel, 2, 0);
        grid.add(filterPatternField, 3, 0);
        grid.add(filterBtn, 4, 0);
        grid.add(clearFilterBtn, 5, 0);
        grid.add(selectAllBtn, 6, 0);
        grid.add(deselectAllBtn, 7, 0);
        
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setMinWidth(80);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setMinWidth(150);
        ColumnConstraints col3 = new ColumnConstraints();
        col3.setMinWidth(80);
        ColumnConstraints col4 = new ColumnConstraints();
        col4.setHgrow(Priority.ALWAYS);
        col4.setMinWidth(200);
        
        grid.getColumnConstraints().addAll(col1, col2, col3, col4);
        
        card.getChildren().add(grid);
        
        return card;
    }
    
    private Node createFileListCard() {
        VBox card = createCard("文件列表", MaterialDesign.MDI_FILE_MULTIPLE);
        
        // 统计信息
        HBox statsBox = new HBox(20);
        statsBox.setPadding(new Insets(0, 0, 10, 0));
        
        fileCountLabel = new Label("总文件数: 0");
        selectedCountLabel = new Label("已选择: 0");
        
        statsBox.getChildren().addAll(fileCountLabel, selectedCountLabel);
        
        // 文件表格
        fileTable = new TableView<>();
        fileTable.setItems(filteredFiles);
        fileTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        fileTable.setPrefHeight(300);
        
        // 选择列
        TableColumn<FileInfo, Boolean> selectCol = new TableColumn<>("下载");
        selectCol.setCellValueFactory(new PropertyValueFactory<>("selected"));
        selectCol.setCellFactory(CheckBoxTableCell.forTableColumn(selectCol));
        selectCol.setEditable(true);
        selectCol.setMaxWidth(60);
        selectCol.setMinWidth(60);
        
        // 文件名列
        TableColumn<FileInfo, String> nameCol = new TableColumn<>("文件名");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("fileName"));
        nameCol.setPrefWidth(300);
        
        // 大小列
        TableColumn<FileInfo, String> sizeCol = new TableColumn<>("大小");
        sizeCol.setCellValueFactory(new PropertyValueFactory<>("fileSize"));
        sizeCol.setMaxWidth(100);
        sizeCol.setMinWidth(80);
        
        // 路径列
        TableColumn<FileInfo, String> pathCol = new TableColumn<>("路径");
        pathCol.setCellValueFactory(new PropertyValueFactory<>("relativePath"));
        pathCol.setPrefWidth(250);
        
        // 状态列
        TableColumn<FileInfo, String> statusCol = new TableColumn<>("状态");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setMaxWidth(120);
        statusCol.setMinWidth(100);
        
        fileTable.getColumns().addAll(selectCol, nameCol, sizeCol, pathCol, statusCol);
        fileTable.setEditable(true);
        
        card.getChildren().addAll(statsBox, fileTable);
        
        return card;
    }
    
    private Node createDownloadCard() {
        VBox card = createCard("下载设置", MaterialDesign.MDI_DOWNLOAD);
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
        grid.setPadding(new Insets(10));
        
        // 下载路径
        Label pathLabel = new Label("下载到:");
        pathLabel.setStyle("-fx-font-weight: bold;");
        
        downloadPathField = new TextField();
        downloadPathField.setPromptText("选择文件下载保存的目录...");
        String userHome = System.getProperty("user.home");
        downloadPathField.setText(Paths.get(userHome, "Downloads", "jenkins-files").toString());
        
        Button browseBtn = new Button("浏览", new FontIcon(MaterialDesign.MDI_FOLDER));
        browseBtn.setOnAction(e -> selectDownloadDirectory());
        
        // 下载按钮
        downloadBtn = new Button("开始下载", new FontIcon(MaterialDesign.MDI_DOWNLOAD));
        downloadBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        downloadBtn.setOnAction(e -> startDownload());
        
        grid.add(pathLabel, 0, 0);
        grid.add(downloadPathField, 1, 0);
        grid.add(browseBtn, 2, 0);
        grid.add(downloadBtn, 3, 0);
        
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setMinWidth(80);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);
        
        grid.getColumnConstraints().addAll(col1, col2);
        
        card.getChildren().add(grid);
        
        return card;
    }
    
    private VBox createCard(String title, MaterialDesign icon) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: white; " +
                     "-fx-border-color: #e0e0e0; " +
                     "-fx-border-radius: 5; " +
                     "-fx-background-radius: 5; " +
                     "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);");
        card.setPadding(new Insets(15));
        
        // 标题栏
        HBox titleBox = new HBox(8);
        titleBox.setAlignment(Pos.CENTER_LEFT);
        
        FontIcon titleIcon = new FontIcon(icon);
        titleIcon.setIconSize(20);
        titleIcon.setStyle("-fx-icon-color: #2196F3;");
        
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #333;");
        
        titleBox.getChildren().addAll(titleIcon, titleLabel);
        
        Separator separator = new Separator();
        
        card.getChildren().addAll(titleBox, separator);
        
        return card;
    }
    
    private Node createStatusBar() {
        VBox statusBox = new VBox(5);
        statusBox.setPadding(new Insets(10));
        statusBox.setStyle("-fx-background-color: linear-gradient(to top, #ffffff, #f0f0f0); " +
                          "-fx-border-color: #d0d0d0; -fx-border-width: 1 0 0 0;");
        
        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(Double.MAX_VALUE);
        
        statusLabel = new Label("就绪");
        statusLabel.setStyle("-fx-text-fill: #666;");
        
        statusBox.getChildren().addAll(progressBar, statusLabel);
        
        return statusBox;
    }
    
    private void bindProperties() {
        statusLabel.textProperty().bind(downloadService.statusMessageProperty());
        progressBar.progressProperty().bind(downloadService.progressProperty());
        
        // 更新统计信息
        allFiles.addListener((javafx.collections.ListChangeListener<FileInfo>) c -> {
            updateStats();
        });
    }
    
    private void loadSavedConfigs() {
        List<JenkinsConfig> configs = configStorage.loadConfigs();
        configComboBox.getItems().clear();
        configComboBox.getItems().addAll(configs);
        
        if (!configs.isEmpty()) {
            configComboBox.setValue(configs.get(0));
            currentConfig = configs.get(0);
        }
    }
    
    private void updateStats() {
        int total = allFiles.size();
        int selected = (int) allFiles.stream().filter(FileInfo::isSelected).count();
        
        fileCountLabel.setText("总文件数: " + total);
        selectedCountLabel.setText("已选择: " + selected);
    }
    
    private void showConfigDialog(JenkinsConfig config) {
        Dialog<JenkinsConfig> dialog = new Dialog<>();
        dialog.setTitle(config == null ? "新建Jenkins配置" : "编辑Jenkins配置");
        dialog.setHeaderText("配置Jenkins服务器连接信息");
        
        ButtonType saveButtonType = new ButtonType("保存", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        TextField nameField = new TextField();
        nameField.setPromptText("例如: 公司Jenkins");
        
        TextField urlField = new TextField();
        urlField.setPromptText("例如: http://jenkins.example.com:8080");
        
        TextField usernameField = new TextField();
        usernameField.setPromptText("Jenkins用户名");
        
        PasswordField tokenField = new PasswordField();
        tokenField.setPromptText("API Token或密码");
        
        // 如果是编辑，填充现有值
        if (config != null) {
            nameField.setText(config.getConfigName());
            urlField.setText(config.getBaseUrl());
            usernameField.setText(config.getUsername());
            tokenField.setText(config.getApiToken());
        }
        
        grid.add(new Label("配置名称:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("服务器URL:"), 0, 1);
        grid.add(urlField, 1, 1);
        grid.add(new Label("用户名:"), 0, 2);
        grid.add(usernameField, 1, 2);
        grid.add(new Label("API Token:"), 0, 3);
        grid.add(tokenField, 1, 3);
        
        // 添加提示
        Label tipLabel = new Label("提示: API Token可在 Jenkins -> 用户设置 -> Configure -> API Token 中生成");
        tipLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");
        tipLabel.setWrapText(true);
        grid.add(tipLabel, 0, 4, 2, 1);
        
        dialog.getDialogPane().setContent(grid);
        
        Platform.runLater(nameField::requestFocus);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                JenkinsConfig newConfig = new JenkinsConfig();
                newConfig.setConfigName(nameField.getText());
                newConfig.setBaseUrl(urlField.getText());
                newConfig.setUsername(usernameField.getText());
                newConfig.setApiToken(tokenField.getText());
                return newConfig;
            }
            return null;
        });
        
        Optional<JenkinsConfig> result = dialog.showAndWait();
        result.ifPresent(newConfig -> {
            if (newConfig.isValid()) {
                configStorage.saveConfig(newConfig);
                loadSavedConfigs();
                configComboBox.setValue(newConfig);
                currentConfig = newConfig;
            } else {
                showAlert("错误", "请填写所有必填字段", Alert.AlertType.ERROR);
            }
        });
    }
    
    private void fetchFileList() {
        if (currentConfig == null) {
            showAlert("错误", "请先选择或创建一个Jenkins配置", Alert.AlertType.ERROR);
            return;
        }
        
        String url = urlField.getText();
        if (url == null || url.trim().isEmpty()) {
            showAlert("错误", "请输入Jenkins工作区URL", Alert.AlertType.ERROR);
            return;
        }
        
        // 验证URL格式
        JenkinsDownloadService.ParsedUrl parsed = downloadService.parseWorkspaceUrl(url);
        if (!parsed.isValid()) {
            showAlert("错误", parsed.getErrorMessage(), Alert.AlertType.ERROR);
            return;
        }
        
        // 在后台线程中获取文件列表
        new Thread(() -> {
            try {
                Platform.runLater(() -> {
                    allFiles.clear();
                    statusLabel.setText("正在获取文件列表...");
                });
                
                List<FileInfo> files = downloadService.fetchFileList(currentConfig, url);
                
                Platform.runLater(() -> {
                    allFiles.addAll(files);
                    currentFilteredFiles = new ArrayList<>(files);
                    updateStats();
                    
                    if (files.isEmpty()) {
                        showAlert("提示", "未找到任何文件，请检查URL是否正确", Alert.AlertType.INFORMATION);
                    }
                });
                
            } catch (SecurityException e) {
                Platform.runLater(() -> {
                    showAlert("认证失败", e.getMessage(), Alert.AlertType.ERROR);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showAlert("获取失败", "无法获取文件列表: " + e.getMessage(), Alert.AlertType.ERROR);
                });
            }
        }).start();
    }
    
    private void applyFilter() {
        String pattern = filterPatternField.getText();
        MatchType matchType = matchTypeCombo.getValue();
        
        if (pattern == null || pattern.trim().isEmpty()) {
            filteredFiles.setPredicate(p -> true);
            currentFilteredFiles = new ArrayList<>(allFiles);
        } else {
            currentFilteredFiles = downloadService.filterFiles(allFiles, pattern, matchType);
            
            // 更新表格显示
            filteredFiles.setPredicate(fileInfo -> {
                return currentFilteredFiles.contains(fileInfo);
            });
        }
        
        updateStats();
    }
    
    private void clearFilter() {
        filterPatternField.clear();
        filteredFiles.setPredicate(p -> true);
        currentFilteredFiles = new ArrayList<>(allFiles);
        updateStats();
    }
    
    private void setAllSelected(boolean selected) {
        for (FileInfo file : currentFilteredFiles) {
            file.setSelected(selected);
        }
        // 刷新表格
        fileTable.refresh();
        updateStats();
    }
    
    private void selectDownloadDirectory() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("选择下载目录");
        
        String currentPath = downloadPathField.getText();
        if (currentPath != null && !currentPath.isEmpty()) {
            File dir = new File(currentPath);
            if (dir.exists()) {
                chooser.setInitialDirectory(dir);
            }
        }
        
        File selected = chooser.showDialog(getScene().getWindow());
        if (selected != null) {
            downloadPathField.setText(selected.getAbsolutePath());
        }
    }
    
    private void startDownload() {
        if (currentConfig == null) {
            showAlert("错误", "请先选择Jenkins配置", Alert.AlertType.ERROR);
            return;
        }
        
        // 获取选中的文件
        List<FileInfo> selectedFiles = new ArrayList<>();
        for (FileInfo file : allFiles) {
            if (file.isSelected()) {
                selectedFiles.add(file);
            }
        }
        
        if (selectedFiles.isEmpty()) {
            showAlert("错误", "请至少选择一个要下载的文件", Alert.AlertType.ERROR);
            return;
        }
        
        String downloadPath = downloadPathField.getText();
        if (downloadPath == null || downloadPath.trim().isEmpty()) {
            showAlert("错误", "请选择下载目录", Alert.AlertType.ERROR);
            return;
        }
        
        Path targetDir = Paths.get(downloadPath);
        
        // 确认对话框
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("确认下载");
        confirm.setHeaderText("即将开始下载文件");
        confirm.setContentText(String.format("将要下载 %d 个文件到:\n%s\n\n是否继续？", 
            selectedFiles.size(), targetDir.toString()));
        
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // 在后台线程中下载
            new Thread(() -> {
                JenkinsDownloadService.DownloadResult downloadResult = 
                    downloadService.downloadFiles(selectedFiles, currentConfig, targetDir);
                
                Platform.runLater(() -> {
                    if (downloadResult.isSuccess()) {
                        showAlert("下载完成", downloadResult.getMessage(), Alert.AlertType.INFORMATION);
                    } else {
                        showAlert("下载完成（有失败）", downloadResult.getMessage(), Alert.AlertType.WARNING);
                    }
                });
            }).start();
        }
    }
    
    private void showHelp() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("使用说明");
        alert.setHeaderText("Jenkins文件下载工具 - 使用指南");
        
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
        alert.setHeaderText("Jenkins文件下载工具");
        alert.setContentText(ResourceLoader.loadResourceFileForClass(getClass(), "about.txt"));
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
