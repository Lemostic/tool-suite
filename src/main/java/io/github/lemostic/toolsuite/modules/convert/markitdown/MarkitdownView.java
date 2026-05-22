package io.github.lemostic.toolsuite.modules.convert.markitdown;

import io.github.lemostic.toolsuite.modules.convert.markitdown.MarkitdownService.ConvertConfig;
import io.github.lemostic.toolsuite.modules.convert.markitdown.MarkitdownService.ConvertResult;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Separator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign.MaterialDesign;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.File;

public class MarkitdownView extends BorderPane {

    private final MarkitdownService service = new MarkitdownService();

    private TextField outputDirField;
    private TextField imageDirField;
    private ComboBox<String> timeoutCombo;
    private TextField pythonPathField;
    private TextArea previewArea;
    private ProgressBar progressBar;
    private Label statusLabel;
    private Button convertBtn;
    private Label dropZoneLabel;
    private Label fileNameLabel;
    private HBox dropZone;

    private File selectedInputFile;
    private boolean markitdownAvailable = false;

    public MarkitdownView() {
        initializeUI();
        checkEnvironment();
    }

    private void initializeUI() {
        setStyle("-fx-background-color: #f0f2f5;");

        Label titleLabel = new Label("Markitdown - 文档转 Markdown");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1a1a2e;");

        HBox topBox = new HBox(titleLabel);
        topBox.setPadding(new Insets(15, 20, 10, 20));
        topBox.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-width: 0 0 1 0;");
        setTop(topBox);

        VBox leftPanel = createOptionsPanel();
        leftPanel.setMinWidth(400);
        leftPanel.setMaxWidth(600);

        VBox rightPanel = createPreviewPanel();

        SplitPane splitPane = new SplitPane();
        splitPane.getItems().addAll(leftPanel, rightPanel);
        splitPane.setDividerPositions(0.45);
        splitPane.setStyle("-fx-background-color: transparent;");
        SplitPane.setResizableWithParent(leftPanel, Boolean.TRUE);
        SplitPane.setResizableWithParent(rightPanel, Boolean.TRUE);
        VBox.setVgrow(splitPane, Priority.ALWAYS);

        VBox centerBox = new VBox(splitPane);
        centerBox.setPadding(new Insets(10, 20, 20, 20));
        VBox.setVgrow(splitPane, Priority.ALWAYS);
        setCenter(centerBox);

        setBottom(createStatusBar());
    }

    private VBox createOptionsPanel() {
        VBox panel = new VBox(12);
        panel.setPadding(new Insets(0));

        panel.getChildren().add(createFileSelectionCard());
        panel.getChildren().add(createOutputCard());
        panel.getChildren().add(createOptionsCard());
        panel.getChildren().add(createPythonPathCard());
        panel.getChildren().add(createActionButtons());

        return panel;
    }

