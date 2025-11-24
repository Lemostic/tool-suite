package io.github.lemostic.toolsuite.modules.file.zipclean;

import io.github.lemostic.toolsuite.modules.file.zipclean.service.ZipCleanService;
import io.github.lemostic.toolsuite.modules.file.zipclean.service.ZipCleanService.CleanRule;
import io.github.lemostic.toolsuite.modules.file.zipclean.service.ZipCleanService.CleanResult;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign.MaterialDesign;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 压缩包清理工具界面
 */
public class ZipCleanView extends BorderPane {
    
    private final ZipCleanService service = new ZipCleanService();
    
    // UI 组件
    private TextField zipFileField;
    private TextField outputNameField;
    private CheckBox extractAfterCleanCheck;
    private CheckBox deleteOriginalCheck;
    private TableView<RuleItem> ruleTable;
    private ObservableList<RuleItem> ruleItems;
    private TextArea previewArea;
    private ProgressBar progressBar;
    private Label statusLabel;
    private Label statsLabel;
    
    private File selectedZipFile;
    
    public ZipCleanView() {
        initializeUI();
        setupDefaultRules();
        bindProperties();
    }
    
    private void initializeUI() {
        // 顶部工具栏
        setTop(createToolbar());
        
        // 中间主内容区
        setCenter(createMainContent());
        
        // 底部状态栏
        setBottom(createStatusBar());
        
        // 样式
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
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        toolbar.getItems().addAll(helpBtn, aboutBtn);
        
        return toolbar;
    }
    
    private Node createMainContent() {
        VBox mainBox = new VBox(15);
        mainBox.setPadding(new Insets(20));
        
        // 文件选择区域
        mainBox.getChildren().add(createFileSelectionCard());
        
        // 清理规则区域
        mainBox.getChildren().add(createRulesCard());
        
        // 预览和操作区域
        mainBox.getChildren().add(createPreviewCard());
        
        ScrollPane scrollPane = new ScrollPane(mainBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #f5f5f5;");
        
        return scrollPane;
    }
    
    private Node createFileSelectionCard() {
        VBox card = createCard("文件选择", MaterialDesign.MDI_FILE_DOCUMENT_BOX);
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
        grid.setPadding(new Insets(10));
        
        // 源文件
        Label sourceLabel = new Label("源压缩包:");
        sourceLabel.setStyle("-fx-font-weight: bold;");
        zipFileField = new TextField();
        zipFileField.setPromptText("请选择要清理的压缩包文件...");
        zipFileField.setEditable(false);
        zipFileField.setPrefWidth(400);
        
        Button browseBtn = new Button("浏览", new FontIcon(MaterialDesign.MDI_FILE));
        browseBtn.setOnAction(e -> selectZipFile());
        
        // 输出名称
        Label outputLabel = new Label("输出名称:");
        outputLabel.setStyle("-fx-font-weight: bold;");
        outputNameField = new TextField();
        outputNameField.setPromptText("输出文件/文件夹名称（不包含扩展名）");
        
        // 选项区
        VBox optionsBox = new VBox(8);
        optionsBox.setPadding(new Insets(5, 0, 0, 0));
        
        extractAfterCleanCheck = new CheckBox("清理后解压为文件夹");
        extractAfterCleanCheck.setStyle("-fx-font-size: 13px;");
        
        deleteOriginalCheck = new CheckBox("删除原文件（谨慎操作）");
        deleteOriginalCheck.setStyle("-fx-font-size: 13px; -fx-text-fill: #d32f2f;");
        
        // 添加提示
        Label tipLabel = new Label("ℹ️ 提示：默认保留原文件，生成 _cleaned 后缀的新文件");
        tipLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666; -fx-font-style: italic;");
        
        optionsBox.getChildren().addAll(extractAfterCleanCheck, deleteOriginalCheck, tipLabel);
        
        grid.add(sourceLabel, 0, 0);
        grid.add(zipFileField, 1, 0);
        grid.add(browseBtn, 2, 0);
        
        grid.add(outputLabel, 0, 1);
        grid.add(outputNameField, 1, 1, 2, 1);
        
        grid.add(new Label("清理选项:"), 0, 2);
        grid.add(optionsBox, 1, 2, 2, 1);
        
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setMinWidth(100);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);
        ColumnConstraints col3 = new ColumnConstraints();
        grid.getColumnConstraints().addAll(col1, col2, col3);
        
        card.getChildren().add(grid);
        
        return card;
    }
    
