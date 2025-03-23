package com.lemostic.work.modules.datamigrator.view;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.lemostic.work.enums.DBEnum;
import com.lemostic.work.modules.datamigrator.service.MigrationService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;

/**
 * 数据迁移试图
 */
public class DataMigrateView extends StackPane {

    private static final int LABEL_WIDTH = 100;
    private static final int FORM_ITEM_WIDTH = 300;

    private MigrationService migrationService;

    /**
     * 数据源连接状态
     * 0：未连接
     * 1：源库已连接
     * 2：目标库已连接
     */
    private int connectStatus = 0;

    private ComboBox<String> sourceDbTypeComboBox;
    private ComboBox<String> sourceDriverComboBox;
    private TextField sourceUrlTextField;
    private TextField sourceUsernameTextField;
    private PasswordField sourcePasswordTextField;
    private TextField sourceDamDatabaseSchema;
    private Button sourceTestConnectionButton;
    // 拷贝的实体表模型Map
    private ComboBox<ModelRelationship> copyModelMapComboBox;
    private TextArea copyDataInfoTextArea = new TextArea();
    private TextArea processInfoTextArea = new TextArea();
    private ComboBox<String> targetDbTypeComboBox;
    private ComboBox<String> targetDriverComboBox;
    private TextField targetUrlTextField;
    private TextField targetUsernameTextField;
    private PasswordField targetPasswordTextField;
    private TextField targetDamDatabaseSchema;
    private Button targetTestConnectionButton;
    private Button startCopyButton;
    private Label sourceConnectionStatusLabel;
    private Label targetConnectionStatusLabel;

    public DataMigrateView(MigrationService migrationService) {
        this.migrationService = migrationService;
        // 左侧表单
        VBox leftForm = createForm(DatasourceCategory.SOURCE);
        HBox sourceTestConnectionFormBox = new HBox(10);
        sourceTestConnectionFormBox.setPadding(new Insets(20));
        sourceTestConnectionButton = new Button("测试连接");
        sourceConnectionStatusLabel = new Label();
        sourceTestConnectionFormBox.getChildren().addAll(sourceTestConnectionButton, sourceConnectionStatusLabel);
        leftForm.getChildren().add(sourceTestConnectionFormBox);
        initMigrateInfoMap();

        // 右侧表单
        VBox rightForm = createForm(DatasourceCategory.TARGET);
        HBox targetTestConnectionFormBox = new HBox(10);
        targetTestConnectionFormBox.setPadding(new Insets(20));
        targetTestConnectionButton = new Button("测试连接");
        targetConnectionStatusLabel = new Label();
        targetTestConnectionFormBox.getChildren().addAll(targetTestConnectionButton, targetConnectionStatusLabel);
        rightForm.getChildren().add(targetTestConnectionFormBox);

        // 底部区域
        VBox bottomBox = new VBox(10);
        copyDataInfoTextArea.setPromptText("拷贝数据信息");
        copyDataInfoTextArea.setEditable(false);
        copyDataInfoTextArea.setStyle("-fx-font-size: 14px;");
        processInfoTextArea.setPromptText("拷贝详情");
        processInfoTextArea.setEditable(false);
        processInfoTextArea.setStyle("-fx-font-size: 14px;");

        // 切换拷贝对象事件
        copyModelMapComboBox.setOnAction(event -> {
            ModelRelationship modelRelationship = copyModelMapComboBox.getValue();
            StringBuilder migrateDetail = new StringBuilder();
            copyDataInfoTextArea.setText(migrateDetail.toString());
        });

        // 主对象数据id
        TextField mainObjectIdTextField = new TextField();
        mainObjectIdTextField.setPromptText("主对象数据id，如：拷贝模型数据，填写tm_model.id的值，多个值以英文逗号分隔");
        mainObjectIdTextField.setStyle("-fx-font-size: 14px;");
        mainObjectIdTextField.setPrefWidth(500);
        bottomBox.getChildren().addAll(copyModelMapComboBox, mainObjectIdTextField, copyDataInfoTextArea, processInfoTextArea);

        startCopyButton = new Button("开始拷贝数据");

        // 布局
        HBox formsBox = new HBox(20, leftForm, rightForm);
        HBox.setHgrow(leftForm, Priority.ALWAYS); // 设置左侧表单的宽度增长优先级
        HBox.setHgrow(rightForm, Priority.ALWAYS); // 设置右侧表单的宽度增长优先级
        VBox root = new VBox(20, formsBox, bottomBox, startCopyButton);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.CENTER);
        getChildren().add(root);