    private Node createFileSelectionCard() {
        VBox card = createCard("选择文件", MaterialDesign.MDI_FILE_FIND);

        dropZone = new HBox(15);
        dropZone.setAlignment(Pos.CENTER);
        dropZone.setPrefHeight(180);
        dropZone.setStyle(
            "-fx-background-color: #f5f7fa; " +
            "-fx-border-style: dashed; " +
            "-fx-border-color: #c0ccda; " +
            "-fx-border-width: 2; " +
            "-fx-border-radius: 8; " +
            "-fx-background-radius: 8; " +
            "-fx-cursor: hand;"
        );

        FontIcon uploadIcon = new FontIcon(MaterialDesign.MDI_UPLOAD);
        uploadIcon.setIconSize(48);
        uploadIcon.setIconColor(Color.web("#8c9eb5"));

        Label uploadLabel = new Label("点击或拖拽文件到此处");
        uploadLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #5a6a85;");

        Label formatLabel = new Label("支持 PDF、Word、Excel、PPT、HTML 等格式");
        formatLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #99a8b7;");

        dropZone.getChildren().addAll(uploadIcon, uploadLabel, formatLabel);

        dropZone.setOnMouseClicked(e -> selectInputFile());
        dropZone.setOnDragOver(e -> {
            e.acceptTransferModes();
            dropZone.setStyle(
                "-fx-background-color: #e8f4fc; " +
                "-fx-border-style: dashed; " +
                "-fx-border-color: #3498db; " +
                "-fx-border-width: 2; " +
                "-fx-border-radius: 8; " +
                "-fx-background-radius: 8; " +
                "-fx-cursor: hand;"
            );
        });
        dropZone.setOnDragExited(e -> {
            dropZone.setStyle(
                "-fx-background-color: #f5f7fa; " +
                "-fx-border-style: dashed; " +
                "-fx-border-color: #c0ccda; " +
                "-fx-border-width: 2; " +
                "-fx-border-radius: 8; " +
                "-fx-background-radius: 8; " +
                "-fx-cursor: hand;"
            );
        });
        dropZone.setOnDragDropped(e -> {
            var files = e.getDragboard().getFiles();
            if (files != null && !files.isEmpty()) {
                File file = files.get(0);
                if (file.isFile()) {
                    setSelectedFile(file);
                }
            }
            dropZone.setStyle(
                "-fx-background-color: #f5f7fa; " +
                "-fx-border-style: dashed; " +
                "-fx-border-color: #c0ccda; " +
                "-fx-border-width: 2; " +
                "-fx-border-radius: 8; " +
                "-fx-background-radius: 8; " +
                "-fx-cursor: hand;"
            );
        });

        fileNameLabel = new Label("未选择文件");
        fileNameLabel.setStyle(
            "-fx-font-size: 14px; " +
            "-fx-font-weight: bold; " +
            "-fx-text-fill: #333; " +
            "-fx-padding: 10 0 0 0;"
        );
        fileNameLabel.setAlignment(Pos.CENTER);
        fileNameLabel.setMaxWidth(Double.MAX_VALUE);
        Label textHint = new Label("点击上方区域选择文件，或将文件拖入");
        textHint.setStyle("-fx-font-size: 12px; -fx-text-fill: #99a8b7;");
        textHint.setAlignment(Pos.CENTER);
        textHint.setMaxWidth(Double.MAX_VALUE);

        HBox clearBtnBox = new HBox();
        clearBtnBox.setAlignment(Pos.CENTER);
        Button clearBtn = new Button("清除文件", new FontIcon(MaterialDesign.MDI_CLOSE_CIRCLE));
        clearBtn.setStyle(
            "-fx-background-color: transparent; " +
            "-fx-text-fill: #8492a6; " +
            "-fx-padding: 8 16; " +
            "-fx-cursor: hand;"
        );
        clearBtn.setOnAction(e -> {
            selectedInputFile = null;
            fileNameLabel.setText("未选择文件");
            previewArea.clear();
            updateDropZoneState();
        });
        clearBtnBox.getChildren().add(clearBtn);
        clearBtnBox.setPadding(new Insets(5, 0, 0, 0));

        VBox content = new VBox(10, dropZone, fileNameLabel, textHint, clearBtnBox);
        content.setPadding(new Insets(10));

        card.getChildren().add(content);
        return card;
    }

    private void updateDropZoneState() {
        if (selectedInputFile != null) {
            dropZone.setStyle(
                "-fx-background-color: #e8f8f0; " +
                "-fx-border-style: solid; " +
                "-fx-border-color: #27ae60; " +
                "-fx-border-width: 2; " +
                "-fx-border-radius: 8; " +
                "-fx-background-radius: 8; " +
                "-fx-cursor: hand;"
            );
        } else {
            dropZone.setStyle(
                "-fx-background-color: #f5f7fa; " +
                "-fx-border-style: dashed; " +
                "-fx-border-color: #c0ccda; " +
                "-fx-border-width: 2; " +
                "-fx-border-radius: 8; " +
                "-fx-background-radius: 8; " +
                "-fx-cursor: hand;"
            );
        }
    }