    private Node createRulesCard() {
        VBox card = createCard("清理规则", MaterialDesign.MDI_FILTER_VARIANT);
        
        // 规则表格
        ruleTable = new TableView<>();
        ruleItems = FXCollections.observableArrayList();
        ruleTable.setItems(ruleItems);
        ruleTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        ruleTable.setPrefHeight(200);
        
        // 启用列
        TableColumn<RuleItem, Boolean> enabledCol = new TableColumn<>("启用");
        enabledCol.setCellValueFactory(new PropertyValueFactory<>("enabled"));
        enabledCol.setCellFactory(CheckBoxTableCell.forTableColumn(enabledCol));
        enabledCol.setEditable(true);
        enabledCol.setMaxWidth(60);
        enabledCol.setMinWidth(60);
        
        // 规则列
        TableColumn<RuleItem, String> patternCol = new TableColumn<>("匹配规则");
        patternCol.setCellValueFactory(new PropertyValueFactory<>("pattern"));
        patternCol.setPrefWidth(300);
        
        // 类型列
        TableColumn<RuleItem, String> typeCol = new TableColumn<>("类型");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        typeCol.setMaxWidth(100);
        
        // 描述列
        TableColumn<RuleItem, String> descCol = new TableColumn<>("说明");
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        
        ruleTable.getColumns().addAll(enabledCol, patternCol, typeCol, descCol);
        ruleTable.setEditable(true);
        
        // 按钮栏
        HBox btnBox = new HBox(10);
        btnBox.setPadding(new Insets(10, 0, 0, 0));
        
        Button addBtn = new Button("添加规则", new FontIcon(MaterialDesign.MDI_PLUS));
        addBtn.setOnAction(e -> addRule());
        
        Button removeBtn = new Button("删除选中", new FontIcon(MaterialDesign.MDI_DELETE));
        removeBtn.setOnAction(e -> removeSelectedRule());
        
        Button resetBtn = new Button("重置默认", new FontIcon(MaterialDesign.MDI_REFRESH));
        resetBtn.setOnAction(e -> setupDefaultRules());
        
        btnBox.getChildren().addAll(addBtn, removeBtn, resetBtn);
        
        card.getChildren().addAll(ruleTable, btnBox);
        
        return card;
    }
    
    private Node createPreviewCard() {
        VBox card = createCard("操作与预览", MaterialDesign.MDI_EYE);
        
        // 按钮区
        HBox buttonBox = new HBox(15);
        buttonBox.setPadding(new Insets(0, 0, 10, 0));
        buttonBox.setAlignment(Pos.CENTER_LEFT);
        
        Button previewBtn = new Button("预览要删除的文件", new FontIcon(MaterialDesign.MDI_FILE_FIND));
        previewBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-size: 14px;");
        previewBtn.setOnAction(e -> previewDeletion());
        
        Button cleanBtn = new Button("开始清理", new FontIcon(MaterialDesign.MDI_DELETE_SWEEP));
        cleanBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        cleanBtn.setOnAction(e -> startCleaning());
        
        statsLabel = new Label("请选择文件并配置规则");
        statsLabel.setStyle("-fx-text-fill: #666; -fx-font-style: italic;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        buttonBox.getChildren().addAll(previewBtn, cleanBtn, spacer, statsLabel);
        
        // 预览区
        Label previewLabel = new Label("预览结果:");
        previewLabel.setStyle("-fx-font-weight: bold;");
        
        previewArea = new TextArea();
        previewArea.setPromptText("点击\"预览要删除的文件\"查看将要删除的文件列表...");
        previewArea.setPrefHeight(200);
        previewArea.setEditable(false);
        previewArea.setStyle("-fx-font-family: 'Consolas', 'Monaco', monospace;");
        
        card.getChildren().addAll(buttonBox, previewLabel, previewArea);
        
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
        statusLabel.textProperty().bind(service.statusMessageProperty());
        progressBar.progressProperty().bind(service.progressProperty());
    }
    
    private void setupDefaultRules() {
        ruleItems.clear();
        
        // 默认规则：删除 mdm 目录
        ruleItems.add(new RuleItem(true, "^mdm/.*", "正则", "删除 mdm 目录下所有文件"));
        
        // 默认规则：删除 lib 目录下不以 mdm 开头的文件
        ruleItems.add(new RuleItem(true, "^lib/(?!mdm-).*\\.jar$", "正则", "保留 lib 目录下 mdm- 开头的 jar 文件"));
        
        // 示例规则（默认禁用）
        ruleItems.add(new RuleItem(false, "^config/.*\\.bak$", "正则", "删除 config 目录下的备份文件"));
        ruleItems.add(new RuleItem(false, "\\.log$", "正则", "删除所有日志文件"));
    }
    
    private void selectZipFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择压缩包文件");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("压缩文件", "*.zip", "*.jar", "*.war")
        );
        
