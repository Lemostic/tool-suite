package io.github.lemostic.toolsuite.modules.excel.json;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign.MaterialDesign;

import java.io.File;
import java.util.concurrent.CompletableFuture;

import io.github.lemostic.toolsuite.util.ResourceLoader;

public class ExcelToJSONView extends BorderPane {

    private final ExcelToJSONService service = new ExcelToJSONService();
    
    // UI Components
    private TextArea jsonOutputArea;
    private Label statusLabel;
    private ProgressBar progressBar;
    private VBox dropZone;
    private Label dropLabel;
    private Button browseBtn;
    private Button convertBtn;
    private Button copyBtn;
    private Button saveBtn;
    private ComboBox<String> sheetSelector;
    private CheckBox headerRowCheck;
    private CheckBox formatJsonCheck;
    
    private File selectedExcelFile;

    public ExcelToJSONView() {
        initializeUI();
        bindProperties();
    }

    private void initializeUI() {
        // Top toolbar
        setTop(createToolbar());
        
        // Center content
        setCenter(createMainContent());
        
        // Bottom status bar
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
        VBox mainContent = new VBox(15);
        mainContent.setPadding(new Insets(20));
        
        // File selection area
        mainContent.getChildren().add(createFileSelectionCard());
        
        // Options area
        mainContent.getChildren().add(createOptionsCard());
        
        // JSON output area
        mainContent.getChildren().add(createOutputCard());
        
        return mainContent;
    }

    private Node createFileSelectionCard() {
        VBox card = createCard("Excel文件选择", MaterialDesign.MDI_FILE_EXCEL);
        
        dropZone = new VBox(10);
        dropZone.setAlignment(Pos.CENTER);
        dropZone.setPrefHeight(120);
        dropZone.setStyle("-fx-border-color: #cccccc; -fx-border-style: dashed; -fx-border-width: 2; " +
                         "-fx-background-color: #fafafa; -fx-background-radius: 5; -fx-border-radius: 5;");
        
        dropLabel = new Label("拖拽Excel文件到此处\n或点击下方按钮选择文件");
        dropLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 14px;");
        dropLabel.setWrapText(true);
        dropLabel.setAlignment(Pos.CENTER);
        
        browseBtn = new Button("选择Excel文件", new FontIcon(MaterialDesign.MDI_FILE_EXCEL));
        browseBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-weight: bold;");
        browseBtn.setOnAction(e -> selectExcelFile());
        
        dropZone.getChildren().addAll(dropLabel, browseBtn);
        
        // Enable drag and drop
        dropZone.setOnDragOver(this::handleDragOver);
        dropZone.setOnDragDropped(this::handleDragDropped);
        
        card.getChildren().add(dropZone);
        
        return card;
    }

