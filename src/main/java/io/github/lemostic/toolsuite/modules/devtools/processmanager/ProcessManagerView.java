package io.github.lemostic.toolsuite.modules.devtools.processmanager;

import io.github.lemostic.toolsuite.modules.devtools.processmanager.model.ProcessInfo;
import io.github.lemostic.toolsuite.modules.devtools.processmanager.service.ProcessManagerService;
import io.github.lemostic.toolsuite.util.ResourceLoader;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign.MaterialDesign;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 进程管理器主视图
 * 支持查看进程列表、通过端口查找进程、结束进程
 */
public class ProcessManagerView extends BorderPane {
    
    private final ProcessManagerService service = new ProcessManagerService();
    
    // 进程列表
    private TableView<ProcessInfo> processTable;
    private ObservableList<ProcessInfo> processList = FXCollections.observableArrayList();
    private ObservableList<ProcessInfo> allProcesses = FXCollections.observableArrayList();
    
    // 搜索控件
    private TextField portSearchField;
    private TextField nameSearchField;
    private Button searchByPortBtn;
    private Button searchByNameBtn;
    private Button refreshBtn;
    private Button killBtn;
    private Button killBatchBtn;
    private CheckBox selectAllCheck;
    
    // 状态栏
    private Label statusLabel;
    private ProgressBar progressBar;
    private Label countLabel;
    
