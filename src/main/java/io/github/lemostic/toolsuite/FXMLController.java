package io.github.lemostic.toolsuite.toolsuite;

import com.dlsc.workbenchfx.Workbench;
import io.github.lemostic.toolsuite.modules.helloworld.HelloWorldModule;
import javafx.fxml.FXML;

public class FXMLController {

  @FXML
  private Workbench workbench;

  @FXML
  private void initialize() {
    workbench.getModules().add(new HelloWorldModule());
  }

}