        File file = fileChooser.showOpenDialog(getScene().getWindow());
        if (file != null) {
            selectedZipFile = file;
            zipFileField.setText(file.getAbsolutePath());
            
            // 自动设置输出文件名
            String name = file.getName();
            int dotIndex = name.lastIndexOf('.');
            String baseName = dotIndex > 0 ? name.substring(0, dotIndex) : name;
            
            outputNameField.setText(baseName + "_cleaned");
        }
    }
    
    /**
     * 删除 selectOutputFile 方法（不再需要）
     */
    
    private void addRule() {
        Dialog<RuleItem> dialog = new Dialog<>();
        dialog.setTitle("添加清理规则");
        dialog.setHeaderText("请输入新的清理规则");
        
        ButtonType addButtonType = new ButtonType("添加", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        TextField patternField = new TextField();
        patternField.setPromptText("例如: ^lib/.*\\.jar$");
        
        CheckBox regexCheck = new CheckBox("使用正则表达式");
        regexCheck.setSelected(true);
        
        TextField descField = new TextField();
        descField.setPromptText("规则说明");
        
        grid.add(new Label("匹配规则:"), 0, 0);
        grid.add(patternField, 1, 0);
        grid.add(regexCheck, 1, 1);
        grid.add(new Label("说明:"), 0, 2);
        grid.add(descField, 1, 2);
        
        dialog.getDialogPane().setContent(grid);
        
        Platform.runLater(patternField::requestFocus);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                return new RuleItem(
                    true,
                    patternField.getText(),
                    regexCheck.isSelected() ? "正则" : "简单",
                    descField.getText()
                );
            }
            return null;
        });
        
        Optional<RuleItem> result = dialog.showAndWait();
        result.ifPresent(ruleItems::add);
    }
    
    private void removeSelectedRule() {
        RuleItem selected = ruleTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            ruleItems.remove(selected);
        }
    }
    
    private void previewDeletion() {
        if (selectedZipFile == null) {
            showAlert("错误", "请先选择压缩包文件", Alert.AlertType.ERROR);
            return;
        }
        
        List<CleanRule> rules = getRules();
        if (rules.isEmpty()) {
            showAlert("错误", "请至少启用一条清理规则", Alert.AlertType.WARNING);
            return;
        }
        
        try {
            List<String> toDelete = service.previewDeletion(selectedZipFile, rules);
            
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("将删除 %d 个文件:\n\n", toDelete.size()));
            
            for (String path : toDelete) {
                sb.append("  ✗ ").append(path).append("\n");
            }
            
            previewArea.setText(sb.toString());
            statsLabel.setText(String.format("预计删除: %d 个文件", toDelete.size()));
            
        } catch (Exception e) {
            showAlert("预览失败", "无法预览文件: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
    
    private void startCleaning() {
        if (selectedZipFile == null) {
            showAlert("错误", "请先选择压缩包文件", Alert.AlertType.ERROR);
            return;
        }
        
        String outputName = outputNameField.getText();
        if (outputName == null || outputName.trim().isEmpty()) {
            showAlert("错误", "请指定输出名称", Alert.AlertType.ERROR);
            return;
        }
        
        List<CleanRule> rules = getRules();
        if (rules.isEmpty()) {
            showAlert("错误", "请至少启用一条清理规则", Alert.AlertType.WARNING);
            return;
        }
        
        // 确认对话框
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("确认清理");
        confirm.setHeaderText("即将开始清理压缩包");
        
        StringBuilder confirmMsg = new StringBuilder();
        confirmMsg.append("源文件: ").append(selectedZipFile.getName()).append("\n");
        confirmMsg.append("输出名称: ").append(outputName).append("\n");
        confirmMsg.append("输出类型: ").append(extractAfterCleanCheck.isSelected() ? "文件夹" : "压缩包").append("\n");
        
        if (deleteOriginalCheck.isSelected()) {
            confirmMsg.append("\n⚠️ 注意：处理后将删除原文件！");
        } else {
            confirmMsg.append("\n原文件将被保留。");
        }
        confirmMsg.append("\n\n是否继续？");
        
        confirm.setContentText(confirmMsg.toString());
        
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            performClean(outputName.trim(), extractAfterCleanCheck.isSelected(), 
                        deleteOriginalCheck.isSelected(), rules);
        }
    }
    
    private void performClean(String outputName, boolean extractAfterClean, 
                             boolean deleteOriginal, List<CleanRule> rules) {
        new Thread(() -> {
            try {
                // 构建输出路径
                File parentDir = selectedZipFile.getParentFile();
                File outputFile;
                
                if (extractAfterClean) {
                    // 输出为文件夹
                    outputFile = new File(parentDir, outputName);
                } else {
                    // 输出为压缩包，保留原始扩展名
                    String originalName = selectedZipFile.getName();
                    int dotIndex = originalName.lastIndexOf('.');
                    String ext = dotIndex > 0 ? originalName.substring(dotIndex) : ".zip";
                    outputFile = new File(parentDir, outputName + ext);
                }
                
                CleanResult result = service.cleanZip(selectedZipFile, rules, outputFile, 
                                                       extractAfterClean, deleteOriginal);
                
                Platform.runLater(() -> {
                    if (result.isSuccess()) {
                        showCleanResult(result, outputFile, extractAfterClean);
                    } else {
                        showAlert("清理失败", result.getMessage(), Alert.AlertType.ERROR);
                    }
                });
                
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showAlert("清理失败", "发生错误: " + e.getMessage(), Alert.AlertType.ERROR);
                });
            }
        }).start();
    }
    
    private void showCleanResult(CleanResult result, File outputFile, boolean isDirectory) {
        double savedPercent = 100.0 * (result.getOriginalSize() - result.getCleanedSize()) / result.getOriginalSize();
        
        StringBuilder msg = new StringBuilder();
        msg.append(String.format("清理完成！\n\n"));
        msg.append(String.format("总文件数: %d\n", result.getTotalFiles()));
        msg.append(String.format("删除文件: %d\n", result.getDeletedFiles()));
        msg.append(String.format("保留文件: %d\n\n", result.getKeptFiles()));
        msg.append(String.format("原始大小: %.2f MB\n", result.getOriginalSize() / 1024.0 / 1024.0));
        msg.append(String.format("清理后: %.2f MB\n", result.getCleanedSize() / 1024.0 / 1024.0));
        msg.append(String.format("节省空间: %.1f%%\n\n", savedPercent));
        msg.append(String.format("输出类型: %s\n", isDirectory ? "文件夹" : "压缩包"));
        msg.append(String.format("输出位置: %s", outputFile.getAbsolutePath()));
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("清理成功");
        alert.setHeaderText("压缩包清理完成");
        alert.setContentText(msg.toString());
        
        // 添加打开文件夹按钮
        ButtonType openFolderBtn = new ButtonType("打开文件夹");
        alert.getButtonTypes().add(openFolderBtn);
        
        Optional<ButtonType> response = alert.showAndWait();
        if (response.isPresent() && response.get() == openFolderBtn) {
            try {
                // 打开文件所在目录
                java.awt.Desktop.getDesktop().open(outputFile.getParentFile());
            } catch (Exception e) {
                showAlert("错误", "无法打开文件夹: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
        
        statsLabel.setText(String.format("完成: 删除 %d 个文件，节省 %.1f%% 空间", 
            result.getDeletedFiles(), savedPercent));
    }
    
    private List<CleanRule> getRules() {
        List<CleanRule> rules = new ArrayList<>();
        for (RuleItem item : ruleItems) {
            if (item.isEnabled()) {
                rules.add(new CleanRule(item.getPattern(), item.isRegex(), true));
            }
        }
        return rules;
    }
    
    private void showHelp() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("使用说明");
        alert.setHeaderText("压缩包清理工具 - 使用指南");
        
        String help = """
        📦 功能说明:
        本工具用于清理压缩包中不需要的文件，特别适用于 Jenkins 构建包清理。
        
        🔧 使用步骤:
        1. 选择要清理的压缩包文件（支持 .zip, .jar, .war）
        2. 配置清理规则（支持正则表达式）
        3. 点击\"预览\"查看将要删除的文件
        4. 确认无误后点击\"开始清理\"
        
        📝 规则说明:
        • 正则表达式: 使用 Java 正则语法匹配文件路径
        • 简单模式: 使用包含匹配（contains）
        • 路径分隔符: 统一使用 / （不是 \\）
        
        💡 示例规则:
        • ^mdm/.*              删除 mdm 目录下所有文件
        • ^lib/(?!mdm-).*\\.jar$  保留 lib 下 mdm- 开头的 jar
        • \\.log$              删除所有 .log 文件
        • ^config/.*\\.bak$     删除 config 目录下的 .bak 文件
        
        ⚠️ 注意事项:
        • 原文件不会被修改，会生成新的清理后的文件
        • 建议先使用\"预览\"功能确认删除列表
        • 支持的文件格式: ZIP, JAR, WAR
        """;
        
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
        alert.setHeaderText("压缩包清理工具");
        alert.setContentText(
            "版本: 1.0.0\n" +
            "作者: lemostic\n" +
            "功能: 智能清理压缩包中的无用文件\n\n" +
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
     * 规则项（用于表格显示）
     */
    public static class RuleItem {
        private javafx.beans.property.BooleanProperty enabled;
        private javafx.beans.property.StringProperty pattern;
        private javafx.beans.property.StringProperty type;
        private javafx.beans.property.StringProperty description;
        
        public RuleItem(boolean enabled, String pattern, String type, String description) {
            this.enabled = new javafx.beans.property.SimpleBooleanProperty(enabled);
            this.pattern = new javafx.beans.property.SimpleStringProperty(pattern);
            this.type = new javafx.beans.property.SimpleStringProperty(type);
            this.description = new javafx.beans.property.SimpleStringProperty(description);
        }
        
        public boolean isEnabled() { return enabled.get(); }
        public void setEnabled(boolean value) { enabled.set(value); }
        public javafx.beans.property.BooleanProperty enabledProperty() { return enabled; }
        
        public String getPattern() { return pattern.get(); }
        public void setPattern(String value) { pattern.set(value); }
        public javafx.beans.property.StringProperty patternProperty() { return pattern; }
        
        public String getType() { return type.get(); }
        public void setType(String value) { type.set(value); }
        public javafx.beans.property.StringProperty typeProperty() { return type; }
        
        public String getDescription() { return description.get(); }
        public void setDescription(String value) { description.set(value); }
        public javafx.beans.property.StringProperty descriptionProperty() { return description; }
        
        public boolean isRegex() {
            return "正则".equals(type.get());
        }
    }
}

