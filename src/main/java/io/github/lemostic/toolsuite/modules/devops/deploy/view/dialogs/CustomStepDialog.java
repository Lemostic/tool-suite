package io.github.lemostic.toolsuite.modules.devops.deploy.view.dialogs;

import io.github.lemostic.toolsuite.modules.devops.deploy.model.CustomStepConfig;
import io.github.lemostic.toolsuite.modules.devops.deploy.service.CustomStepService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign.MaterialDesign;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 自定义步骤管理对话框
 */
public class CustomStepDialog extends Dialog<CustomStepConfig> {
    
    private final CustomStepService customStepService;
    private final CustomStepConfig editingStep;
    
    // UI组件
    private TextField nameField;
    private TextArea descriptionArea;
    private ComboBox<CustomStepConfig.StepType> typeComboBox;
    private CheckBox enabledCheckBox;
    private Spinner<Integer> orderSpinner;
    private TextArea parametersArea;
    
    public CustomStepDialog(CustomStepService customStepService) {
        this(customStepService, null);
    }
    
    public CustomStepDialog(CustomStepService customStepService, CustomStepConfig editingStep) {
        this.customStepService = customStepService;
        this.editingStep = editingStep;
        
        setTitle(editingStep == null ? "添加自定义步骤" : "编辑自定义步骤");
        setHeaderText(editingStep == null ? "创建新的部署步骤" : "修改现有部署步骤");
        
        // 设置图标
        Stage stage = (Stage) getDialogPane().getScene().getWindow();
        stage.getIcons().add(new javafx.scene.image.Image(
            getClass().getResourceAsStream("/io/github/lemostic/toolsuite/icon.png")));
        
        // 创建对话框内容
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        
        // 基本信息区域
        content.getChildren().add(createBasicInfoSection());
        
        // 参数配置区域
        content.getChildren().add(createParametersSection());
        
        getDialogPane().setContent(content);
        
        // 添加按钮
        ButtonType saveButtonType = new ButtonType("保存", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        // 设置结果转换器
        setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return createStepFromInput();
            }
            return null;
        });
        
        // 如果是编辑模式，填充现有数据
        if (editingStep != null) {
            populateFields();
        }
    }
    
    private VBox createBasicInfoSection() {
        VBox section = new VBox(10);
        section.setStyle("-fx-background-color: #f8f9fa; -fx-padding: 15; -fx-border-radius: 5;");
        
        Label titleLabel = new Label("基本信息");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        // 步骤名称
        HBox nameRow = new HBox(10);
        nameRow.setAlignment(Pos.CENTER_LEFT);
        Label nameLabel = new Label("步骤名称:");
        nameLabel.setPrefWidth(80);
        nameField = new TextField();
        nameField.setPromptText("输入步骤名称");
        HBox.setHgrow(nameField, Priority.ALWAYS);
        nameRow.getChildren().addAll(nameLabel, nameField);
        
        // 步骤描述
        HBox descRow = new HBox(10);
        descRow.setAlignment(Pos.CENTER_LEFT);
        Label descLabel = new Label("步骤描述:");
        descLabel.setPrefWidth(80);
        descriptionArea = new TextArea();
        descriptionArea.setPromptText("描述这个步骤的作用");
        descriptionArea.setPrefRowCount(2);
        HBox.setHgrow(descriptionArea, Priority.ALWAYS);
        descRow.getChildren().addAll(descLabel, descriptionArea);
        
        // 步骤类型
        HBox typeRow = new HBox(10);
        typeRow.setAlignment(Pos.CENTER_LEFT);
        Label typeLabel = new Label("步骤类型:");
        typeLabel.setPrefWidth(80);
        typeComboBox = new ComboBox<>();
        typeComboBox.setItems(FXCollections.observableArrayList(
            Arrays.asList(CustomStepConfig.StepType.values())));
        typeComboBox.setPromptText("选择步骤类型");
        HBox.setHgrow(typeComboBox, Priority.ALWAYS);
        typeRow.getChildren().addAll(typeLabel, typeComboBox);
        
        // 启用状态和顺序
        HBox optionsRow = new HBox(20);
        optionsRow.setAlignment(Pos.CENTER_LEFT);
        
        enabledCheckBox = new CheckBox("启用此步骤");
        enabledCheckBox.setSelected(true);
        
        Label orderLabel = new Label("执行顺序:");
        orderSpinner = new Spinner<>(1, 100, 1);
        orderSpinner.setEditable(true);
        orderSpinner.setPrefWidth(80);
        
        optionsRow.getChildren().addAll(enabledCheckBox, orderLabel, orderSpinner);
        
        section.getChildren().addAll(titleLabel, nameRow, descRow, typeRow, optionsRow);
        
        return section;
    }
    
    private VBox createParametersSection() {
        VBox section = new VBox(10);
        section.setStyle("-fx-background-color: #f8f9fa; -fx-padding: 15; -fx-border-radius: 5;");
        
        Label titleLabel = new Label("参数配置");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        Label hintLabel = new Label("根据选择的步骤类型配置相应参数（JSON格式）:");
        hintLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");
        
        parametersArea = new TextArea();
        parametersArea.setPromptText("{\n  \"param1\": \"value1\",\n  \"param2\": \"value2\"\n}");
        parametersArea.setPrefRowCount(6);
        parametersArea.setFont(javafx.scene.text.Font.font("Monaco", 12));
        
        // 类型选择监听器，提供参数模板
        typeComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                updateParameterTemplate(newVal);
            }
        });
        
        section.getChildren().addAll(titleLabel, hintLabel, parametersArea);
        
        return section;
    }
    
    private void updateParameterTemplate(CustomStepConfig.StepType type) {
        String template = switch (type) {
            case SSH_COMMAND -> "{\n  \"command\": \"your_ssh_command_here\",\n  \"timeoutSeconds\": 30\n}";
            case FILE_TRANSFER -> "{\n  \"source\": \"/local/path\",\n  \"target\": \"/remote/path\",\n  \"mode\": \"upload\"\n}";
            case SCRIPT_EXECUTION -> "{\n  \"scriptPath\": \"/path/to/script.sh\",\n  \"arguments\": []\n}";
            case HEALTH_CHECK -> "{\n  \"checkCommand\": \"ps aux | grep service\",\n  \"expectedOutput\": \"running\",\n  \"timeoutSeconds\": 30\n}";
            case CUSTOM_SCRIPT -> "{\n  \"scriptContent\": \"#!/bin/bash\\necho 'Hello World'\",\n  \"interpreter\": \"/bin/bash\"\n}";
        };
        parametersArea.setText(template);
    }
    
    private void populateFields() {
        nameField.setText(editingStep.getName());
        descriptionArea.setText(editingStep.getDescription());
        typeComboBox.setValue(editingStep.getType());
        enabledCheckBox.setSelected(editingStep.isEnabled());
        orderSpinner.getValueFactory().setValue(editingStep.getOrder());
        
        if (editingStep.getParameters() != null) {
            // 将参数转换为JSON字符串显示
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                String json = mapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(editingStep.getParameters());
                parametersArea.setText(json);
            } catch (Exception e) {
                parametersArea.setText(editingStep.getParameters().toString());
            }
        }
    }
    
    private CustomStepConfig createStepFromInput() {
        CustomStepConfig step = editingStep != null ? 
            new CustomStepConfig() {{ setId(editingStep.getId()); }} : 
            new CustomStepConfig();
            
        step.setName(nameField.getText());
        step.setDescription(descriptionArea.getText());
        step.setType(typeComboBox.getValue());
        step.setEnabled(enabledCheckBox.isSelected());
        step.setOrder(orderSpinner.getValue());
        
        // 解析参数
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> params = mapper.readValue(
                parametersArea.getText(), 
                new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {}
            );
            step.setParameters(params);
        } catch (Exception e) {
            // 如果解析失败，创建空参数映射
            step.setParameters(new HashMap<>());
        }
        
        return step;
    }
}