    private Node createOutputCard() {
        VBox card = createCard("输出设置", MaterialDesign.MDI_FOLDER);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
        grid.setPadding(new Insets(10));

        Label dirLabel = new Label("输出目录:");
        dirLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #333;");
        outputDirField = new TextField();
        outputDirField.setPromptText("默认: 源文件所在目录");
        outputDirField.setEditable(false);
        outputDirField.setStyle("-fx-font-size: 13px;");

        Button dirBrowseBtn = new Button("选择", new FontIcon(MaterialDesign.MDI_FOLDER));
        dirBrowseBtn.setOnAction(e -> selectOutputDir());

        Label nameLabel = new Label("输出文件名:");
        nameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #333;");

        Label fixedNameLabel = new Label(".md (固定格式)");
        fixedNameLabel.setStyle(
            "-fx-font-size: 13px; " +
            "-fx-text-fill: #27ae60; " +
            "-fx-font-weight: bold;"
        );

        grid.add(dirLabel, 0, 0);
        grid.add(outputDirField, 1, 0);
        grid.add(dirBrowseBtn, 2, 0);
        grid.add(nameLabel, 0, 1);
        grid.add(fixedNameLabel, 1, 1, 2, 1);

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setMinWidth(80);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(col1, col2);

        card.getChildren().add(grid);
        return card;
    }

    private Node createOptionsCard() {
        VBox card = createCard("转换选项", MaterialDesign.MDI_SETTINGS);

        VBox options = new VBox(10);
        options.setPadding(new Insets(5, 10, 5, 10));

        GridPane imgGrid = new GridPane();
        imgGrid.setHgap(10);
        imgGrid.setVgap(5);
        Label imgLabel = new Label("图片保存在:");
        imgLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #333;");
        imageDirField = new TextField();
        imageDirField.setPromptText("留空则图片内嵌为 base64");
        imageDirField.setStyle("-fx-font-size: 13px;");
        Button imgBtn = new Button("...", new FontIcon(MaterialDesign.MDI_FOLDER));
        imgBtn.setOnAction(e -> selectImageDir());
        imgBtn.setStyle("-fx-padding: 4 12;");

        imgGrid.add(imgLabel, 0, 0);
        imgGrid.add(imageDirField, 1, 0);
        imgGrid.add(imgBtn, 2, 0);
        ColumnConstraints imgCol1 = new ColumnConstraints();
        imgCol1.setMinWidth(80);
        ColumnConstraints imgCol2 = new ColumnConstraints();
        imgCol2.setHgrow(Priority.ALWAYS);
        imgGrid.getColumnConstraints().addAll(imgCol1, imgCol2);

        Separator sep = new Separator();

        Label timeoutLabel = new Label("超时时间:");
        timeoutLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #333;");
        timeoutCombo = new ComboBox<>();
        timeoutCombo.getItems().addAll("30秒", "60秒", "120秒", "300秒", "600秒");
        timeoutCombo.setValue("120秒");
        timeoutCombo.setMaxWidth(150);
        timeoutCombo.setStyle("-fx-font-size: 13px;");

        HBox timeoutBox = new HBox(10, timeoutLabel, timeoutCombo);
        timeoutBox.setAlignment(Pos.CENTER_LEFT);

        options.getChildren().addAll(imgGrid, sep, timeoutBox);

        card.getChildren().add(options);
        return card;
    }

    private Node createPythonPathCard() {
        VBox card = createCard("Python 环境", MaterialDesign.MDI_CODE_BRACES);

        VBox content = new VBox(10);
        content.setPadding(new Insets(5, 10, 5, 10));

        Label pythonLabel = new Label("Python 路径（可选）:");
        pythonLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #333;");

        HBox pythonBox = new HBox(8);
        pythonBox.setAlignment(Pos.CENTER_LEFT);

        pythonPathField = new TextField();
        pythonPathField.setPromptText("留空则自动检测 Python");
        pythonPathField.setStyle("-fx-font-size: 13px;");
        HBox.setHgrow(pythonPathField, Priority.ALWAYS);

        Button checkBtn = new Button("检测", new FontIcon(MaterialDesign.MDI_MAGNIFY));
        checkBtn.setStyle("-fx-padding: 6 16; -fx-background-radius: 4;");
        checkBtn.setOnAction(e -> checkPythonWithCustomPath());

        pythonBox.getChildren().addAll(pythonPathField, checkBtn);

        Label hintLabel = new Label("如果自动检测失败，请手动指定 Python 路径（如 C:\\Python311\\python.exe）");
        hintLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #99a8b7;");
        hintLabel.setWrapText(true);

        content.getChildren().addAll(pythonLabel, pythonBox, hintLabel);
        card.getChildren().add(content);

        return card;
    }

