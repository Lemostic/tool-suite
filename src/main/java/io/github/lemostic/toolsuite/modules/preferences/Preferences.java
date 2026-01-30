package io.github.lemostic.toolsuite.modules.preferences;

import com.dlsc.formsfx.model.structure.Field;
import com.dlsc.formsfx.model.structure.IntegerField;
import com.dlsc.formsfx.model.validators.DoubleRangeValidator;
import com.dlsc.preferencesfx.PreferencesFx;
import com.dlsc.preferencesfx.formsfx.view.controls.IntegerSliderControl;
import com.dlsc.preferencesfx.model.Category;
import com.dlsc.preferencesfx.model.Group;
import com.dlsc.preferencesfx.model.Setting;
import com.dlsc.preferencesfx.view.PreferencesFxView;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.Arrays;

/**
 * Model object for Preferences.
 */
public class Preferences {

    public PreferencesFx preferencesFx;

    // General
    StringProperty welcomeText = new SimpleStringProperty("你好，欢迎使用数据治理工具");
    IntegerProperty brightness = new SimpleIntegerProperty(50);
    BooleanProperty nightMode = new SimpleBooleanProperty(true);

    // Screen
    DoubleProperty scaling = new SimpleDoubleProperty(1);
    StringProperty screenName = new SimpleStringProperty("PreferencesFx Monitor");

    ObservableList<String> resolutionItems = FXCollections.observableArrayList(Arrays.asList(
            "1024x768", "1280x1024", "1440x900", "1920x1080")
    );
    ObjectProperty<String> resolutionSelection = new SimpleObjectProperty<>("1024x768");

    ListProperty<String> orientationItems = new SimpleListProperty<>(
            FXCollections.observableArrayList(Arrays.asList("Vertical", "Horizontal"))
    );
    ObjectProperty<String> orientationSelection = new SimpleObjectProperty<>("Vertical");

    IntegerProperty fontSize = new SimpleIntegerProperty(12);
    DoubleProperty lineSpacing = new SimpleDoubleProperty(1.5);

    // Favorites
    ListProperty<String> favoritesItems = new SimpleListProperty<>(
            FXCollections.observableArrayList(Arrays.asList(
                            "eMovie", "Eboda Phot-O-Shop", "Mikesoft Text",
                            "Mikesoft Numbers", "Mikesoft Present", "IntelliG"
                    )
            )
    );
    ListProperty<String> favoritesSelection = new SimpleListProperty<>(
            FXCollections.observableArrayList(Arrays.asList(
                    "Eboda Phot-O-Shop", "Mikesoft Text"))
    );

    // Custom Control
    IntegerProperty customControlProperty = new SimpleIntegerProperty(42);
    IntegerField customControl = setupCustomControl();

    // WorkbenchFX 相关
    IntegerProperty modulesPerPage = new SimpleIntegerProperty(9);
    BooleanProperty drawerOverlay = new SimpleBooleanProperty(false);
    BooleanProperty toolbarLeft = new SimpleBooleanProperty(true);

    public Preferences() {
        preferencesFx = createPreferences();
    }

    private IntegerField setupCustomControl() {
        return Field.ofIntegerType(customControlProperty).render(
                new IntegerSliderControl(0, 42));
    }

    private PreferencesFx createPreferences() {
        return PreferencesFx.of(PreferencesModule.class,
                Category.of("General",
                        Group.of("Greeting",
                                Setting.of("Welcome Text", welcomeText)
                        ),
                        Group.of("Display",
                                Setting.of("Brightness", brightness),
                                Setting.of("Night mode", nightMode)
                        )
                ),
                Category.of("Screen")
                        .subCategories(
                                Category.of("Scaling & Ordering",
                                        Group.of(
                                                Setting.of("Scaling", scaling)
                                                        .validate(DoubleRangeValidator
                                                                .atLeast(1, "Scaling needs to be at least 1")
                                                        ),
                                                Setting.of("Screen name", screenName),
                                                Setting.of("Resolution", resolutionItems, resolutionSelection),
                                                Setting.of("Orientation", orientationItems, orientationSelection)
                                        ).description("Screen Options"),
                                        Group.of(
                                                Setting.of("Font Size", fontSize, 6, 36),
                                                Setting.of("Line Spacing", lineSpacing, 0, 3, 1)
                                        )
                                )
                        ),
                Category.of("Favorites",
                        Setting.of("Favorites", favoritesItems, favoritesSelection),
                        Setting.of("Favorite Number", customControl, customControlProperty)
                ),
                Category.of("Workbench",
                        Group.of("布局与外观",
                                Setting.of("每页模块数量", modulesPerPage, 6, 24)
                        ).description("重启或重新打开模块列表后生效"),
                        Group.of("抽屉与工具栏",
                                Setting.of("抽屉以覆盖层显示", drawerOverlay),
                                Setting.of("工具栏靠左", toolbarLeft)
                        ).description("部分选项需重启应用生效")
                )
        ).persistWindowState(false).saveSettings(true).debugHistoryMode(false).buttonsVisibility(true);
    }

    public void save() {
        preferencesFx.saveSettings();
    }

    public void discardChanges() {
        preferencesFx.discardChanges();
    }

    public PreferencesFxView getPreferencesFxView() {
        return preferencesFx.getView();
    }

    public BooleanProperty nightModeProperty() {
        return nightMode;
    }

    public boolean isNightMode() {
        return nightMode.get();
    }

    public int getModulesPerPage() {
        return modulesPerPage.get();
    }

    public IntegerProperty modulesPerPageProperty() {
        return modulesPerPage;
    }

    public boolean isDrawerOverlay() {
        return drawerOverlay.get();
    }

    public BooleanProperty drawerOverlayProperty() {
        return drawerOverlay;
    }

    public boolean isToolbarLeft() {
        return toolbarLeft.get();
    }

    public BooleanProperty toolbarLeftProperty() {
        return toolbarLeft;
    }
}
