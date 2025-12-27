package io.github.lemostic.toolsuite.modules.search.es;

import io.github.lemostic.toolsuite.modules.search.es.service.EsQueryService;
import io.github.lemostic.toolsuite.modules.search.es.service.EsQueryService.ConnectionInfo;
import io.github.lemostic.toolsuite.modules.search.es.service.EsQueryService.QueryResult;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign.MaterialDesign;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ES数据查询界面
 */
public class EsQueryView extends BorderPane {
    
    private final EsQueryService service = new EsQueryService();
    
    // 连接配置
    private TextField hostField;
    private TextField portField;
    private TextField usernameField;
    private PasswordField passwordField;
    private CheckBox httpsCheck;
    private Button connectBtn;
    private Label connectionStatusLabel;
    
    // 查询配置
    private ComboBox<String> indexCombo;
    private TextArea queryArea;
    private Button executeBtn;
    private Button loadFieldsBtn;
    
    // 结果显示
    private TableView<Map<String, Object>> resultTable;
    private ObservableList<Map<String, Object>> resultData;
    private Label resultCountLabel;
    
    // 列选择
    private TableView<ColumnItem> columnTable;
    private ObservableList<ColumnItem> columnItems;
    
    // 状态栏
    private ProgressBar progressBar;
    private Label statusLabel;
    
    private ConnectionInfo currentConnection;
    private List<String> availableFields = new ArrayList<>();
    
    public EsQueryView() {
        initializeUI();
        bindProperties();
        setupDefaultQuery();
    }
    
    private void initializeUI() {
        // 顶部工具栏
        setTop(createToolbar());
        
        // 主内容区
        setCenter(createMainContent());
        
        // 底部状态栏
        setBottom(createStatusBar());
        
        setStyle("-fx-background-color: #f5f5f5;");
    }
    
    private Node createToolbar() {
        ToolBar toolbar = new ToolBar();
        toolbar.setStyle("-fx-background-color: linear-gradient(to bottom, #ffffff, #e8e8e8); " +
                        "-fx-border-color: #d0d0d0; -fx-border-width: 0 0 1 0;");
        
        Button helpBtn = new Button("使用说明", new FontIcon(MaterialDesign.MDI_HELP_CIRCLE));
        helpBtn.setOnAction(e -> showHelp());
        
        Button aboutBtn = new Button("关于", new FontIcon(MaterialDesign.MDI_INFORMATION));
        aboutBtn.setOnAction(e -> showAbout());
        
        toolbar.getItems().addAll(helpBtn, aboutBtn);
        
        return toolbar;
    }
    
    private Node createMainContent() {
        SplitPane splitPane = new SplitPane();
        splitPane.setOrientation(javafx.geometry.Orientation.VERTICAL);
        splitPane.setDividerPositions(0.4, 0.6);
        
        // 上部：连接和查询配置
        splitPane.getItems().add(createTopPanel());
        
        // 下部：结果显示和列选择
        splitPane.getItems().add(createBottomPanel());
        
        return splitPane;
    }
    
    private Node createTopPanel() {
        VBox vbox = new VBox(15);
        vbox.setPadding(new Insets(15));
        
        vbox.getChildren().addAll(
            createConnectionCard(),
            createQueryCard()
        );
        
        ScrollPane scrollPane = new ScrollPane(vbox);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #f5f5f5;");
        
        return scrollPane;
    }
    
    private Node createBottomPanel() {
        SplitPane bottomSplit = new SplitPane();
        bottomSplit.setDividerPositions(0.7);
        
        // 左侧：结果表格
        bottomSplit.getItems().add(createResultCard());
        
        // 右侧：列选择
        bottomSplit.getItems().add(createColumnCard());
        
        return bottomSplit;
    }
    