    private Node createActionButtons() {
        HBox box = new HBox(12);
        box.setPadding(new Insets(15, 0, 10, 0));
        box.setAlignment(Pos.CENTER);

        convertBtn = new Button("开始转换");
        convertBtn.setStyle(
            "-fx-background-color: #3498db; -fx-text-fill: white; " +
            "-fx-font-weight: bold; -fx-padding: 14 40; -fx-background-radius: 6; " +
            "-fx-cursor: hand; -fx-font-size: 15px;"
        );
        convertBtn.setMaxWidth(Double.MAX_VALUE);
        convertBtn.setOnAction(e -> startConversion());
        HBox.setHgrow(convertBtn, Priority.ALWAYS);

        box.getChildren().add(convertBtn);
        return box;
    }

    private VBox createPreviewPanel() {
        VBox panel = new VBox(12);
        panel.setPadding(new Insets(15));
        panel.setStyle(
            "-fx-background-color: white; -fx-border-color: #e8e8e8; " +
            "-fx-border-radius: 10; -fx-background-radius: 10;"
        );

        Label previewHeader = new Label("转换预览");
        previewHeader.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #333;");

        previewArea = new TextArea();
        previewArea.setPromptText("转换后的 Markdown 内容将显示在这里...");
        previewArea.setEditable(false);
        previewArea.setWrapText(false);
        previewArea.setStyle(
            "-fx-font-family: 'Consolas', 'Monaco', 'Courier New', monospace; " +
            "-fx-font-size: 13px; -fx-background-color: #fafafa;"
        );
        VBox.setVgrow(previewArea, Priority.ALWAYS);

        HBox btnBar = new HBox(12);
        btnBar.setPadding(new Insets(5, 0, 0, 0));
        btnBar.setAlignment(Pos.CENTER_LEFT);

        Button copyBtn = new Button("复制内容", new FontIcon(MaterialDesign.MDI_CONTENT_COPY));
        copyBtn.setStyle("-fx-padding: 6 16; -fx-background-radius: 4; -fx-cursor: hand;");
        copyBtn.setOnAction(e -> copyToClipboard());

        Button openFolderBtn = new Button("打开目录", new FontIcon(MaterialDesign.MDI_FOLDER));
        openFolderBtn.setStyle("-fx-padding: 6 16; -fx-background-radius: 4; -fx-cursor: hand;");
        openFolderBtn.setOnAction(e -> openOutputFolder());

        Button clearPreviewBtn = new Button("清空预览", new FontIcon(MaterialDesign.MDI_ERASER));
        clearPreviewBtn.setStyle("-fx-padding: 6 16; -fx-background-radius: 4; -fx-cursor: hand;");
        clearPreviewBtn.setOnAction(e -> previewArea.clear());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        btnBar.getChildren().addAll(copyBtn, openFolderBtn, spacer, clearPreviewBtn);

        panel.getChildren().addAll(previewHeader, previewArea, btnBar);
        return panel;
    }

    private Node createStatusBar() {
        VBox statusBox = new VBox(5);
        statusBox.setPadding(new Insets(10));
        statusBox.setStyle(
            "-fx-background-color: linear-gradient(to top, #ffffff, #f0f0f0); " +
            "-fx-border-color: #d0d0d0; -fx-border-width: 1 0 0 0;"
        );

        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(Double.MAX_VALUE);
        progressBar.setVisible(false);

        statusLabel = new Label("就绪");
        statusLabel.setStyle("-fx-text-fill: #666;");

        statusBox.getChildren().addAll(progressBar, statusLabel);
        return statusBox;
    }

    private VBox createCard(String title, MaterialDesign icon) {
        VBox card = new VBox(10);
        card.setStyle(
            "-fx-background-color: white; " +
            "-fx-border-color: #e0e0e0; " +
            "-fx-border-radius: 5; " +
            "-fx-background-radius: 5; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 5, 0, 0, 2);"
        );
        card.setPadding(new Insets(15));

        HBox titleBox = new HBox(8);
        titleBox.setAlignment(Pos.CENTER_LEFT);

        FontIcon titleIcon = new FontIcon(icon);
        titleIcon.setIconSize(20);
        titleIcon.setStyle("-fx-icon-color: #3498db;");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #333;");

        titleBox.getChildren().addAll(titleIcon, titleLabel);

        Separator separator = new Separator();

        card.getChildren().addAll(titleBox, separator);
        return card;
    }

