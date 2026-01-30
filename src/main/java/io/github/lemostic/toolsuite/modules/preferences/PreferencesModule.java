package io.github.lemostic.toolsuite.modules.preferences;

import com.dlsc.workbenchfx.model.WorkbenchModule;
import com.dlsc.workbenchfx.view.controls.ToolbarItem;
import io.github.lemostic.toolsuite.core.module.ModuleCategory;
import io.github.lemostic.toolsuite.core.module.ToolModule;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import org.kordamp.ikonli.fontawesome.FontAwesome;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign.MaterialDesign;

@ToolModule(
    name = "Preferences",
    category = ModuleCategory.SYSTEM,
    menuGroup = "系统",
    menuGroupOrder = 1,
    description = "系统偏好设置与 Workbench 布局配置",
    version = "1.0.0",
    author = "lemostic",
    requiresPreferences = true,
    priority = 1
)
public class PreferencesModule extends WorkbenchModule {

  Preferences preferences;

  public PreferencesModule(Preferences preferences) {
    super("Preferences", FontAwesome.GEAR);
    this.preferences = preferences;

    ToolbarItem save = new ToolbarItem(new FontIcon(MaterialDesign.MDI_CONTENT_SAVE),
        event -> preferences.save());
    ToolbarItem discardChanges =
        new ToolbarItem(new FontIcon(MaterialDesign.MDI_DELETE),
            event -> getWorkbench().showConfirmationDialog("Discard Changes",
                "Are you sure you want to discard all changes since you last saved?",
                buttonType -> {
                  if (ButtonType.YES.equals(buttonType)) {
                    preferences.discardChanges();
                  }
                })
        );
    getToolbarControlsLeft().addAll(save, discardChanges);
  }

  @Override
  public Node activate() {
    return preferences.getPreferencesFxView();
  }

  @Override
  public boolean destroy() {
    preferences.save();
    return true;
  }
}
