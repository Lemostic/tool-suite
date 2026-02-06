package io.github.lemostic.toolsuite.modules.devops.deploy.view.components;

import io.github.lemostic.toolsuite.modules.devops.deploy.model.BackupOptions;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class BackupOptionsPanel extends VBox {
    
    private RadioButton allFilesRadio;
    private RadioButton selectiveRadio;
    private RadioButton patternRadio;
    
    private TextArea fileListArea;
    private TextField patternField;
    private CheckBox deleteAfterBackupCheck;
    
    public BackupOptionsPanel() {
        initializeUI();
    }
    
    private void initializeUI() {
        setSpacing(10);
        setPadding(new Insets(10));
        
        // 标题
        Label titleLabel = new Label("备份选项");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        // 备份模式选择
        ToggleGroup group = new ToggleGroup();
        
        allFilesRadio = new RadioButton("备份所有文件 (*)");
        allFilesRadio.setToggleGroup(group);
        allFilesRadio.setSelected(true);
        
        selectiveRadio = new RadioButton("选择性备份（指定文件/目录列表）");
        selectiveRadio.setToggleGroup(group);
        
        fileListArea = new TextArea();
        fileListArea.setPromptText("输入要备份的文件或目录，每行一个\n例如:\nlib/\nconfig/\n*.jar");
        fileListArea.setPrefRowCount(4);
        fileListArea.setDisable(true);
        
        selectiveRadio.selectedProperty().addListener((obs, oldVal, newVal) -> {
            fileListArea.setDisable(!newVal);
        });
        
        patternRadio = new RadioButton("按模式匹配");
        patternRadio.setToggleGroup(group);
        
        patternField = new TextField();
        patternField.setPromptText("例如: *.jar, *.xml");
        patternField.setDisable(true);
        
        patternRadio.selectedProperty().addListener((obs, oldVal, newVal) -> {
            patternField.setDisable(!newVal);
        });
        
        // 删除选项
        deleteAfterBackupCheck = new CheckBox("备份成功后删除原程序文件");
        deleteAfterBackupCheck.setSelected(true);
        
        // 提示
        Label tipLabel = new Label("提示: 备份文件将存储在服务器的备份目录中，按时间戳命名");
        tipLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666; -fx-font-style: italic;");
        
        getChildren().addAll(
            titleLabel,
            new Separator(),
            allFilesRadio,
            selectiveRadio,
            fileListArea,
            patternRadio,
            patternField,
            new Separator(),
            deleteAfterBackupCheck,
            tipLabel
        );
    }
    
    public BackupOptions getOptions() {
        BackupOptions options = new BackupOptions();
        
        if (allFilesRadio.isSelected()) {
            options.setMode(BackupOptions.BackupMode.ALL);
        } else if (selectiveRadio.isSelected()) {
            options.setMode(BackupOptions.BackupMode.SELECTIVE);
            String[] files = fileListArea.getText().split("\\n");
            for (String file : files) {
                if (!file.trim().isEmpty()) {
                    options.getSelectedFiles().add(file.trim());
                }
            }
        } else {
            options.setMode(BackupOptions.BackupMode.PATTERN);
            options.setPattern(patternField.getText());
        }
        
        options.setDeleteAfterBackup(deleteAfterBackupCheck.isSelected());
        
        return options;
    }
    
    public void setOptions(BackupOptions options) {
        if (options == null) return;
        
        switch (options.getMode()) {
            case ALL:
                allFilesRadio.setSelected(true);
                break;
            case SELECTIVE:
                selectiveRadio.setSelected(true);
                StringBuilder sb = new StringBuilder();
                for (String file : options.getSelectedFiles()) {
                    sb.append(file).append("\n");
                }
                fileListArea.setText(sb.toString());
                break;
            case PATTERN:
                patternRadio.setSelected(true);
                patternField.setText(options.getPattern());
                break;
        }
        
        deleteAfterBackupCheck.setSelected(options.getDeleteAfterBackup());
    }
}