    private void checkEnvironment() {
        checkPythonWithCustomPath();
    }

    private void checkPythonWithCustomPath() {
        new Thread(() -> {
            String customPython = pythonPathField.getText().trim();
            boolean pythonOk;

            if (!customPython.isEmpty()) {
                pythonOk = MarkitdownService.isPythonAvailable(customPython);
            } else {
                pythonOk = MarkitdownService.isPythonAvailable();
            }

            if (!pythonOk) {
                Platform.runLater(() -> {
                    statusLabel.setText("未检测到 Python 环境，请先安装 Python 3.8+ 或手动指定路径");
                    statusLabel.setStyle("-fx-text-fill: #e74c3c;");
                    convertBtn.setDisable(true);
                    convertBtn.setStyle(
                        "-fx-background-color: #bdc3c7; -fx-text-fill: white; " +
                        "-fx-font-weight: bold; -fx-padding: 14 40; -fx-background-radius: 6; " +
                        "-fx-font-size: 15px;"
                    );
                });
                return;
            }

            boolean installed;
            if (!customPython.isEmpty()) {
                installed = MarkitdownService.isMarkitdownInstalled(customPython);
            } else {
                installed = MarkitdownService.isMarkitdownInstalled();
            }

            markitdownAvailable = installed;
            MarkitdownService.setCustomPythonPath(customPython.isEmpty() ? null : customPython);

            Platform.runLater(() -> {
                if (installed) {
                    statusLabel.setText("markitdown 已就绪，支持 PDF/Word/Excel/PPT 等格式");
                    statusLabel.setStyle("-fx-text-fill: #27ae60;");
                } else {
                    statusLabel.setText("未安装 markitdown，请运行: pip install 'markitdown[all]'");
                    statusLabel.setStyle("-fx-text-fill: #e67e22;");
                    convertBtn.setDisable(true);
                    convertBtn.setStyle(
                        "-fx-background-color: #bdc3c7; -fx-text-fill: white; " +
                        "-fx-font-weight: bold; -fx-padding: 14 40; -fx-background-radius: 6; " +
                        "-fx-font-size: 15px;"
                    );
                }
            });
        }).start();
    }

    private void selectInputFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择要转换的文档");

