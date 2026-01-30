package io.github.lemostic.toolsuite;

import com.dlsc.workbenchfx.Workbench;
import com.dlsc.workbenchfx.model.WorkbenchModule;
import com.dlsc.workbenchfx.view.controls.ToolbarItem;
import io.github.lemostic.toolsuite.controls.*;
import io.github.lemostic.toolsuite.core.ModuleLoader;
import io.github.lemostic.toolsuite.core.module.ModuleRegistry;
import io.github.lemostic.toolsuite.modules.preferences.Preferences;
import fr.brouillard.oss.cssfx.CSSFX;
import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
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
        
        // 销毁 SPI 提供者
        io.github.lemostic.toolsuite.core.ModuleLoader.destroySpiProviders();

        logger.info("应用已关闭");
    }

    private Workbench initWorkbench() {
        int modulesPerPage = Math.max(6, Math.min(24, preferences.getModulesPerPage()));

        workbench = Workbench.builder(
                        modules.toArray(new WorkbenchModule[0])
                )
                .toolbarLeft(new ToolbarItem("数据治理工具"))
                .modulesPerPage(modulesPerPage)
                .pageFactory(CustomPage::new)
                .tabFactory(CustomTab::new)
                .tileFactory(CustomTile::new)
                .navigationDrawer(new CustomNavigationDrawer())
                .navigationDrawerItems(new MenuItem[0])
                .build();

        // 根据插件列表自动生成左侧抽屉菜单树（按 @ToolModule 的 menuGroup/category 分组）
        List<MenuItem> drawerItems = buildDrawerItemsFromModules(workbench, modules);
        // MenuItem showOverlay = new MenuItem("显示覆盖层");
        // MenuItem showBlockingOverlay = new MenuItem("显示阻塞覆盖层");
        // drawerItems.add(showOverlay);
        // drawerItems.add(showBlockingOverlay);
        workbench.getNavigationDrawer().getItems().addAll(drawerItems);

        // CustomOverlay customOverlay = new CustomOverlay(workbench, false);
        // CustomOverlay blockingCustomOverlay = new CustomOverlay(workbench, true);
        // showOverlay.setOnAction(event -> workbench.showOverlay(customOverlay, false));
        // showBlockingOverlay.setOnAction(event -> workbench.showOverlay(blockingCustomOverlay, true));


        workbench.getStylesheets().add(WorkBenchApplication.class.getResource("customTheme.css").toExternalForm());
        workbench.getStylesheets().add(WorkBenchApplication.class.getResource("customOverlay.css").toExternalForm());

        return workbench;
    }

    /**
     * 根据已加载的模块列表，按 @ToolModule 的 menuGroup/category 分组生成左侧抽屉菜单树。
     * 每个分组对应一个 Menu（子菜单），其下为可点击打开对应模块的 MenuItem。
     */
    private List<MenuItem> buildDrawerItemsFromModules(Workbench workbench, List<WorkbenchModule> modules) {
        List<MenuItem> result = new ArrayList<>();
        for (ModuleRegistry.GroupedModules group : ModuleRegistry.getGroupedModulesForDrawer(modules)) {
            Menu menu = new Menu(group.getGroupName());
            for (WorkbenchModule module : group.getModules()) {
                MenuItem item = new MenuItem(module.getName(), (Node) module.getIcon());
                item.setOnAction(e -> {
                    workbench.openModule(module);
                    workbench.hideNavigationDrawer();
                });
                menu.getItems().add(item);
            }
            result.add(menu);
        }
        return result;
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