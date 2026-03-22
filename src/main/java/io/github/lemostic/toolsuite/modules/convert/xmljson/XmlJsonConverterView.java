package io.github.lemostic.toolsuite.modules.convert.xmljson;

import io.github.lemostic.toolsuite.util.ResourceLoader;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign.MaterialDesign;

import java.io.File;
import java.nio.file.Files;

/**
 * XML/JSON转换器视图
 * 提供美观的UI界面，支持双向转换
 */
public class XmlJsonConverterView extends BorderPane {

    private final XmlJsonConverterService service = new XmlJsonConverterService();

    // UI组件
    private TextArea inputArea;
    private TextArea outputArea;
    private Label statusLabel;
    private ProgressBar progressBar;
    private CheckBox formatCheck;
    private Button xmlToJsonBtn;
    private Button jsonToXmlBtn;
    private Button copyBtn;
    private Button clearBtn;
    private Button loadFileBtn;
    private Button saveFileBtn;
    private Label inputTypeLabel;
    private Label outputTypeLabel;

    public XmlJsonConverterView() {
        initializeUI();
        bindProperties();
    }

    private void initializeUI() {
        // 顶部工具栏
        setTop(createToolbar());

        // 中心内容区
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
        VBox mainContent = new VBox(15);
        mainContent.setPadding(new Insets(20));

        // 选项卡片
        mainContent.getChildren().add(createOptionsCard());

        // 转换区域（左右布局）
        mainContent.getChildren().add(createConversionArea());

        // 操作按钮区域
        mainContent.getChildren().add(createActionButtonsArea());

        return mainContent;
    }

    private Node createOptionsCard() {
        VBox card = createCard("转换选项", MaterialDesign.MDI_SETTINGS);

        HBox optionsBox = new HBox(20);
        optionsBox.setAlignment(Pos.CENTER_LEFT);
        optionsBox.setPadding(new Insets(5));

        // 格式化选项
        formatCheck = new CheckBox("格式化输出");
        formatCheck.setSelected(true);
        formatCheck.setTooltip(new Tooltip("勾选此项将美化输出格式，使其更易阅读"));
        formatCheck.setStyle("-fx-font-size: 13px;");

        optionsBox.getChildren().addAll(formatCheck);

        card.getChildren().add(optionsBox);

        return card;
    }

    private Node createConversionArea() {
        HBox conversionArea = new HBox(15);
        conversionArea.setAlignment(Pos.CENTER);

        // 左侧输入区域
        VBox inputBox = createInputOutputBox("输入", MaterialDesign.MDI_FILE_IMPORT, true);

        // 中间转换按钮区域
        VBox convertButtonsBox = createConvertButtonsBox();

        // 右侧输出区域
        VBox outputBox = createInputOutputBox("输出", MaterialDesign.MDI_FILE_EXPORT, false);

        // 设置各区域比例
        HBox.setHgrow(inputBox, Priority.ALWAYS);
        HBox.setHgrow(outputBox, Priority.ALWAYS);

        conversionArea.getChildren().addAll(inputBox, convertButtonsBox, outputBox);

        return conversionArea;
    }

    private VBox createInputOutputBox(String title, MaterialDesign icon, boolean isInput) {
        VBox box = new VBox(10);
        box.setStyle("-fx-background-color: white; " +
                    "-fx-border-color: #e0e0e0; " +
                    "-fx-border-radius: 5; " +
                    "-fx-background-radius: 5; " +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);");
        box.setPadding(new Insets(15));

        // 标题栏
        HBox titleBox = new HBox(8);
        titleBox.setAlignment(Pos.CENTER_LEFT);

        FontIcon titleIcon = new FontIcon(icon);
        titleIcon.setIconSize(20);
        titleIcon.setStyle("-fx-icon-color: #2196F3;");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #333;");

        // 类型标签
        Label typeLabel = new Label(isInput ? "XML/JSON" : "结果");
        typeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666; -fx-background-color: #e3f2fd; " +
                          "-fx-padding: 2 8 2 8; -fx-background-radius: 10;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        if (isInput) {
            inputTypeLabel = typeLabel;
        } else {
            outputTypeLabel = typeLabel;
        }

