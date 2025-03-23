module ToolSuite {
    requires javafx.graphics;
    requires org.kordamp.ikonli.materialdesign;
    requires com.dlsc.workbenchfx.core;
    requires fr.brouillard.oss.cssfx;
    requires javafx.fxml;
    requires javafx.controls;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.fontawesome;
    requires org.kordamp.ikonli.core;
    requires org.slf4j;
    requires com.calendarfx.view;
    requires com.dlsc.gmapsfx;
    requires com.dlsc.preferencesfx;
    requires com.google.common;
    requires javafx.web;
    requires java.sql;
    requires cn.hutool; // 添加对 hutool-all 模块的依赖
    requires org.apache.commons.lang3;
    requires static lombok;
    exports com.lemostic.work;
}