        FileChooser.ExtensionFilter docFilter = new FileChooser.ExtensionFilter(
            "支持文档 (*.pdf, *.docx, *.pptx, *.xlsx, *.html, *.csv, *.json, *.xml, *.txt, *.md, *.rtf)",
            "*.pdf", "*.docx", "*.pptx", "*.xlsx", "*.xls",
            "*.csv", "*.json", "*.xml", "*.html", "*.htm",
            "*.txt", "*.md", "*.rtf", "*.odt", "*.ods",
            "*.epub", "*.zip"
        );
        fileChooser.getExtensionFilters().add(docFilter);
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("所有文件", "*.*"));

        File file = fileChooser.showOpenDialog(getScene().getWindow());
        if (file != null) {
            setSelectedFile(file);
        }
    }

    private void setSelectedFile(File file) {
        selectedInputFile = file;
        fileNameLabel.setText(file.getName());

        if (outputDirField.getText().isEmpty()) {
            outputDirField.setText(file.getParent());
        }

        updateDropZoneState();
    }

    private void selectOutputDir() {
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("选择输出目录");
        if (selectedInputFile != null) {
            dirChooser.setInitialDirectory(selectedInputFile.getParentFile());
        }
        File dir = dirChooser.showDialog(getScene().getWindow());
        if (dir != null) {
            outputDirField.setText(dir.getAbsolutePath());
        }
    }

    private void selectImageDir() {
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("选择图片保存目录");
        if (selectedInputFile != null) {
            dirChooser.setInitialDirectory(selectedInputFile.getParentFile());
        }
        File dir = dirChooser.showDialog(getScene().getWindow());
        if (dir != null) {
            imageDirField.setText(dir.getAbsolutePath());
        }
    }

    private void startConversion() {
        if (selectedInputFile == null || !selectedInputFile.exists()) {
            showAlert("请先选择要转换的源文档", AlertType.WARNING);
            return;
        }

        if (!markitdownAvailable) {
            showAlert(
                "未安装 markitdown 库\n\n" +
                "请打开命令行运行:\n\n" +
                "  pip install 'markitdown[all]'\n\n" +
                "该命令将安装 markitdown 及所有可选依赖，支持更多文档格式。",
                AlertType.ERROR
            );
            return;
        }

        ConvertConfig config = buildConfig();
        if (config.getOutputFile() == null) {
            String outputDir = outputDirField.getText().trim();
            if (outputDir.isEmpty()) {
                outputDir = selectedInputFile.getParent();
            }
            String baseName = selectedInputFile.getName();
            int dotIndex = baseName.lastIndexOf('.');
            if (dotIndex > 0) {
                baseName = baseName.substring(0, dotIndex);
            }
            String fileName = baseName + ".md";
            config.setOutputFile(new File(outputDir, fileName));
        }

        progressBar.setVisible(true);
        progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        statusLabel.setText("正在转换...");
        statusLabel.setStyle("-fx-text-fill: #3498db;");
        convertBtn.setDisable(true);
        convertBtn.setStyle(
            "-fx-background-color: #bdc3c7; -fx-text-fill: white; " +
            "-fx-font-weight: bold; -fx-padding: 14 40; -fx-background-radius: 6; " +
            "-fx-font-size: 15px;"
        );

        new Thread(() -> {
            ConvertResult result = service.convert(config);
            Platform.runLater(() -> handleResult(result));
        }).start();
    }

    private ConvertConfig buildConfig() {
        ConvertConfig config = new ConvertConfig();
        config.setInputFile(selectedInputFile);
        config.setImageOutputDir(imageDirField.getText().isBlank() ? null : imageDirField.getText().trim());

        String timeoutVal = timeoutCombo.getValue();
        if (timeoutVal != null) {
            try {
                config.setTimeoutSeconds(Integer.parseInt(timeoutVal.replace("秒", "")));
            } catch (NumberFormatException e) {
                config.setTimeoutSeconds(120);
            }
        }
        return config;
    }

    private void handleResult(ConvertResult result) {
        progressBar.setVisible(false);
        convertBtn.setDisable(false);
        convertBtn.setStyle(
            "-fx-background-color: #3498db; -fx-text-fill: white; " +
            "-fx-font-weight: bold; -fx-padding: 14 40; -fx-background-radius: 6; " +
            "-fx-cursor: hand; -fx-font-size: 15px;"
        );

        if (result.isSuccess()) {
            statusLabel.setText(result.getMessage());
            statusLabel.setStyle("-fx-text-fill: #27ae60;");

            if (result.getExtractedImages() > 0) {
                statusLabel.setText(result.getMessage() + " (已提取 " + result.getExtractedImages() + " 张图片)");
            }

            previewArea.setText(result.getMarkdownContent());

            String msg = result.getMessage();
            if (result.getExtractedImages() > 0) {
                msg += "\n已提取 " + result.getExtractedImages() + " 张图片到指定目录";
            }
            showInfo(msg);
        } else {
            statusLabel.setText(result.getMessage());
            statusLabel.setStyle("-fx-text-fill: #e74c3c;");
            showAlert("转换失败:\n" + result.getMessage(), AlertType.ERROR);
        }
    }

    private void copyToClipboard() {
        String text = previewArea.getText();
        if (text == null || text.isEmpty()) {
            showAlert("预览区没有可复制的内容", AlertType.WARNING);
            return;
        }
        StringSelection selection = new StringSelection(text);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
        showInfo("Markdown 内容已复制到剪贴板");
    }

    private void openOutputFolder() {
        if (selectedInputFile != null) {
            String dir = outputDirField.getText().trim();
            if (dir.isEmpty()) {
                dir = selectedInputFile.getParent();
            }
            try {
                java.awt.Desktop.getDesktop().open(new File(dir));
            } catch (Exception e) {
                showAlert("无法打开目录: " + e.getMessage(), AlertType.ERROR);
            }
        }
    }

    private void showAlert(String msg, AlertType type) {
        Alert alert = new Alert(type, msg, ButtonType.OK);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private void showInfo(String msg) {
        Alert alert = new Alert(AlertType.INFORMATION, msg, ButtonType.OK);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}