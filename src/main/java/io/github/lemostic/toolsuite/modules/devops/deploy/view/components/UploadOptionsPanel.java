package io.github.lemostic.toolsuite.modules.devops.deploy.view.components;

import io.github.lemostic.toolsuite.modules.devops.deploy.model.UploadOptions;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class UploadOptionsPanel extends VBox {
    
    private CheckBox updatePermissionsCheck;
    private TextField permissionsField;
    private TextField ownerField;
    private TextField groupField;
    private CheckBox preserveTimestampCheck;
    
    public UploadOptionsPanel() {
        initializeUI();
    }
    
    private void initializeUI() {
        setSpacing(10);
        setPadding(new Insets(10));
        
        // 标题
        Label titleLabel = new Label("上传选项");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        // 权限设置
        updatePermissionsCheck = new CheckBox("上传后更新文件权限");
        
        GridPane permGrid = new GridPane();
        permGrid.setHgap(10);
        permGrid.setVgap(8);
        permGrid.setPadding(new Insets(0, 0, 0, 20));
        permGrid.setDisable(true);
        
        permissionsField = new TextField("755");
        permissionsField.setPrefWidth(80);
        permissionsField.setPromptText("755");
        
        ownerField = new TextField();
        ownerField.setPromptText("用户名");
        
        groupField = new TextField();
        groupField.setPromptText("用户组");
        
        permGrid.add(new Label("权限:"), 0, 0);
        permGrid.add(permissionsField, 1, 0);
        permGrid.add(new Label("(如: 755, 644)"), 2, 0);
        
        permGrid.add(new Label("所有者:"), 0, 1);
        permGrid.add(ownerField, 1, 1);
        
        permGrid.add(new Label("用户组:"), 0, 2);
        permGrid.add(groupField, 1, 2);
        
        updatePermissionsCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            permGrid.setDisable(!newVal);
        });
        
        // 时间戳
        preserveTimestampCheck = new CheckBox("保留文件时间戳");
        preserveTimestampCheck.setSelected(true);
        
        // 提示
        Label tipLabel = new Label("提示: 权限设置需要SSH用户具有足够的权限");
        tipLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666; -fx-font-style: italic;");
        
        getChildren().addAll(
            titleLabel,
            new Separator(),
            updatePermissionsCheck,
            permGrid,
            new Separator(),
            preserveTimestampCheck,
            tipLabel
        );
    }
    
    public UploadOptions getOptions() {
        UploadOptions options = new UploadOptions();
        options.setUpdatePermissions(updatePermissionsCheck.isSelected());
        options.setFilePermissions(permissionsField.getText());
        options.setOwner(ownerField.getText());
        options.setGroup(groupField.getText());
        options.setPreserveTimestamp(preserveTimestampCheck.isSelected());
        return options;
    }
    
    public void setOptions(UploadOptions options) {
        if (options == null) return;
        
        updatePermissionsCheck.setSelected(options.getUpdatePermissions());
        permissionsField.setText(options.getFilePermissions());
        ownerField.setText(options.getOwner());
        groupField.setText(options.getGroup());
        preserveTimestampCheck.setSelected(options.getPreserveTimestamp());
    }
}