    private Node createConnectionCard() {
        VBox card = createCard("Elasticsearch连接", MaterialDesign.MDI_SERVER_NETWORK);
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
        grid.setPadding(new Insets(10));
        
        // Host
        Label hostLabel = new Label("主机:");
        hostLabel.setStyle("-fx-font-weight: bold;");
        hostField = new TextField("localhost");
        hostField.setPromptText("ES服务器地址");
        
        // Port
        Label portLabel = new Label("端口:");
        portLabel.setStyle("-fx-font-weight: bold;");
        portField = new TextField("9200");
        portField.setPromptText("端口号");
        portField.setPrefWidth(100);
        
        // HTTPS
        httpsCheck = new CheckBox("使用HTTPS");
        
        // Username
        Label userLabel = new Label("用户名:");
        userLabel.setStyle("-fx-font-weight: bold;");
        usernameField = new TextField();
        usernameField.setPromptText("可选");
        
        // Password
        Label passLabel = new Label("密码:");
        passLabel.setStyle("-fx-font-weight: bold;");
        passwordField = new PasswordField();
        passwordField.setPromptText("可选");
        
        // Connect button
        connectBtn = new Button("连接", new FontIcon(MaterialDesign.MDI_POWER_PLUG));
        connectBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        connectBtn.setOnAction(e -> connectToEs());
        
        connectionStatusLabel = new Label("未连接");
        connectionStatusLabel.setStyle("-fx-text-fill: #999;");
        
        grid.add(hostLabel, 0, 0);
        grid.add(hostField, 1, 0);
        grid.add(portLabel, 2, 0);
        grid.add(portField, 3, 0);
        grid.add(httpsCheck, 4, 0);
        
        grid.add(userLabel, 0, 1);
        grid.add(usernameField, 1, 1);
        grid.add(passLabel, 2, 1);
        grid.add(passwordField, 3, 1);
        
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_LEFT);
        buttonBox.getChildren().addAll(connectBtn, connectionStatusLabel);
        grid.add(buttonBox, 0, 2, 5, 1);
        
        card.getChildren().add(grid);
        