    private Node createOptionsCard() {
        VBox card = createCard("转换选项", MaterialDesign.MDI_SETTINGS);
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));
        
        // Sheet selection
        Label sheetLabel = new Label("工作表:");
        sheetLabel.setStyle("-fx-font-weight: bold;");
        sheetSelector = new ComboBox<>();
        sheetSelector.setPrefWidth(200);
        sheetSelector.setDisable(true);
        sheetSelector.setPromptText("选择工作表");
        
        // Header row option
        headerRowCheck = new CheckBox("首行为列标题");
        headerRowCheck.setSelected(true);
        headerRowCheck.setTooltip(new Tooltip("勾选此项将把Excel的第一行作为JSON对象的键名"));
        
        // Format JSON option
        formatJsonCheck = new CheckBox("格式化JSON");
        formatJsonCheck.setSelected(true);
        formatJsonCheck.setTooltip(new Tooltip("勾选此项将美化输出的JSON格式"));
        
        grid.add(sheetLabel, 0, 0);
        grid.add(sheetSelector, 1, 0);
        grid.add(headerRowCheck, 2, 0);
        grid.add(formatJsonCheck, 3, 0);
        
        card.getChildren().add(grid);
        
        return card;
    }

    private Node createOutputCard() {
        VBox card = createCard("JSON输出", MaterialDesign.MDI_FILE_DOCUMENT);
        
        jsonOutputArea = new TextArea();
        jsonOutputArea.setPrefRowCount(15);
        jsonOutputArea.setWrapText(true);
        jsonOutputArea.setStyle("-fx-font-family: 'Consolas', 'Monaco', monospace; -fx-font-size: 12px;");
        jsonOutputArea.setEditable(false);
        
        // Buttons
        HBox buttonBox = new HBox(10);
        buttonBox.setPadding(new Insets(10, 0, 0, 0));
        buttonBox.setAlignment(Pos.CENTER_LEFT);
        
        convertBtn = new Button("转换为JSON", new FontIcon(MaterialDesign.MDI_DOWNLOAD));
        convertBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
        convertBtn.setOnAction(e -> convertExcelToJSON());
        convertBtn.setDisable(true);
        
        copyBtn = new Button("复制JSON", new FontIcon(MaterialDesign.MDI_CONTENT_COPY));
        copyBtn.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-font-weight: bold;");
        copyBtn.setOnAction(e -> copyJSONToClipboard());
        copyBtn.setDisable(true);
        
        saveBtn = new Button("保存JSON", new FontIcon(MaterialDesign.MDI_CONTENT_SAVE));
        saveBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-weight: bold;");
        saveBtn.setOnAction(e -> saveJSONToFile());
        saveBtn.setDisable(true);
        
        buttonBox.getChildren().addAll(convertBtn, copyBtn, saveBtn);
        
        card.getChildren().addAll(jsonOutputArea, buttonBox);
        
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

    private void selectExcelFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择Excel文件");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Excel Files", "*.xlsx", "*.xls"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        File file = fileChooser.showOpenDialog(getScene().getWindow());
        if (file != null) {
            handleExcelFile(file);
        }
    }

    private void handleExcelFile(File file) {
        if (file != null) {
            selectedExcelFile = file;
            dropLabel.setText("已选择: " + file.getName());
            convertBtn.setDisable(false);
            
            // Load sheet names in background thread
            new Thread(() -> {
                try {
                    String[] sheetNames = service.getSheetNames(file);
                    Platform.runLater(() -> {
                        sheetSelector.getItems().clear();
                        if (sheetNames != null && sheetNames.length > 0) {
                            sheetSelector.getItems().addAll(sheetNames);
                            sheetSelector.setDisable(false);
                            sheetSelector.getSelectionModel().select(0);
                        } else {
                            sheetSelector.setDisable(true);
                            showAlert("提示", "该Excel文件中没有可用的工作表", Alert.AlertType.WARNING);
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        showAlert("错误", "无法读取Excel文件: " + e.getMessage(), Alert.AlertType.ERROR);
                        selectedExcelFile = null;
                        dropLabel.setText("拖拽Excel文件到此处\n或点击下方按钮选择文件");
                        convertBtn.setDisable(true);
                    });
                }
            }).start();
        }
    }

    private void convertExcelToJSON() {
        if (selectedExcelFile == null) {
            showAlert("错误", "请先选择Excel文件", Alert.AlertType.WARNING);
            return;
        }

        String selectedSheet = sheetSelector.getValue();
        if (selectedSheet == null || selectedSheet.isEmpty()) {
            showAlert("错误", "请选择要转换的工作表", Alert.AlertType.WARNING);
            return;
        }

        // Disable controls during conversion
        convertBtn.setDisable(true);
        browseBtn.setDisable(true);
        copyBtn.setDisable(true);
        saveBtn.setDisable(true);
        
        // Clear previous output
        jsonOutputArea.setText("");
        
        // Run conversion in background thread
        CompletableFuture.runAsync(() -> {
            try {
                String jsonResult = service.convertExcelToJSON(
                    selectedExcelFile, 
                    selectedSheet, 
                    headerRowCheck.isSelected(),
                    formatJsonCheck.isSelected()
                );
                
                Platform.runLater(() -> {
                    jsonOutputArea.setText(jsonResult);
                    copyBtn.setDisable(false);
                    saveBtn.setDisable(false);
                    convertBtn.setDisable(false);
                    browseBtn.setDisable(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showAlert("转换失败", "转换过程中发生错误: " + e.getMessage(), Alert.AlertType.ERROR);
                    convertBtn.setDisable(false);
                    browseBtn.setDisable(false);
                });
            }
        });
    }

    private void copyJSONToClipboard() {
        String jsonText = jsonOutputArea.getText();
        if (jsonText != null && !jsonText.isEmpty()) {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(jsonText);
            clipboard.setContent(content);
            statusLabel.setText("JSON已复制到剪贴板");
        } else {
            statusLabel.setText("没有可复制的JSON内容");
        }
    }
    
    private void saveJSONToFile() {
        String jsonText = jsonOutputArea.getText();
        if (jsonText == null || jsonText.isEmpty()) {
            statusLabel.setText("没有可保存的JSON内容");
            return;
        }
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("保存JSON文件");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("JSON Files", "*.json"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        
        // 设置默认文件名
        if (selectedExcelFile != null) {
            String baseName = selectedExcelFile.getName();
            if (baseName.lastIndexOf('.') > 0) {
                baseName = baseName.substring(0, baseName.lastIndexOf('.'));
            }
            fileChooser.setInitialFileName(baseName + ".json");
        }
        
        File file = fileChooser.showSaveDialog(getScene().getWindow());
        if (file != null) {
            try {
                java.nio.file.Files.write(file.toPath(), jsonText.getBytes("UTF-8"));
                statusLabel.setText("JSON文件已保存至: " + file.getAbsolutePath());
            } catch (Exception e) {
                showAlert("保存失败", "保存JSON文件时发生错误: " + e.getMessage(), Alert.AlertType.ERROR);
            }
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
                handleExcelFile(file);
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

    private void showHelp() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("使用说明");
        alert.setHeaderText("Excel转JSON工具 - 使用指南");
        
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
        alert.setHeaderText("Excel转JSON工具");
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