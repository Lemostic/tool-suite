package com.lemostic.work;

import com.dlsc.workbenchfx.Workbench;
import com.lemostic.work.modules.helloworld.HelloWorldModule;
import javafx.fxml.FXML;

public class FXMLController {

  @FXML
  private Workbench workbench;

  @FXML
  private void initialize() {
    workbench.getModules().add(new HelloWorldModule());
  }

}