        titleBox.getChildren().addAll(titleIcon, titleLabel, spacer, typeLabel);

        Separator separator = new Separator();

        // 文本区域
        TextArea textArea = new TextArea();
        textArea.setPrefRowCount(18);
        textArea.setWrapText(false);
        textArea.setStyle("-fx-font-family: 'Consolas', 'Monaco', 'Courier New', monospace; " +
                         "-fx-font-size: 13px; " +
                         "-fx-background-color: #fafafa;");

        if (isInput) {
            inputArea = textArea;
            inputArea.setPromptText("在此输入XML或JSON内容...\n\n" +
                                   "示例XML:\n<user>\n  <name>张三</name>\n  <age>25</age>\n</user>\n\n" +
                                   "示例JSON:\n{\n  \"name\": \"张三\",\n  \"age\": 25\n}");
            // 添加文本变化监听，自动检测类型
            inputArea.textProperty().addListener((obs, oldVal, newVal) -> autoDetectType(newVal));
        } else {
            outputArea = textArea;
            outputArea.setPromptText("转换结果将显示在这里...");
            outputArea.setEditable(false);
        }

        box.getChildren().addAll(titleBox, separator, textArea);

        return box;
    }

    private VBox createConvertButtonsBox() {
        VBox box = new VBox(15);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(0, 5, 0, 5));

        // XML转JSON按钮
        xmlToJsonBtn = new Button();
        xmlToJsonBtn.setGraphic(createButtonContent("XML → JSON", MaterialDesign.MDI_ARROW_RIGHT_BOLD_CIRCLE));
        xmlToJsonBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; " +
                             "-fx-padding: 12 20; -fx-font-size: 13px;");
        xmlToJsonBtn.setTooltip(new Tooltip("将XML转换为JSON格式"));
        xmlToJsonBtn.setOnAction(e -> convertXmlToJson());

        // JSON转XML按钮
        jsonToXmlBtn = new Button();
        jsonToXmlBtn.setGraphic(createButtonContent("JSON → XML", MaterialDesign.MDI_ARROW_RIGHT_BOLD_CIRCLE));
        jsonToXmlBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-weight: bold; " +
                             "-fx-padding: 12 20; -fx-font-size: 13px;");
        jsonToXmlBtn.setTooltip(new Tooltip("将JSON转换为XML格式"));
        jsonToXmlBtn.setOnAction(e -> convertJsonToXml());

        // 交换按钮
        Button swapBtn = new Button();
        swapBtn.setGraphic(new FontIcon(MaterialDesign.MDI_SWAP_VERTICAL));
        swapBtn.setStyle("-fx-background-color: #757575; -fx-text-fill: white; " +
                        "-fx-padding: 8 15;");
        swapBtn.setTooltip(new Tooltip("交换输入和输出内容"));
        swapBtn.setOnAction(e -> swapContent());

        box.getChildren().addAll(xmlToJsonBtn, jsonToXmlBtn, swapBtn);

        return box;
    }

    private Node createButtonContent(String text, MaterialDesign icon) {
        HBox content = new HBox(8);
        content.setAlignment(Pos.CENTER);

        Label label = new Label(text);
        FontIcon fontIcon = new FontIcon(icon);
        fontIcon.setIconSize(18);
        fontIcon.setStyle("-fx-icon-color: white;");

        content.getChildren().addAll(label, fontIcon);

        return content;
    }

    private Node createActionButtonsArea() {
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(5));

        // 加载文件按钮
        loadFileBtn = new Button("加载文件", new FontIcon(MaterialDesign.MDI_FOLDER));
        loadFileBtn.setStyle("-fx-background-color: #607D8B; -fx-text-fill: white; -fx-font-weight: bold;");
        loadFileBtn.setOnAction(e -> loadFromFile());

        // 清空按钮
        clearBtn = new Button("清空内容", new FontIcon(MaterialDesign.MDI_DELETE));
        clearBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-weight: bold;");
        clearBtn.setOnAction(e -> clearContent());

        // 复制结果按钮
        copyBtn = new Button("复制结果", new FontIcon(MaterialDesign.MDI_CONTENT_COPY));
        copyBtn.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-font-weight: bold;");
        copyBtn.setOnAction(e -> copyResult());
        copyBtn.setDisable(true);

        // 保存文件按钮
        saveFileBtn = new Button("保存结果", new FontIcon(MaterialDesign.MDI_CONTENT_SAVE));
        saveFileBtn.setStyle("-fx-background-color: #9C27B0; -fx-text-fill: white; -fx-font-weight: bold;");
        saveFileBtn.setOnAction(e -> saveToFile());
        saveFileBtn.setDisable(true);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        buttonBox.getChildren().addAll(loadFileBtn, clearBtn, spacer, copyBtn, saveFileBtn);

        return buttonBox;
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

    private void bindProperties() {
        statusLabel.textProperty().bind(service.statusMessageProperty());
        progressBar.progressProperty().bind(service.progressProperty());
    }

    /**
     * 自动检测输入内容的类型
     */
    private void autoDetectType(String content) {
        if (content == null || content.trim().isEmpty()) {
            inputTypeLabel.setText("XML/JSON");
            inputTypeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666; -fx-background-color: #e3f2fd; " +
                                   "-fx-padding: 2 8 2 8; -fx-background-radius: 10;");
            return;
        }

        String trimmed = content.trim();
        if (trimmed.startsWith("<") || trimmed.startsWith("<?xml")) {
            inputTypeLabel.setText("XML");
            inputTypeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: white; -fx-background-color: #4CAF50; " +
                                   "-fx-padding: 2 8 2 8; -fx-background-radius: 10;");
        } else if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            inputTypeLabel.setText("JSON");
            inputTypeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: white; -fx-background-color: #2196F3; " +
                                   "-fx-padding: 2 8 2 8; -fx-background-radius: 10;");
        } else {
            inputTypeLabel.setText("文本");
            inputTypeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666; -fx-background-color: #e3f2fd; " +
                                   "-fx-padding: 2 8 2 8; -fx-background-radius: 10;");
        }
    }

    private void convertXmlToJson() {
        String xmlContent = inputArea.getText();
        if (xmlContent == null || xmlContent.trim().isEmpty()) {
            showAlert("提示", "请输入XML内容", Alert.AlertType.WARNING);
            return;
        }

        if (!service.isValidXml(xmlContent)) {
            showAlert("错误", "输入的内容不是有效的XML格式", Alert.AlertType.ERROR);
            return;
        }

        // 禁用按钮
        setButtonsDisabled(true);
        outputArea.setText("");

        service.xmlToJsonAsync(xmlContent, formatCheck.isSelected())
            .thenAcceptAsync(result -> {
                Platform.runLater(() -> {
                    outputArea.setText(result);
                    outputTypeLabel.setText("JSON");
                    outputTypeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: white; -fx-background-color: #2196F3; " +
                                            "-fx-padding: 2 8 2 8; -fx-background-radius: 10;");
                    copyBtn.setDisable(false);
                    saveFileBtn.setDisable(false);
                    setButtonsDisabled(false);
                });
            }, Platform::runLater)
            .exceptionally(throwable -> {
                Platform.runLater(() -> {
                    showAlert("转换失败", throwable.getCause() != null ? 
                        throwable.getCause().getMessage() : throwable.getMessage(), 
                        Alert.AlertType.ERROR);
                    setButtonsDisabled(false);
                });
                return null;
            });
    }

    private void convertJsonToXml() {
        String jsonContent = inputArea.getText();
        if (jsonContent == null || jsonContent.trim().isEmpty()) {
            showAlert("提示", "请输入JSON内容", Alert.AlertType.WARNING);
            return;
        }

        if (!service.isValidJson(jsonContent)) {
            showAlert("错误", "输入的内容不是有效的JSON格式", Alert.AlertType.ERROR);
            return;
        }

        // 禁用按钮
        setButtonsDisabled(true);
        outputArea.setText("");

        service.jsonToXmlAsync(jsonContent, formatCheck.isSelected())
            .thenAcceptAsync(result -> {
                Platform.runLater(() -> {
                    outputArea.setText(result);
                    outputTypeLabel.setText("XML");
                    outputTypeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: white; -fx-background-color: #4CAF50; " +
                                            "-fx-padding: 2 8 2 8; -fx-background-radius: 10;");
                    copyBtn.setDisable(false);
                    saveFileBtn.setDisable(false);
                    setButtonsDisabled(false);
                });
            }, Platform::runLater)
            .exceptionally(throwable -> {
                Platform.runLater(() -> {
                    showAlert("转换失败", throwable.getCause() != null ? 
                        throwable.getCause().getMessage() : throwable.getMessage(), 
                        Alert.AlertType.ERROR);
                    setButtonsDisabled(false);
                });
                return null;
            });
    }

    private void swapContent() {
        String input = inputArea.getText();
        String output = outputArea.getText();

        inputArea.setText(output);
        outputArea.setText(input);

        // 交换类型标签
        String inputType = inputTypeLabel.getText();
        String outputType = outputTypeLabel.getText();
        inputTypeLabel.setText(outputType);
        outputTypeLabel.setText(inputType);

        // 更新类型样式
        autoDetectType(output);

        // 更新输出类型样式
        if ("JSON".equals(inputType)) {
            outputTypeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: white; -fx-background-color: #2196F3; " +
                                    "-fx-padding: 2 8 2 8; -fx-background-radius: 10;");
        } else if ("XML".equals(inputType)) {
            outputTypeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: white; -fx-background-color: #4CAF50; " +
                                    "-fx-padding: 2 8 2 8; -fx-background-radius: 10;");
        }
    }

    private void clearContent() {
        inputArea.clear();
        outputArea.clear();
        inputTypeLabel.setText("XML/JSON");
        inputTypeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666; -fx-background-color: #e3f2fd; " +
                               "-fx-padding: 2 8 2 8; -fx-background-radius: 10;");
        outputTypeLabel.setText("结果");
        outputTypeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666; -fx-background-color: #e3f2fd; " +
                                "-fx-padding: 2 8 2 8; -fx-background-radius: 10;");
        copyBtn.setDisable(true);
        saveFileBtn.setDisable(true);
        service.resetStatus();
    }

    private void copyResult() {
        String result = outputArea.getText();
        if (result != null && !result.isEmpty()) {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(result);
            clipboard.setContent(content);
            statusLabel.setText("结果已复制到剪贴板");
        }
    }

    private void loadFromFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择文件");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("所有支持的文件", "*.xml", "*.json"),
            new FileChooser.ExtensionFilter("XML文件", "*.xml"),
            new FileChooser.ExtensionFilter("JSON文件", "*.json"),
            new FileChooser.ExtensionFilter("所有文件", "*.*")
        );

        File file = fileChooser.showOpenDialog(getScene().getWindow());
        if (file != null) {
            try {
                String content = Files.readString(file.toPath());
                inputArea.setText(content);
                statusLabel.setText("已加载文件: " + file.getName());
            } catch (Exception e) {
                showAlert("错误", "读取文件失败: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    private void saveToFile() {
        String result = outputArea.getText();
        if (result == null || result.isEmpty()) {
            showAlert("提示", "没有可保存的内容", Alert.AlertType.WARNING);
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("保存结果");

        // 根据输出类型设置默认扩展名
        String outputType = outputTypeLabel.getText();
        if ("JSON".equals(outputType)) {
            fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JSON文件", "*.json")
            );
            fileChooser.setInitialFileName("result.json");
        } else if ("XML".equals(outputType)) {
            fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("XML文件", "*.xml")
            );
            fileChooser.setInitialFileName("result.xml");
        } else {
            fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("文本文件", "*.txt")
            );
            fileChooser.setInitialFileName("result.txt");
        }

        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("所有文件", "*.*")
        );

        File file = fileChooser.showSaveDialog(getScene().getWindow());
        if (file != null) {
            try {
                Files.writeString(file.toPath(), result);
                statusLabel.setText("结果已保存至: " + file.getAbsolutePath());
            } catch (Exception e) {
                showAlert("错误", "保存文件失败: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    private void setButtonsDisabled(boolean disabled) {
        xmlToJsonBtn.setDisable(disabled);
        jsonToXmlBtn.setDisable(disabled);
        loadFileBtn.setDisable(disabled);
        clearBtn.setDisable(disabled);
    }

    private void showHelp() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("使用说明");
        alert.setHeaderText("XML/JSON互转工具 - 使用指南");

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
        alert.setHeaderText("XML/JSON互转工具");
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