        // 事件处理
        sourceTestConnectionButton.setOnAction(testConnectionAction(sourceDbTypeComboBox, sourceDriverComboBox, sourceUrlTextField, sourceUsernameTextField, sourcePasswordTextField, sourceConnectionStatusLabel));
        targetTestConnectionButton.setOnAction(testConnectionAction(targetDbTypeComboBox, sourceDriverComboBox, targetUrlTextField, targetUsernameTextField, targetPasswordTextField, targetConnectionStatusLabel));
        startCopyButton.setOnAction(startCopyAction());
    }

    private void initMigrateInfoMap() {
        copyModelMapComboBox = new ComboBox<>();
        copyModelMapComboBox.setPrefWidth(200);
        ObservableList<ModelRelationship> items = FXCollections.observableArrayList();
        TableItem modelItem = new TableItem("TM_MODEL", "ID", "", "", "");
        TableItem modelVerItem = new TableItem("TM_MODEL_VER", "ID", "MODEL_VER_ID", modelItem.name, modelItem.primaryKey);
        TableItem modelVerFieldItem = new TableItem("TM_MODEL_VER_FIELD", "ID", "MODEL_VER_ID", modelVerItem.name, modelVerItem.primaryKey);
        ModelRelationship modelRelationship = new ModelRelationship("模型数据");
        modelRelationship.setRelationships(CollectionUtil.newLinkedList(modelItem, modelVerItem, modelVerFieldItem));
        items.add(modelRelationship);
        copyModelMapComboBox.setItems(items);
    }

    private VBox createForm(DatasourceCategory datasourceCategory) {
        VBox form = new VBox(10);
        form.setAlignment(Pos.CENTER_LEFT);
        form.setPadding(new Insets(10));
        form.setId(datasourceCategory.getValue());

        switch (datasourceCategory) {
            case SOURCE:
                addRow(form, datasourceCategory.getName(), sourceDbTypeComboBox = createDbTypeComboBox(DBEnum.nameList()));
                addRow(form, "数据库驱动", sourceDriverComboBox = createDriverClassComboBox(DBEnum.classNameList()));
                addRow(form, "URL", sourceUrlTextField = createUrlTextField());
                addRow(form, "用户名", sourceUsernameTextField = createUsernameTextField());
                addRow(form, "密码", sourcePasswordTextField = createPasswordField());
                addRow(form, "Schema", sourceDamDatabaseSchema = createSchemaTextField());
                sourceDbTypeComboBox.setOnAction(event -> {
                    clearDataInfo(datasourceCategory);
                    DBEnum dbEnum = DBEnum.of(sourceDbTypeComboBox.getValue());
                    sourceDriverComboBox.setValue(dbEnum.getClassName());
                });
                break;
            case TARGET:
                addRow(form, datasourceCategory.getName(), targetDbTypeComboBox = createDbTypeComboBox(DBEnum.nameList()));
                addRow(form, "数据库驱动", targetDriverComboBox = createDriverClassComboBox(DBEnum.classNameList()));
                addRow(form, "URL", targetUrlTextField = createUrlTextField());
                addRow(form, "用户名", targetUsernameTextField = createUsernameTextField());
                addRow(form, "密码", targetPasswordTextField = createPasswordField());
                addRow(form, "Schema", targetDamDatabaseSchema = createSchemaTextField());
                targetDbTypeComboBox.setOnAction(event -> {
                    clearDataInfo(datasourceCategory);
                    DBEnum dbEnum = DBEnum.of(targetDbTypeComboBox.getValue());
                    targetDriverComboBox.setValue(dbEnum.getClassName());
                });
                break;
            default:
                break;
        }

        return form;
    }

    private EventHandler<ActionEvent> testConnectionAction(ComboBox<String> dbTypeComboBox, ComboBox<String> driverComboBox, TextField urlTextField, TextField usernameTextField, PasswordField passwordTextField, Label statusLabel) {
        return event -> {
            String dbType = dbTypeComboBox.getValue();
            String driver = driverComboBox.getValue();
            String url = urlTextField.getText().trim();
            String username = usernameTextField.getText().trim();
            String password = passwordTextField.getText().trim();

            if (StrUtil.isBlank(dbType) || StrUtil.isBlank(driver) || StrUtil.isBlank(url) || StrUtil.isBlank(username) || StrUtil.isBlank(password)) {
                statusLabel.setText("请填写完整信息");
                statusLabel.setTextFill(Color.RED);
                return;
            }
            try {
                Class.forName(driver);
                Connection connection = DriverManager.getConnection(url, username, password);
                statusLabel.setText("连接成功");
                statusLabel.setTextFill(Color.GREEN);

                connection.close();
            } catch (Exception e) {
                statusLabel.setText("连接失败: " + e.getMessage());
                statusLabel.setTextFill(Color.RED);
            }
        };
    }

    private EventHandler<ActionEvent> startCopyAction() {
        return event -> {

            // 获取右侧数据库连接信息
            String targetDbType = targetDbTypeComboBox.getValue();
            String targetDriver = targetDriverComboBox.getValue();
            String targetUrl = targetUrlTextField.getText();
            String targetUsername = targetUsernameTextField.getText();
            String targetPassword = targetPasswordTextField.getText();

            try {
                Class.forName(targetDriver);
                Connection targetConnection = DriverManager.getConnection(targetUrl, targetUsername, targetPassword);
                Statement targetStatement = targetConnection.createStatement();

                targetConnection.close();
                sourceConnectionStatusLabel.setText("数据复制成功");
                sourceConnectionStatusLabel.setTextFill(Color.GREEN);
            } catch (Exception e) {
                sourceConnectionStatusLabel.setText("数据复制失败: " + e.getMessage());
                sourceConnectionStatusLabel.setTextFill(Color.RED);
            }
        };
    }

    private static class TableModel {
        private final SimpleStringProperty name;

        public TableModel(String name) {
            this.name = new SimpleStringProperty(name);
        }

        public String getName() {
            return name.get();
        }

        public SimpleStringProperty nameProperty() {
            return name;
        }

        public void setName(String name) {
            this.name.set(name);
        }
    }

    public static enum DatasourceCategory {
        SOURCE("源数据库", "source"),
        TARGET("目标数据库", "target");

        private final String name;
        private final String value;

        DatasourceCategory(String name, String value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public String getValue() {
            return value;
        }

        // is方法，用于判断name是不是这个枚举
        public boolean is(String value) {
            return this.value.equals(value);
        }
    }

    private void addRow(VBox form, String labelName, Control control) {
        HBox row = new HBox(10); // 每一行使用 HBox，水平排列 Label 和输入控件
        row.setAlignment(Pos.CENTER_LEFT);

        Label label = new Label(labelName + ":"); // 创建 Label，并添加冒号提升可读性
        label.setPrefWidth(LABEL_WIDTH);
        label.setMinWidth(LABEL_WIDTH);
        row.getChildren().addAll(label, control); // 将 Label 和输入控件添加到 HBox 中
        form.getChildren().add(row); // 将 HBox 添加到 VBox 中
    }

    private ComboBox<String> createDbTypeComboBox(List<String> items) {
        ComboBox<String> comboBox = new ComboBox<>(FXCollections.observableArrayList(items));
        comboBox.setPrefWidth(FORM_ITEM_WIDTH); // 设置宽度以确保一致性
        return comboBox;
    }

    private ComboBox<String> createDriverClassComboBox(List<String> items) {
        ComboBox<String> comboBox = new ComboBox<>(FXCollections.observableArrayList(items));
        comboBox.setPrefWidth(FORM_ITEM_WIDTH); // 设置宽度以确保一致性
        return comboBox;
    }

    private TextField createUrlTextField() {
        TextField textField = new TextField();
        textField.setPromptText("请输入数据库 URL");
        textField.setPrefWidth(FORM_ITEM_WIDTH);
        return textField;
    }

    private TextField createUsernameTextField() {
        TextField textField = new TextField();
        textField.setPromptText("请输入用户名");
        textField.setPrefWidth(FORM_ITEM_WIDTH);
        return textField;
    }

    private PasswordField createPasswordField() {
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("请输入密码");
        passwordField.setPrefWidth(FORM_ITEM_WIDTH);
        return passwordField;
    }

    private TextField createSchemaTextField() {
        TextField textField = new TextField();
        textField.setPromptText("请输入数据库操作对象，可为空");
        textField.setPrefWidth(FORM_ITEM_WIDTH);
        return textField;
    }

    private void clearDataInfo(DatasourceCategory datasourceCategory) {
        switch (datasourceCategory) {
            case SOURCE:
                sourceUrlTextField.setText("");
                sourceUsernameTextField.setText("");
                sourcePasswordTextField.setText("");
                sourceDamDatabaseSchema.setText("");
                break;
            case TARGET:
                targetUrlTextField.setText("");
                targetUsernameTextField.setText("");
                targetPasswordTextField.setText("");
                targetDamDatabaseSchema.setText("");
            default:
                break;
        }
    }

    /**
     * 模型关系
     */
    public class ModelRelationship {
        private String name;
        private LinkedList<TableItem> relationships = new LinkedList<>();

        public ModelRelationship() {
        }

        public ModelRelationship(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public LinkedList<TableItem> getRelationships() {
            return relationships;
        }

        public void setRelationships(LinkedList<TableItem> relationships) {
            this.relationships = relationships;
        }

        @Override
        public String toString() {
            return name;
        }


    }
    public class TableItem {
        private String name;
        private String primaryKey = "";
        private String foreignKey = "";
        private String referenceTable = "";
        private String referenceTableKey = "";

        public TableItem() {
        }

        public TableItem(String name, String primaryKey, String foreignKey, String referenceTable, String referenceTableKey) {
            this.name = name;
            this.primaryKey = primaryKey;
            this.foreignKey = foreignKey;
            this.referenceTable = referenceTable;
            this.referenceTableKey = referenceTableKey;
        }

        public String getReferenceTableKey() {
            return referenceTableKey;
        }

        public void setReferenceTableKey(String referenceTableKey) {
            this.referenceTableKey = referenceTableKey;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPrimaryKey() {
            return primaryKey;
        }

        public void setPrimaryKey(String primaryKey) {
            this.primaryKey = primaryKey;
        }

        public String getReferenceTable() {
            return referenceTable;
        }

        public void setReferenceTable(String referenceTable) {
            this.referenceTable = referenceTable;
        }

        public String getForeignKey() {
            return foreignKey;
        }

        public void setForeignKey(String foreignKey) {
            this.foreignKey = foreignKey;
        }
    }
}
