package io.github.lemostic.toolsuite.modules.helloworld;

import io.github.lemostic.toolsuite.util.ResourceLoader;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign.MaterialDesign;

public class HelloWorldView extends BorderPane {

  public HelloWorldView() {
    getStyleClass().add("module-background");
    
    // 创建工具栏
    ToolBar toolbar = new ToolBar();
    toolbar.setStyle("-fx-background-color: linear-gradient(to bottom, #ffffff, #e8e8e8); " +
                    "-fx-border-color: #d0d0d0; -fx-border-width: 0 0 1 0;");
    
    Button helpBtn = new Button("使用说明", new FontIcon(MaterialDesign.MDI_HELP_CIRCLE));
    helpBtn.setOnAction(e -> showHelp());
    
    Button aboutBtn = new Button("关于", new FontIcon(MaterialDesign.MDI_INFORMATION));
    aboutBtn.setOnAction(e -> showAbout());
    
    toolbar.getItems().addAll(helpBtn, aboutBtn);
    
    setTop(toolbar);
    setCenter(new Label("My first workbench module."));
  }
  
  private void showHelp() {
    Alert alert = new Alert(Alert.AlertType.INFORMATION);
    alert.setTitle("使用说明");
    alert.setHeaderText("HelloWorld模块 - 使用指南");
    
    String help = ResourceLoader.loadResourceFileForClass(getClass(), "help.txt");
    
    TextArea textArea = new TextArea(help);
    textArea.setEditable(false);
    textArea.setWrapText(true);
    textArea.setPrefHeight(300);
    
    alert.getDialogPane().setContent(textArea);
    alert.getDialogPane().setPrefWidth(500);
    alert.showAndWait();
  }
  
  private void showAbout() {
    Alert alert = new Alert(Alert.AlertType.INFORMATION);
    alert.setTitle("关于");
    alert.setHeaderText("HelloWorld模块");
    String about = ResourceLoader.loadResourceFileForClass(getClass(), "about.txt");
    alert.setContentText(about);
    alert.showAndWait();
  }
}
