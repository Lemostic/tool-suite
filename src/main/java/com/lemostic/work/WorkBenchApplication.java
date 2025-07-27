package com.lemostic.work;

import com.dlsc.workbenchfx.Workbench;
import com.dlsc.workbenchfx.view.controls.ToolbarItem;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import com.lemostic.work.controls.*;
import com.lemostic.work.modules.datamigrator.DataMigrateModule;
import com.lemostic.work.modules.deployment.PackageDeploymentModule;
import com.lemostic.work.modules.preferences.Preferences;
import com.lemostic.work.modules.preferences.PreferencesModule;
import fr.brouillard.oss.cssfx.CSSFX;
import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.MenuItem;
import javafx.stage.Stage;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign.MaterialDesign;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkBenchApplication extends Application {

    private static final Logger logger = LoggerFactory.getLogger(WorkBenchApplication.class);
    private Workbench workbench;
    private Preferences preferences;

    private PreferencesModule preferencesModule;

    private DataMigrateModule dataMigrateModule;

    private PackageDeploymentModule packageDeploymentModule;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        logger.info("应用启动中...");

        preferences = new Preferences();
        preferencesModule = new PreferencesModule(preferences);
        dataMigrateModule = new DataMigrateModule();
        packageDeploymentModule = new PackageDeploymentModule();

        Scene myScene = new Scene(initWorkbench());

        CSSFX.start(myScene);

        primaryStage.setTitle("数据治理工具");
        primaryStage.setScene(myScene);
        primaryStage.setWidth(1000);
        primaryStage.setHeight(700);
        primaryStage.show();
        primaryStage.centerOnScreen();

        initNightMode();
    }

    private Workbench initWorkbench() {
        // Navigation Drawer
        MenuItem dataMigrateMenuItem = new MenuItem("数据迁移", createIcon(MaterialDesign.MDI_TRANSFER));
        MenuItem packageDeploymentMenuItem = new MenuItem("包部署", createIcon(MaterialDesign.MDI_CLOUD_UPLOAD));

        MenuItem showOverlay = new MenuItem("Show overlay");
        MenuItem showBlockingOverlay = new MenuItem("Show blocking overlay");

        // Toolbar
        /*ToolbarItem addPreferences = new ToolbarItem("Add", new FontIcon(FontAwesome.GEARS));
        ToolbarItem removePreferences = new ToolbarItem("Remove", new FontIcon(FontAwesome.GEARS));
        ToolbarItem showDialogButton = new ToolbarItem("Show", new FontIcon(FontAwesome.GEARS));*/

        // Settings
        MenuItem settingsMenuItem = new MenuItem("设置", createIcon(MaterialDesign.MDI_SETTINGS));
        // WorkbenchFX
        workbench = Workbench.builder(
                        dataMigrateModule,
                        packageDeploymentModule,
                        preferencesModule
                )
                .toolbarLeft(
                        new ToolbarItem("WorkbenchFX")
                )
                /*.toolbarRight(
                        new ToolbarItem(
                                new ImageView(WorkBenchApplication.class.getResource("user.png").toExternalForm()),
                                new Menu(
                                        "Submenus",
                                        new FontIcon(FontAwesome.PLUS),
                                        new MenuItem("Submenu 1"),
                                        new CustomMenuItem(new Label("CustomMenuItem"), false))),
                        new ToolbarItem(
                                "Text",
                                new ImageView(WorkBenchApplication.class.getResource("user.png").toExternalForm()),
                                new CustomMenuItem(new Label("Content 1")),
                                new CustomMenuItem(new Label("Content 2"))))*/
                .modulesPerPage(9)
                .pageFactory(CustomPage::new)
                .tabFactory(CustomTab::new)
                .tileFactory(CustomTile::new)
                .navigationDrawer(new CustomNavigationDrawer())
                .navigationDrawerItems(
                        dataMigrateMenuItem, packageDeploymentMenuItem, showOverlay, showBlockingOverlay, settingsMenuItem)
                .build();


        CustomOverlay customOverlay = new CustomOverlay(workbench, false);
        CustomOverlay blockingCustomOverlay = new CustomOverlay(workbench, true);
        showOverlay.setOnAction(event -> workbench.showOverlay(customOverlay, false));
        showBlockingOverlay.setOnAction(event -> workbench.showOverlay(blockingCustomOverlay, true));

        // 数据迁移菜单点击处理
        dataMigrateMenuItem.setOnAction(event -> {
            workbench.hideDrawer();
            workbench.openModule(dataMigrateModule);
        });

        // 包部署菜单点击处理
        packageDeploymentMenuItem.setOnAction(event -> {
            workbench.hideDrawer();
            workbench.openModule(packageDeploymentModule);
        });

        settingsMenuItem.setOnAction(event -> {
            workbench.hideDrawer();
            workbench.openModule(preferencesModule);
        });
        workbench.getStylesheets().add(WorkBenchApplication.class.getResource("customTheme.css").toExternalForm());
        workbench.getStylesheets().add(WorkBenchApplication.class.getResource("customOverlay.css").toExternalForm());

        return workbench;
    }

    private Node createIcon(Ikon icon) {
        return new FontIcon(icon);
    }

    private void initNightMode() {
        // initially set stylesheet
        setNightMode(preferences.isNightMode());

        // change stylesheet depending on whether nightmode is on or not
        preferences.nightModeProperty().addListener((observable, oldValue, newValue) -> {
            System.out.println("Night mode changed to " + newValue);
            setNightMode(newValue);
        });
    }

    private void setNightMode(boolean on) {
        String customTheme = WorkBenchApplication.class.getResource("customTheme.css").toExternalForm();
        String darkTheme = WorkBenchApplication.class.getResource("darkTheme.css").toExternalForm();
        ObservableList<String> stylesheets = workbench.getStylesheets();
        if (on) {
            workbench.getStylesheets().removeAll(stylesheets);
            stylesheets.add(darkTheme);
        } else {
            workbench.getStylesheets().removeAll(stylesheets);
            stylesheets.add(customTheme);
        }
    }
}