    public ProcessManagerView() {
        initializeUI();
        bindProperties();
        loadProcessList();
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
        VBox toolbarContainer = new VBox(10);
        toolbarContainer.setPadding(new Insets(15));
        toolbarContainer.setStyle("-fx-background-color: linear-gradient(to bottom, #ffffff, #e8e8e8); " +
                                  "-fx-border-color: #d0d0d0; -fx-border-width: 0 0 1 0;");
        
        // 第一行：标题和主要操作
        HBox topRow = new HBox(15);
        topRow.setAlignment(Pos.CENTER_LEFT);
        
        FontIcon titleIcon = new FontIcon(MaterialDesign.MDI_APPLICATION);
        titleIcon.setIconSize(24);
        titleIcon.setStyle("-fx-icon-color: #2196F3;");
        
        Label titleLabel = new Label("进程管理器");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #333;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        refreshBtn = new Button("刷新列表", new FontIcon(MaterialDesign.MDI_REFRESH));
        refreshBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-weight: bold;");
        refreshBtn.setOnAction(e -> loadProcessList());
        
        killBtn = new Button("结束进程", new FontIcon(MaterialDesign.MDI_CLOSE_OCTAGON));
        killBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-weight: bold;");
        killBtn.setOnAction(e -> killSelectedProcess());
        killBtn.setDisable(true);
        
        killBatchBtn = new Button("批量结束", new FontIcon(MaterialDesign.MDI_CLOSE_OCTAGON_OUTLINE));
        killBatchBtn.setStyle("-fx-background-color: #FF5722; -fx-text-fill: white; -fx-font-weight: bold;");
        killBatchBtn.setOnAction(e -> killSelectedProcesses());
        killBatchBtn.setDisable(true);
        
        Button helpBtn = new Button("帮助", new FontIcon(MaterialDesign.MDI_HELP_CIRCLE));
        helpBtn.setOnAction(e -> showHelp());
        
        Button aboutBtn = new Button("关于", new FontIcon(MaterialDesign.MDI_INFORMATION));
        aboutBtn.setOnAction(e -> showAbout());
        
        topRow.getChildren().addAll(titleIcon, titleLabel, spacer, refreshBtn, killBtn, killBatchBtn, helpBtn, aboutBtn);
        
        // 第二行：搜索区域（使用 GridPane 对齐）
        GridPane searchGrid = new GridPane();
        searchGrid.setHgap(15);
        searchGrid.setVgap(8);
        
        // 端口搜索
        Label portLabel = new Label("按端口查找:");
        portLabel.setStyle("-fx-font-weight: bold; -fx-pref-width: 80;");
        GridPane.setHalignment(portLabel, HPos.RIGHT);
        
        HBox portInputBox = new HBox(5);
        portInputBox.setAlignment(Pos.CENTER_LEFT);
        portSearchField = new TextField();
        portSearchField.setPromptText("8080, 3306, 6379 (多个用逗号分隔)");
        portSearchField.setPrefWidth(280);
        searchByPortBtn = new Button("查找", new FontIcon(MaterialDesign.MDI_MAGNIFY));
        searchByPortBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        searchByPortBtn.setOnAction(e -> searchByPort());
        portSearchField.setOnAction(e -> searchByPort());
        portInputBox.getChildren().addAll(portSearchField, searchByPortBtn);
        
        searchGrid.add(portLabel, 0, 0);
        searchGrid.add(portInputBox, 1, 0);
        
        // 名称搜索
        Label nameLabel = new Label("按名称筛选:");
        nameLabel.setStyle("-fx-font-weight: bold;");
        GridPane.setHalignment(nameLabel, HPos.RIGHT);
        
        HBox nameInputBox = new HBox(5);
        nameInputBox.setAlignment(Pos.CENTER_LEFT);
        nameSearchField = new TextField();
        nameSearchField.setPromptText("输入进程名称关键词");
        nameSearchField.setPrefWidth(200);
        searchByNameBtn = new Button("筛选", new FontIcon(MaterialDesign.MDI_FILTER));
        searchByNameBtn.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white;");
        searchByNameBtn.setOnAction(e -> filterByName());
        nameSearchField.setOnAction(e -> filterByName());
        Button clearFilterBtn = new Button("清除", new FontIcon(MaterialDesign.MDI_FILTER_REMOVE));
        clearFilterBtn.setStyle("-fx-background-color: #9E9E9E; -fx-text-fill: white;");
        clearFilterBtn.setOnAction(e -> clearFilter());
        nameInputBox.getChildren().addAll(nameSearchField, searchByNameBtn, clearFilterBtn);
        
        searchGrid.add(nameLabel, 2, 0);
        searchGrid.add(nameInputBox, 3, 0);
        
        // 全选复选框
        selectAllCheck = new CheckBox("全选");
        selectAllCheck.setOnAction(e -> toggleSelectAll());
        searchGrid.add(selectAllCheck, 4, 0);
        
        // 设置列宽约束
        ColumnConstraints cc1 = new ColumnConstraints();
        cc1.setHalignment(HPos.RIGHT);
        ColumnConstraints cc2 = new ColumnConstraints();
        cc2.setHgrow(Priority.ALWAYS);
        ColumnConstraints cc3 = new ColumnConstraints();
        cc3.setHalignment(HPos.RIGHT);
        ColumnConstraints cc4 = new ColumnConstraints();
        cc4.setHgrow(Priority.ALWAYS);
        ColumnConstraints cc5 = new ColumnConstraints();
        cc5.setMinWidth(60);
        searchGrid.getColumnConstraints().addAll(cc1, cc2, cc3, cc4, cc5);
        
        toolbarContainer.getChildren().addAll(topRow, new Separator(), searchGrid);
        return toolbarContainer;
    }
    
