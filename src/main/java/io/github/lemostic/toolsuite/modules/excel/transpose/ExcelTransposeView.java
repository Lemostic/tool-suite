package io.github.lemostic.toolsuite.modules.excel.transpose;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign.MaterialDesign;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ExcelTransposeView extends BorderPane {

    private final ExcelTransposeService service = new ExcelTransposeService();
    
    private VBox dropZone;
    private Label dropLabel;
    private Button browseBtn;
    private ComboBox<String> sheetSelector;
    private ComboBox<Integer> previewRowCount;
    private ToggleButton transposeToggle;
    private Button loadBtn;
    private TableView<ObservableList<String>> dataTable;
    private VBox columnCopyPanel;
    private TextArea previewArea;
    private TextField prefixField;
    private TextField suffixField;
    private TextField separatorField;
    private Button copyBtn;
    private Label selectedColumnInfo;
    private File selectedFile;
    private List<List<String>> currentData;
    private boolean isTransposed;
    private int selectedColumnIndex = -1;
    private List<String> currentHeaders;

    public ExcelTransposeView() {
        initializeUI();
        bindProperties();
    }

    private void initializeUI() {
        setTop(createTopBar());
        setCenter(createMainContent());
        setBottom(createStatusBar());
        setStyle("-fx-background-color: #f5f5f5;");
    }

    private Node createTopBar() {
        HBox toolbar = new HBox(15);
        toolbar.setPadding(new Insets(12, 15, 12, 15));
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setStyle("-fx-background-color: white; " +
                        "-fx-border-color: #e0e0e0; " +
                        "-fx-border-width: 0 0 1 0; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 4, 0, 0, 1);");
        
        HBox fileBox = new HBox(10);
        fileBox.setAlignment(Pos.CENTER_LEFT);
        FontIcon fileIcon = new FontIcon(MaterialDesign.MDI_FILE_EXCEL);
        fileIcon.setIconSize(20);
        fileIcon.setStyle("-fx-icon-color: #4CAF50;");
        Label fileLabel = new Label("文件:");
        fileLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        
        browseBtn = new Button("选择文件", new FontIcon(MaterialDesign.MDI_FOLDER));
        browseBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 6 12;");
        browseBtn.setOnAction(e -> selectFile());
        
        fileBox.getChildren().addAll(fileIcon, fileLabel, browseBtn);
        
        Separator sep1 = new Separator();
        sep1.setOrientation(javafx.geometry.Orientation.VERTICAL);
        Region spacer1 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);
        
        HBox sheetBox = new HBox(8);
        sheetBox.setAlignment(Pos.CENTER_LEFT);
        Label sheetLabel = new Label("Sheet:");
        sheetLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        sheetSelector = new ComboBox<>();
        sheetSelector.setPrefWidth(150);
        sheetSelector.setDisable(true);
        sheetSelector.setPromptText("选择工作表");
        sheetSelector.setOnAction(e -> loadCurrentSheet());
        sheetBox.getChildren().addAll(sheetLabel, sheetSelector);
        
        Separator sep2 = new Separator();
        sep2.setOrientation(javafx.geometry.Orientation.VERTICAL);
        
        HBox previewBox = new HBox(8);
        previewBox.setAlignment(Pos.CENTER_LEFT);
        Label previewLabel = new Label("预览:");
        previewLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        previewRowCount = new ComboBox<>();
        previewRowCount.getItems().addAll(10, 20, 50, 100, 1000, -1);
        previewRowCount.setCellFactory(lv -> new javafx.scene.control.ListCell<Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("");
                } else {
                    setText(item == -1 ? "全部" : String.valueOf(item) + " 行");
                }
            }
        });
        previewRowCount.setButtonCell(new javafx.scene.control.ListCell<Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("");
                } else {
                    setText(item == -1 ? "全部" : String.valueOf(item) + " 行");
                }
            }
        });
        previewRowCount.setValue(10);
        previewRowCount.setPrefWidth(100);
        previewBox.getChildren().addAll(previewLabel, previewRowCount);
        
        Separator sep3 = new Separator();
        sep3.setOrientation(javafx.geometry.Orientation.VERTICAL);
        
        transposeToggle = new ToggleButton("行→列", new FontIcon(MaterialDesign.MDI_SWAP_HORIZONTAL));
        transposeToggle.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 6 12;");
        transposeToggle.setDisable(true);
        transposeToggle.setOnAction(e -> {
            if (transposeToggle.isSelected()) {
                transposeToggle.setText("列→行");
            } else {
                transposeToggle.setText("行→列");
            }
            applyTranspose();
        });
        
        loadBtn = new Button("加载数据", new FontIcon(MaterialDesign.MDI_DATABASE_PLUS));
        loadBtn.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 6 12;");
        loadBtn.setDisable(true);
        loadBtn.setOnAction(e -> loadCurrentSheet());
        
        toolbar.getChildren().addAll(fileBox, sep1, sheetBox, sep2, previewBox, sep3, transposeToggle, loadBtn);
        
        return toolbar;
    }

    private Node createMainContent() {
        HBox mainContent = new HBox(0);
        
        VBox tableArea = createCard("数据预览", MaterialDesign.MDI_TABLE);
        tableArea.setPadding(new Insets(15));
        
        dropZone = new VBox(10);
        dropZone.setAlignment(Pos.CENTER);
        dropZone.setPrefHeight(100);
        dropZone.setStyle("-fx-border-color: #4CAF50; -fx-border-style: dashed; -fx-border-width: 2; " +
                         "-fx-background-color: #f9fff9; -fx-background-radius: 8; -fx-border-radius: 8;");
        
        FontIcon dropIcon = new FontIcon(MaterialDesign.MDI_CLOUD_UPLOAD);
        dropIcon.setIconSize(32);
        dropIcon.setStyle("-fx-icon-color: #4CAF50;");
        
        dropLabel = new Label("拖拽Excel文件到此处\n或点击「选择文件」按钮");
        dropLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 14px;");
        dropLabel.setWrapText(true);
        dropLabel.setAlignment(Pos.CENTER);
        
        dropZone.getChildren().addAll(dropIcon, dropLabel);
        dropZone.setOnDragOver(this::handleDragOver);
        dropZone.setOnDragDropped(this::handleDragDropped);
        
        dataTable = new TableView<>();
        dataTable.setEditable(false);
        dataTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        dataTable.setPlaceholder(new Label("暂无数据，请先选择并加载Excel文件"));
        dataTable.setPrefHeight(400);
        dataTable.setStyle("-fx-font-size: 12px;");
        
        dataTable.getSelectionModel().setCellSelectionEnabled(true);
        
        dataTable.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.SECONDARY) {
                var cell = (TableCell<?, ?>) e.getTarget();
                if (cell != null && cell.getTableColumn() != null) {
                    int colIndex = dataTable.getColumns().indexOf(cell.getTableColumn());
                    selectedColumnIndex = colIndex;
                    showColumnContextMenu(e, colIndex);
                }
            }
        });
        
        dataTable.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            updateColumnCopyPanel();
        });
        
        tableArea.getChildren().addAll(dropZone, dataTable);
        HBox.setHgrow(tableArea, Priority.ALWAYS);
        
        columnCopyPanel = createColumnCopyPanel();
        columnCopyPanel.setVisible(false);
        columnCopyPanel.setManaged(false);
        
        mainContent.getChildren().addAll(tableArea, columnCopyPanel);
        
        return mainContent;
    }

    private VBox createColumnCopyPanel() {
        VBox panel = createCard("列拼接工具", MaterialDesign.MDI_TEXTBOX);
        panel.setPrefWidth(320);
        panel.setMinWidth(320);
        panel.setPadding(new Insets(15));
        panel.setStyle(panel.getStyle() + "-fx-border-color: #2196F3;");
        
        selectedColumnInfo = new Label("未选择列");
        selectedColumnInfo.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #2196F3;");
        
        GridPane configGrid = new GridPane();
        configGrid.setHgap(8);
        configGrid.setVgap(8);
        configGrid.setPadding(new Insets(8, 0, 8, 0));
        
        Label prefixLabel = new Label("前缀:");
        prefixLabel.setStyle("-fx-font-weight: bold;");
        prefixField = new TextField("'");
        prefixField.setPrefWidth(80);
        prefixField.textProperty().addListener((obs, old, newVal) -> updatePreviewArea());
        
        Label suffixLabel = new Label("后缀:");
        suffixLabel.setStyle("-fx-font-weight: bold;");
        suffixField = new TextField("'");
        suffixField.setPrefWidth(80);
        suffixField.textProperty().addListener((obs, old, newVal) -> updatePreviewArea());
        
        Label sepLabel = new Label("分隔符:");
        sepLabel.setStyle("-fx-font-weight: bold;");
        separatorField = new TextField(",");
        separatorField.textProperty().addListener((obs, old, newVal) -> updatePreviewArea());
        
        configGrid.add(prefixLabel, 0, 0);
        configGrid.add(prefixField, 1, 0);
        configGrid.add(suffixLabel, 2, 0);
        configGrid.add(suffixField, 3, 0);
        configGrid.add(sepLabel, 0, 1);
        configGrid.add(separatorField, 1, 1, 3, 1);
        
        Label previewLabel = new Label("拼接预览:");
        previewLabel.setStyle("-fx-font-weight: bold; -fx-padding: 8 0 4 0;");
        
        previewArea = new TextArea();
        previewArea.setPrefRowCount(6);
        previewArea.setWrapText(true);
        previewArea.setEditable(false);
        previewArea.setStyle("-fx-font-family: 'Consolas', 'Monaco', monospace; -fx-font-size: 11px;");
        
        copyBtn = new Button("复制结果", new FontIcon(MaterialDesign.MDI_CONTENT_COPY));
        copyBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 16;");
        copyBtn.setPrefWidth(Double.MAX_VALUE);
        copyBtn.setOnAction(e -> copyResult());
        copyBtn.setDisable(true);
        
        panel.getChildren().addAll(selectedColumnInfo, configGrid, previewLabel, previewArea, copyBtn);
        
        return panel;
    }

    private VBox createCard(String title, MaterialDesign icon) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: white; " +
                     "-fx-border-color: #e0e0e0; " +
                     "-fx-border-radius: 8; " +
                     "-fx-background-radius: 8; " +
                     "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 6, 0, 0, 2);");
        card.setPadding(new Insets(15));
        
        HBox titleBox = new HBox(8);
        titleBox.setAlignment(Pos.CENTER_LEFT);
        
        FontIcon titleIcon = new FontIcon(icon);
        titleIcon.setIconSize(18);
        titleIcon.setStyle("-fx-icon-color: #2196F3;");
        
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #333;");
        
        titleBox.getChildren().addAll(titleIcon, titleLabel);
        
        Separator separator = new Separator();
        
        card.getChildren().addAll(titleBox, separator);
        
        return card;
    }

    private Node createStatusBar() {
        HBox statusBar = new HBox(15);
        statusBar.setPadding(new Insets(8, 15, 8, 15));
        statusBar.setStyle("-fx-background-color: white; " +
                          "-fx-border-color: #e0e0e0; " +
                          "-fx-border-width: 1 0 0 0;");

        FontIcon statusIcon = new FontIcon(MaterialDesign.MDI_INFORMATION_OUTLINE);
        statusIcon.setIconSize(14);
        statusIcon.setStyle("-fx-icon-color: #666;");
        
        Label statusLabel = new Label("就绪");
        statusLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");
        statusLabel.textProperty().bind(service.statusMessageProperty());
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Label rowCountLabel = new Label();
        rowCountLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 12px;");
        rowCountLabel.textProperty().bind(
            service.totalRowsProperty().asString("总行数: %d").concat("  |  ").concat(
            service.totalColsProperty().asString("总列数: %d"))
        );
        
        statusBar.getChildren().addAll(statusIcon, statusLabel, spacer, rowCountLabel);
        
        return statusBar;
    }

    private void bindProperties() {
    }

    private void selectFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择Excel文件");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Excel Files", "*.xlsx", "*.xls"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        File file = fileChooser.showOpenDialog(getScene().getWindow());
        if (file != null) {
            handleFile(file);
        }
    }

    private void handleFile(File file) {
        selectedFile = file;
        dropLabel.setText("已选择: " + file.getName());
        
        CompletableFuture.runAsync(() -> {
            try {
                String[] sheetNames = service.getSheetNames(file);
                Platform.runLater(() -> {
                    sheetSelector.getItems().clear();
                    if (sheetNames != null && sheetNames.length > 0) {
                        sheetSelector.getItems().addAll(sheetNames);
                        sheetSelector.setDisable(false);
                        sheetSelector.getSelectionModel().select(0);
                        loadBtn.setDisable(false);
                    } else {
                        showAlert("提示", "该Excel文件中没有可用的工作表", Alert.AlertType.WARNING);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showAlert("错误", "无法读取Excel文件: " + e.getMessage(), Alert.AlertType.ERROR);
                    selectedFile = null;
                });
            }
        });
    }

    private void loadCurrentSheet() {
        if (selectedFile == null) return;
        
        String selectedSheet = sheetSelector.getValue();
        if (selectedSheet == null || selectedSheet.isEmpty()) {
            showAlert("错误", "请选择要加载的工作表", Alert.AlertType.WARNING);
            return;
        }
        
        loadBtn.setDisable(true);
        browseBtn.setDisable(true);
        sheetSelector.setDisable(true);
        
        int maxRows = previewRowCount.getValue();
        
        CompletableFuture.runAsync(() -> {
            try {
                currentData = service.readSheetData(selectedFile, selectedSheet, maxRows);
                currentHeaders = currentData.isEmpty() ? new ArrayList<>() : new ArrayList<>(currentData.get(0));
                
                Platform.runLater(() -> {
                    buildTable(currentData, false);
                    transposeToggle.setDisable(false);
                    isTransposed = false;
                    transposeToggle.setSelected(false);
                    transposeToggle.setText("行→列");
                    
                    loadBtn.setDisable(false);
                    browseBtn.setDisable(false);
                    sheetSelector.setDisable(false);
                    
                    columnCopyPanel.setVisible(false);
                    columnCopyPanel.setManaged(false);
                    selectedColumnIndex = -1;
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showAlert("加载失败", "加载数据时发生错误: " + e.getMessage(), Alert.AlertType.ERROR);
                    loadBtn.setDisable(false);
                    browseBtn.setDisable(false);
                    sheetSelector.setDisable(false);
                });
            }
        });
    }

    private void applyTranspose() {
        if (currentData == null || currentData.isEmpty()) return;
        
        if (isTransposed) {
            try {
                int maxRows = previewRowCount.getValue();
                currentData = service.readSheetData(selectedFile, sheetSelector.getValue(), maxRows);
                buildTable(currentData, false);
                isTransposed = false;
            } catch (Exception e) {
                showAlert("错误", "还原数据失败: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        } else {
            List<List<String>> transposed = service.transposeData(currentData);
            buildTable(transposed, true);
            isTransposed = true;
        }
        
        columnCopyPanel.setVisible(false);
        columnCopyPanel.setManaged(false);
        selectedColumnIndex = -1;
    }

    private void buildTable(List<List<String>> data, boolean transposed) {
        dataTable.getColumns().clear();
        dataTable.getItems().clear();
        
        if (data.isEmpty()) {
            return;
        }
        
        int colCount = data.get(0).size();
        
        for (int i = 0; i < colCount; i++) {
            final int colIndex = i;
            TableColumn<ObservableList<String>, String> column = new TableColumn<>();
            
            String header = "列 " + (i + 1);
            if (!data.isEmpty() && transposed) {
                header = data.get(0).get(i);
            }
            column.setText(header);
            
            column.setCellValueFactory(param -> {
                ObservableList<String> row = param.getValue();
                if (colIndex < row.size()) {
                    return new SimpleStringProperty(row.get(colIndex));
                }
                return new SimpleStringProperty("");
            });
            
            dataTable.getColumns().add(column);
        }
        
        int startRow = transposed ? 1 : 0;
        ObservableList<ObservableList<String>> items = FXCollections.observableArrayList();
        for (int i = startRow; i < data.size(); i++) {
            items.add(FXCollections.observableArrayList(data.get(i)));
        }
        dataTable.setItems(items);
    }

    private void showColumnContextMenu(javafx.scene.input.MouseEvent event, int colIndex) {
        ContextMenu contextMenu = new ContextMenu();
        
        MenuItem copyItem = new MenuItem("复制");
        copyItem.setOnAction(e -> copyColumnValues(colIndex, false));
        
        MenuItem copyWithHeader = new MenuItem("复制（带标题）");
        copyWithHeader.setOnAction(e -> copyColumnValues(colIndex, true));
        
        MenuItem customCopy = new MenuItem("列拼接复制");
        customCopy.setOnAction(e -> showColumnCopyPanel(colIndex));
        
        contextMenu.getItems().addAll(copyItem, copyWithHeader, customCopy);
        contextMenu.show(dataTable, event.getScreenX(), event.getScreenY());
    }

    private void copyColumnValues(int colIndex, boolean includeHeader) {
        if (currentData == null || currentData.isEmpty()) return;
        
        List<String> values = service.getColumnData(currentData, colIndex);
        if (!includeHeader && values.size() > 1) {
            values = values.subList(1, values.size());
        }
        
        String text = String.join("\n", values);
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        clipboard.setContent(content);
    }

    private void showColumnCopyPanel(int colIndex) {
        selectedColumnIndex = colIndex;
        columnCopyPanel.setVisible(true);
        columnCopyPanel.setManaged(true);
        
        String header = "列 " + (colIndex + 1);
        if (!currentData.isEmpty() && colIndex < currentData.get(0).size()) {
            String firstValue = currentData.get(0).get(colIndex);
            if (firstValue != null && !firstValue.isEmpty()) {
                header = firstValue;
            }
        }
        selectedColumnInfo.setText("当前列: " + header);
        
        updatePreviewArea();
    }

    private void updateColumnCopyPanel() {
    }

    private void updatePreviewArea() {
        if (selectedColumnIndex < 0 || currentData == null || currentData.isEmpty()) {
            previewArea.setText("");
            copyBtn.setDisable(true);
            return;
        }
        
        List<String> columnData = service.getColumnData(currentData, selectedColumnIndex);
        String prefix = prefixField.getText();
        String suffix = suffixField.getText();
        String separator = separatorField.getText();
        
        String result = service.joinColumnData(columnData, prefix, suffix, separator);
        previewArea.setText(result);
        copyBtn.setDisable(false);
    }

    private void copyResult() {
        String text = previewArea.getText();
        if (text != null && !text.isEmpty()) {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(text);
            clipboard.setContent(content);
        }
    }

    private void handleDragOver(DragEvent event) {
        if (event.getGestureSource() != dropZone && 
            event.getDragboard().hasFiles()) {
            event.acceptTransferModes(TransferMode.COPY);
        }
        event.consume();
    }

    private void handleDragDropped(DragEvent event) {
        Dragboard db = event.getDragboard();
        boolean success = false;
        
        if (db.hasFiles()) {
            File file = db.getFiles().get(0);
            String fileName = file.getName().toLowerCase();
            
            if (fileName.endsWith(".xlsx") || fileName.endsWith(".xls")) {
                handleFile(file);
                success = true;
            } else {
                Platform.runLater(() -> {
                    showAlert("错误", "请选择Excel文件（.xlsx 或 .xls）", Alert.AlertType.ERROR);
                });
            }
        }
        
        event.setDropCompleted(success);
        event.consume();
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
