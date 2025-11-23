package io.github.lemostic.toolsuite;

import com.dlsc.workbenchfx.Workbench;
import com.dlsc.workbenchfx.model.WorkbenchModule;
import com.dlsc.workbenchfx.view.controls.ToolbarItem;
import io.github.lemostic.toolsuite.controls.*;
import io.github.lemostic.toolsuite.core.ModuleLoader;
import io.github.lemostic.toolsuite.modules.preferences.Preferences;
import fr.brouillard.oss.cssfx.CSSFX;
import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.MenuItem;
import javafx.stage.Stage;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class WorkBenchApplication extends Application {

    private static final Logger logger = LoggerFactory.getLogger(WorkBenchApplication.class);
    
    private Workbench workbench;
    private Preferences preferences;
    private List<WorkbenchModule> modules;

    public static void main(String[] args) {
        Application.launch(args);
    }


    @Override
    public void start(Stage primaryStage) {
        logger.info("应用启动中...");
        
        // 创建偏好设置
        preferences = new Preferences();
        
        // 自动加载所有模块
        modules = ModuleLoader.loadModules(preferences);

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
    
    @Override
    public void stop() {
        logger.info("应用关闭中...");

        logger.info("应用已关闭");
    }

    private Workbench initWorkbench() {
        // Navigation Drawer
        MenuItem showOverlay = new MenuItem("Show overlay");
        MenuItem showBlockingOverlay = new MenuItem("Show blocking overlay");

        // 动态创建导航项
        List<MenuItem> drawerItems = new ArrayList<>();
        // 追加覆盖层项
        drawerItems.add(showOverlay);
        drawerItems.add(showBlockingOverlay);

        // WorkbenchFX
        workbench = Workbench.builder(
                        modules.toArray(new WorkbenchModule[0])
                )
                .toolbarLeft(
                        new ToolbarItem("WorkbenchFX")
                )
                .modulesPerPage(9)
                .pageFactory(CustomPage::new)
                .tabFactory(CustomTab::new)
                .tileFactory(CustomTile::new)
                .navigationDrawer(new CustomNavigationDrawer())
                .navigationDrawerItems(drawerItems.toArray(new MenuItem[0]))
                .build();


        CustomOverlay customOverlay = new CustomOverlay(workbench, false);
        CustomOverlay blockingCustomOverlay = new CustomOverlay(workbench, true);
        showOverlay.setOnAction(event -> workbench.showOverlay(customOverlay, false));
        showBlockingOverlay.setOnAction(event -> workbench.showOverlay(blockingCustomOverlay, true));


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