    private Node createMainContent() {
        VBox mainContent = new VBox(10);
        mainContent.setPadding(new Insets(15));
        
        // 进程列表表格
        processTable = new TableView<>();
        processTable.setItems(processList);
        processTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(processTable, Priority.ALWAYS);
        
        // 选择列
        TableColumn<ProcessInfo, Boolean> selectCol = new TableColumn<>("");
        selectCol.setMinWidth(35);
        selectCol.setMaxWidth(35);
        selectCol.setResizable(false);
        selectCol.setCellValueFactory(data -> new SimpleBooleanProperty(data.getValue().isSelected()));
        selectCol.setCellFactory(col -> new TableCell<ProcessInfo, Boolean>() {
            private final CheckBox checkBox = new CheckBox();
            {
                checkBox.setOnAction(e -> {
                    ProcessInfo process = getTableView().getItems().get(getIndex());
                    process.setSelected(checkBox.isSelected());
                    updateButtonStates();
                });
            }
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    ProcessInfo process = getTableView().getItems().get(getIndex());
                    checkBox.setSelected(process.isSelected());
                    setGraphic(checkBox);
                }
            }
        });
        
        // PID 列
        TableColumn<ProcessInfo, Number> pidCol = new TableColumn<>("PID");
        pidCol.setPrefWidth(80);
        pidCol.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getPid()));
        pidCol.setStyle("-fx-alignment: CENTER;");
        
        // 进程名列
        TableColumn<ProcessInfo, String> nameCol = new TableColumn<>("进程名称");
        nameCol.setPrefWidth(200);
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        
        // 端口列
        TableColumn<ProcessInfo, String> portCol = new TableColumn<>("占用端口");
        portCol.setPrefWidth(150);
        portCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPortsString()));
        
        // 内存列
        TableColumn<ProcessInfo, String> memoryCol = new TableColumn<>("内存");
        memoryCol.setPrefWidth(100);
        memoryCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFormattedMemory()));
        memoryCol.setStyle("-fx-alignment: CENTER_RIGHT;");
        
        // 路径列
        TableColumn<ProcessInfo, String> pathCol = new TableColumn<>("路径/命令");
        pathCol.setPrefWidth(400);
        pathCol.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().getPath() != null ? data.getValue().getPath() : data.getValue().getCommandLine()
        ));
        
        // 状态列
        TableColumn<ProcessInfo, String> statusCol = new TableColumn<>("状态");
        statusCol.setPrefWidth(80);
        statusCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus()));
        
        processTable.getColumns().addAll(selectCol, pidCol, nameCol, portCol, memoryCol, pathCol, statusCol);
        
        // 双击结束进程
        processTable.setRowFactory(tv -> {
            TableRow<ProcessInfo> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    ProcessInfo process = row.getItem();
                    confirmKillProcess(process);
                }
            });
            return row;
        });
        
        // 选择变化监听
        processTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            updateButtonStates();
        });
        
        mainContent.getChildren().add(processTable);
        return mainContent;
    }
    
    private Node createStatusBar() {
        VBox statusBox = new VBox(5);
        statusBox.setPadding(new Insets(10));
        statusBox.setStyle("-fx-background-color: linear-gradient(to top, #ffffff, #f0f0f0); " +
                          "-fx-border-color: #d0d0d0; -fx-border-width: 1 0 0 0;");
        
        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(Double.MAX_VALUE);
        progressBar.setVisible(false);
        
        HBox statusLine = new HBox(15);
        statusLine.setAlignment(Pos.CENTER_LEFT);
        
        FontIcon statusIcon = new FontIcon(MaterialDesign.MDI_INFORMATION_OUTLINE);
        statusIcon.setIconSize(16);
        statusIcon.setStyle("-fx-icon-color: #666;");
        
        statusLabel = new Label("就绪");
        statusLabel.setStyle("-fx-text-fill: #666;");
        
        countLabel = new Label("进程数: 0");
        countLabel.setStyle("-fx-text-fill: #666; -fx-font-weight: bold;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        statusLine.getChildren().addAll(statusIcon, statusLabel, spacer, countLabel);
        
        statusBox.getChildren().addAll(progressBar, statusLine);
        
        return statusBox;
    }
    
    private void bindProperties() {
        statusLabel.textProperty().bind(service.statusMessageProperty());
        progressBar.progressProperty().bind(service.progressProperty());
    }
    
    private void loadProcessList() {
        progressBar.setVisible(true);
        processList.clear();
        allProcesses.clear();
        
        service.getProcessListAsync()
            .thenAccept(processes -> Platform.runLater(() -> {
                allProcesses.addAll(processes);
                processList.addAll(processes);
                updateCountLabel();
                progressBar.setVisible(false);
            }))
            .exceptionally(throwable -> {
                Platform.runLater(() -> {
                    showAlert("错误", "获取进程列表失败: " + throwable.getMessage(), Alert.AlertType.ERROR);
                    progressBar.setVisible(false);
                });
                return null;
            });
    }
    
    private void searchByPort() {
        String portsStr = portSearchField.getText().trim();
        if (portsStr.isEmpty()) {
            showAlert("提示", "请输入要查找的端口号", Alert.AlertType.WARNING);
            return;
        }
        
        progressBar.setVisible(true);
        processList.clear();
        
        service.findProcessesByPortsAsync(portsStr)
            .thenAccept(processes -> Platform.runLater(() -> {
                processList.addAll(processes);
                updateCountLabel();
                progressBar.setVisible(false);
                
                if (processes.isEmpty()) {
                    showAlert("提示", "未找到占用指定端口的进程", Alert.AlertType.INFORMATION);
                }
            }))
            .exceptionally(throwable -> {
                Platform.runLater(() -> {
                    showAlert("错误", "查找进程失败: " + throwable.getMessage(), Alert.AlertType.ERROR);
                    progressBar.setVisible(false);
                });
                return null;
            });
    }
    
    private void filterByName() {
        String keyword = nameSearchField.getText().trim().toLowerCase();
        if (keyword.isEmpty()) {
            processList.setAll(allProcesses);
        } else {
            List<ProcessInfo> filtered = allProcesses.stream()
                .filter(p -> p.getName() != null && p.getName().toLowerCase().contains(keyword))
                .collect(Collectors.toList());
            processList.setAll(filtered);
        }
        updateCountLabel();
    }
    
    private void clearFilter() {
        nameSearchField.clear();
        portSearchField.clear();
        processList.setAll(allProcesses);
        updateCountLabel();
    }
    
    private void toggleSelectAll() {
        boolean select = selectAllCheck.isSelected();
        for (ProcessInfo process : processList) {
            process.setSelected(select);
        }
        processTable.refresh();
        updateButtonStates();
    }
    
    private void killSelectedProcess() {
        ProcessInfo selected = processTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("提示", "请先选择一个进程", Alert.AlertType.WARNING);
            return;
        }
        confirmKillProcess(selected);
    }
    
    private void killSelectedProcesses() {
        List<Integer> selectedPids = processList.stream()
            .filter(ProcessInfo::isSelected)
            .map(ProcessInfo::getPid)
            .collect(Collectors.toList());
        
        if (selectedPids.isEmpty()) {
            showAlert("提示", "请先勾选要结束的进程", Alert.AlertType.WARNING);
            return;
        }
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("确认批量结束进程");
        confirm.setHeaderText("即将结束 " + selectedPids.size() + " 个进程");
        confirm.setContentText("确定要结束这些进程吗？此操作不可恢复。");
        
        confirm.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                progressBar.setVisible(true);
                service.killProcessesAsync(selectedPids)
                    .thenAccept(counts -> Platform.runLater(() -> {
                        int success = counts[0];
                        int fail = counts[1];
                        showAlert("完成", "成功结束 " + success + " 个进程，失败 " + fail + " 个", 
                                 fail > 0 ? Alert.AlertType.WARNING : Alert.AlertType.INFORMATION);
                        loadProcessList(); // 刷新列表
                        progressBar.setVisible(false);
                    }));
            }
        });
    }
    
    private void confirmKillProcess(ProcessInfo process) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("确认结束进程");
        confirm.setHeaderText("结束进程: " + process.getName());
        confirm.setContentText("PID: " + process.getPid() + "\n路径: " + process.getPath() + 
                              "\n\n确定要结束此进程吗？");
        
        confirm.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                progressBar.setVisible(true);
                service.killProcessAsync(process.getPid())
                    .thenAccept(success -> Platform.runLater(() -> {
                        if (success) {
                            showAlert("成功", "进程已结束", Alert.AlertType.INFORMATION);
                            loadProcessList(); // 刷新列表
                        } else {
                            showAlert("失败", "结束进程失败，可能需要管理员权限", Alert.AlertType.ERROR);
                        }
                        progressBar.setVisible(false);
                    }));
            }
        });
    }
    
    private void updateButtonStates() {
        ProcessInfo selected = processTable.getSelectionModel().getSelectedItem();
        long selectedCount = processList.stream().filter(ProcessInfo::isSelected).count();
        
        killBtn.setDisable(selected == null);
        killBatchBtn.setDisable(selectedCount == 0);
    }
    
    private void updateCountLabel() {
        countLabel.setText("进程数: " + processList.size());
    }
    
    private void showHelp() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("使用说明");
        alert.setHeaderText("进程管理器 - 使用指南");
        
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
        alert.setHeaderText("进程管理器");
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
}