        return card;
    }
    
    private Node createQueryCard() {
        VBox card = createCard("查询配置", MaterialDesign.MDI_MAGNIFY);
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
        grid.setPadding(new Insets(10));
        
        // Index selection
        Label indexLabel = new Label("索引:");
        indexLabel.setStyle("-fx-font-weight: bold;");
        indexCombo = new ComboBox<>();
        indexCombo.setPromptText("选择索引");
        indexCombo.setPrefWidth(300);
        
        loadFieldsBtn = new Button("加载字段", new FontIcon(MaterialDesign.MDI_DOWNLOAD));
        loadFieldsBtn.setOnAction(e -> loadIndexFields());
        loadFieldsBtn.setDisable(true);
        
        grid.add(indexLabel, 0, 0);
        grid.add(indexCombo, 1, 0);
        grid.add(loadFieldsBtn, 2, 0);
        
        // Query JSON
        Label queryLabel = new Label("查询JSON:");
        queryLabel.setStyle("-fx-font-weight: bold;");
        queryArea = new TextArea();
        queryArea.setPromptText("输入Elasticsearch查询DSL...");
        queryArea.setPrefRowCount(8);
        queryArea.setStyle("-fx-font-family: 'Consolas', 'Monaco', monospace;");
        
        VBox queryBox = new VBox(5);
        queryBox.getChildren().addAll(queryLabel, queryArea);
        grid.add(queryBox, 0, 1, 3, 1);
        
        // Execute button
        executeBtn = new Button("执行查询", new FontIcon(MaterialDesign.MDI_PLAY));
        executeBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        executeBtn.setOnAction(e -> executeQuery());
        executeBtn.setDisable(true);
        
        resultCountLabel = new Label("");
        resultCountLabel.setStyle("-fx-text-fill: #666;");
        
        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER_LEFT);
        buttonBox.setPadding(new Insets(10, 0, 0, 0));
        buttonBox.getChildren().addAll(executeBtn, resultCountLabel);
        grid.add(buttonBox, 0, 2, 3, 1);
        
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setMinWidth(80);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(col1, col2);
        
        card.getChildren().add(grid);
        
        return card;
    }
    
    private Node createResultCard() {
        VBox card = createCard("查询结果", MaterialDesign.MDI_TABLE_LARGE);
        
        resultTable = new TableView<>();
        resultData = FXCollections.observableArrayList();
        resultTable.setItems(resultData);
        resultTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        resultTable.setPlaceholder(new Label("执行查询后结果将显示在此处..."));
        
        VBox.setVgrow(resultTable, Priority.ALWAYS);
        card.getChildren().add(resultTable);
        
        return card;
    }
    
    private Node createColumnCard() {
        VBox card = createCard("列选择与导出", MaterialDesign.MDI_VIEW_COLUMN);
        
        // 列选择表格
        columnTable = new TableView<>();
        columnItems = FXCollections.observableArrayList();
        columnTable.setItems(columnItems);
        columnTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        columnTable.setPrefHeight(200);
        columnTable.setPlaceholder(new Label("执行查询后可选择要导出的列..."));
        
        // 启用列
        TableColumn<ColumnItem, Boolean> enabledCol = new TableColumn<>("选择");
        enabledCol.setCellValueFactory(cellData -> cellData.getValue().selectedProperty());
        enabledCol.setCellFactory(CheckBoxTableCell.forTableColumn(enabledCol));
        enabledCol.setEditable(true);
        enabledCol.setMaxWidth(60);
        enabledCol.setMinWidth(60);
        
        // 列名
        TableColumn<ColumnItem, String> nameCol = new TableColumn<>("列名");
        nameCol.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        
        columnTable.getColumns().addAll(enabledCol, nameCol);
        columnTable.setEditable(true);
        
        // 按钮栏
        HBox btnBox = new HBox(10);
        btnBox.setPadding(new Insets(10, 0, 0, 0));
        
        Button selectAllBtn = new Button("全选", new FontIcon(MaterialDesign.MDI_CHECK_ALL));
        selectAllBtn.setOnAction(e -> selectAllColumns(true));
        
        Button deselectAllBtn = new Button("取消全选", new FontIcon(MaterialDesign.MDI_CHECKBOX_BLANK_OUTLINE));
        deselectAllBtn.setOnAction(e -> selectAllColumns(false));
        
        Button exportBtn = new Button("导出Excel", new FontIcon(MaterialDesign.MDI_FILE_EXCEL));
        exportBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
        exportBtn.setOnAction(e -> exportToExcel());
        
        btnBox.getChildren().addAll(selectAllBtn, deselectAllBtn, exportBtn);
        
        VBox.setVgrow(columnTable, Priority.ALWAYS);
        card.getChildren().addAll(columnTable, btnBox);
        
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
        statusLabel.textProperty().bind(service.statusMessageProperty());
        progressBar.progressProperty().bind(service.progressProperty());
    }
    
    private void setupDefaultQuery() {
        String defaultQuery = "{\n" +
                "  \"query\": {\n" +
                "    \"match_all\": {}\n" +
                "  },\n" +
                "  \"size\": 100\n" +
                "}";
        queryArea.setText(defaultQuery);
    }
    
    // Action handlers
    
    private void connectToEs() {
        try {
            String host = hostField.getText().trim();
            String portStr = portField.getText().trim();
            
            if (host.isEmpty() || portStr.isEmpty()) {
                showAlert("错误", "请输入主机和端口", Alert.AlertType.ERROR);
                return;
            }
            
            int port = Integer.parseInt(portStr);
            String username = usernameField.getText().trim();
            String password = passwordField.getText();
            boolean useHttps = httpsCheck.isSelected();
            
            currentConnection = new ConnectionInfo(host, port, 
                    username.isEmpty() ? null : username,
                    password.isEmpty() ? null : password,
                    useHttps);
            
            connectBtn.setDisable(true);
            connectionStatusLabel.setText("连接中...");
            connectionStatusLabel.setStyle("-fx-text-fill: #FFC107;");
            
            new Thread(() -> {
                try {
                    boolean success = service.testConnection(currentConnection);
                    
                    if (success) {
                        List<String> indices = service.getIndices(currentConnection);
                        
                        Platform.runLater(() -> {
                            connectionStatusLabel.setText("已连接");
                            connectionStatusLabel.setStyle("-fx-text-fill: #4CAF50;");
                            indexCombo.setItems(FXCollections.observableArrayList(indices));
                            loadFieldsBtn.setDisable(false);
                            executeBtn.setDisable(false);
                            connectBtn.setDisable(false);
                        });
                    } else {
                        Platform.runLater(() -> {
                            connectionStatusLabel.setText("连接失败");
                            connectionStatusLabel.setStyle("-fx-text-fill: #f44336;");
                            connectBtn.setDisable(false);
                        });
                    }
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        connectionStatusLabel.setText("连接失败: " + e.getMessage());
                        connectionStatusLabel.setStyle("-fx-text-fill: #f44336;");
                        showAlert("连接失败", e.getMessage(), Alert.AlertType.ERROR);
                        connectBtn.setDisable(false);
                    });
                }
            }).start();
            
        } catch (NumberFormatException e) {
            showAlert("错误", "端口必须是数字", Alert.AlertType.ERROR);
        }
    }
    
    private void loadIndexFields() {
        String index = indexCombo.getValue();
        if (index == null || index.isEmpty()) {
            showAlert("错误", "请选择索引", Alert.AlertType.WARNING);
            return;
        }
        
        new Thread(() -> {
            try {
                availableFields = service.getIndexFields(currentConnection, index);
                Platform.runLater(() -> {
                    showAlert("成功", String.format("加载了 %d 个字段", availableFields.size()), 
                             Alert.AlertType.INFORMATION);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showAlert("加载失败", e.getMessage(), Alert.AlertType.ERROR);
                });
            }
        }).start();
    }
    
    private void executeQuery() {
        String index = indexCombo.getValue();
        if (index == null || index.isEmpty()) {
            showAlert("错误", "请选择索引", Alert.AlertType.WARNING);
            return;
        }
        
        String queryJson = queryArea.getText().trim();
        if (queryJson.isEmpty()) {
            showAlert("错误", "请输入查询JSON", Alert.AlertType.WARNING);
            return;
        }
        
        executeBtn.setDisable(true);
        resultCountLabel.setText("查询中...");
        
        new Thread(() -> {
            try {
                QueryResult result = service.executeQuery(currentConnection, index, queryJson);
                
                Platform.runLater(() -> {
                    displayResults(result);
                    resultCountLabel.setText(String.format("总计: %d 条记录, 当前显示: %d 条", 
                            result.getTotal(), result.getDocuments().size()));
                    executeBtn.setDisable(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showAlert("查询失败", e.getMessage(), Alert.AlertType.ERROR);
                    executeBtn.setDisable(false);
                });
            }
        }).start();
    }
    
    private void displayResults(QueryResult result) {
        resultData.clear();
        resultTable.getColumns().clear();
        columnItems.clear();
        
        if (result.getDocuments().isEmpty()) {
            showAlert("提示", "没有查询到数据", Alert.AlertType.INFORMATION);
            return;
        }
        
        // 获取所有列名
        Set<String> allColumns = new LinkedHashSet<>();
        for (Map<String, Object> doc : result.getDocuments()) {
            allColumns.addAll(doc.keySet());
        }
        
        // 创建表格列
        for (String columnName : allColumns) {
            TableColumn<Map<String, Object>, Object> column = new TableColumn<>(columnName);
            column.setCellValueFactory(cellData -> {
                Object value = cellData.getValue().get(columnName);
                return new SimpleObjectProperty<>(value);
            });
            column.setCellFactory(col -> new TableCell<Map<String, Object>, Object>() {
                @Override
                protected void updateItem(Object item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(item.toString());
                    }
                }
            });
            column.setPrefWidth(150);
            resultTable.getColumns().add(column);
            
            // 添加到列选择器
            columnItems.add(new ColumnItem(columnName, true));
        }
        
        // 填充数据
        resultData.addAll(result.getDocuments());
    }
    
    private void selectAllColumns(boolean selected) {
        for (ColumnItem item : columnItems) {
            item.setSelected(selected);
        }
    }
    
    private void exportToExcel() {
        if (resultData.isEmpty()) {
            showAlert("错误", "没有数据可导出", Alert.AlertType.WARNING);
            return;
        }
        
        List<String> selectedColumns = columnItems.stream()
                .filter(ColumnItem::isSelected)
                .map(ColumnItem::getName)
                .collect(Collectors.toList());
        
        if (selectedColumns.isEmpty()) {
            showAlert("错误", "请至少选择一列", Alert.AlertType.WARNING);
            return;
        }
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("导出Excel文件");
        
        // 生成带索引名和时间戳的文件名
        String indexName = indexCombo.getValue();
        if (indexName == null || indexName.isEmpty()) {
            indexName = "unknown";
        }
        String timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = String.format("es_export_%s_%s.xlsx", indexName, timestamp);
        
        fileChooser.setInitialFileName(fileName);
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Excel Files", "*.xlsx")
        );
        
        File file = fileChooser.showSaveDialog(getScene().getWindow());
        if (file != null) {
            new Thread(() -> {
                try {
                    service.exportToExcel(new ArrayList<>(resultData), selectedColumns, file);
                    
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("导出成功");
                        alert.setHeaderText("数据已导出");
                        alert.setContentText(String.format("文件: %s\n\n总记录数: %d\n导出列数: %d",
                                file.getAbsolutePath(), resultData.size(), selectedColumns.size()));
                        
                        ButtonType openBtn = new ButtonType("打开文件夹");
                        alert.getButtonTypes().add(openBtn);
                        
                        Optional<ButtonType> response = alert.showAndWait();
                        if (response.isPresent() && response.get() == openBtn) {
                            try {
                                java.awt.Desktop.getDesktop().open(file.getParentFile());
                            } catch (Exception e) {
                                showAlert("错误", "无法打开文件夹: " + e.getMessage(), 
                                         Alert.AlertType.ERROR);
                            }
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        showAlert("导出失败", e.getMessage(), Alert.AlertType.ERROR);
                    });
                }
            }).start();
        }
    }
    
    private void showHelp() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("使用说明");
        alert.setHeaderText("ES数据查询工具 - 使用指南");
        
        String help = """
        🔍 功能说明:
        本工具用于连接Elasticsearch，执行查询并导出数据为Excel。
        
        🔧 使用步骤:
        1. 输入ES服务器连接信息（主机、端口、用户名/密码）
        2. 点击“连接”按钮，等待连接成功
        3. 从下拉列表中选择要查询的索引
        4. （可选）点击“加载字段”查看索引的字段信息
        5. 在查询区域输入Elasticsearch DSL查询语句
        6. 点击“执行查询”，结果将显示在下方表格中
        7. 在右侧列选择区选择要导出的列
        8. 点击“导出Excel”将数据导出为.xlsx文件
        
        📝 查询示例:
        基本查询：
        {
          "query": {
            "match_all": {}
          },
          "size": 100
        }
        
        条件查询：
        {
          "query": {
            "match": {
              "field_name": "search_value"
            }
          },
          "size": 100
        }
        
        范围查询：
        {
          "query": {
            "range": {
              "timestamp": {
                "gte": "2024-01-01",
                "lte": "2024-12-31"
              }
            }
          },
          "size": 1000
        }
        
        ⚠️ 注意事项:
        • 默认连接到本地ES（localhost:9200）
        • 如果需要认证，请输入用户名和密码
        • 查询结果数量由查询JSON中的 size 字段控制
        • 大量数据导出可能需要较长时间，请耐心等待
        """;
        
        TextArea textArea = new TextArea(help);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPrefHeight(500);
        
        alert.getDialogPane().setContent(textArea);
        alert.getDialogPane().setPrefWidth(700);
        alert.showAndWait();
    }
    
    private void showAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("关于");
        alert.setHeaderText("ES数据查询工具");
        alert.setContentText(
            "版本: 1.0.0\n" +
            "作者: lemostic\n" +
            "功能: 连接Elasticsearch执行查询，支持字段搜索、列筛选和Excel导出\n\n" +
            "© 2025 Tool Suite"
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
    
    /**
     * 列选择项
     */
    public static class ColumnItem {
        private final SimpleStringProperty name;
        private final SimpleBooleanProperty selected;
        
        public ColumnItem(String name, boolean selected) {
            this.name = new SimpleStringProperty(name);
            this.selected = new SimpleBooleanProperty(selected);
        }
        
        public String getName() {
            return name.get();
        }
        
        public void setName(String value) {
            name.set(value);
        }
        
        public SimpleStringProperty nameProperty() {
            return name;
        }
        
        public boolean isSelected() {
            return selected.get();
        }
        
        public void setSelected(boolean value) {
            selected.set(value);
        }
        
        public SimpleBooleanProperty selectedProperty() {
            return selected;
        }
    }
}