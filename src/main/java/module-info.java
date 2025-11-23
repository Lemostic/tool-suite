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
    requires com.dlsc.preferencesfx;
    requires com.google.common;
    requires javafx.web;
    requires java.sql;
    requires cn.hutool;
    requires org.apache.commons.lang3;
    requires static lombok;

    // 数据库相关
    requires java.naming;
    requires java.desktop;
    requires com.zaxxer.hikari;
    requires jsch;
    requires com.h2database;
    requires jakarta.persistence;
    requires jakarta.xml.bind;
    requires org.hibernate.orm.core;

    exports io.github.lemostic.toolsuite;
    exports io.github.lemostic.toolsuite.core;

    // 开放包给JavaFX进行反射访问
    opens io.github.lemostic.toolsuite to javafx.fxml;
    opens io.github.lemostic.toolsuite.modules.preferences to javafx.fxml;

    opens io.github.lemostic.toolsuite.modules.helloworld to javafx.fxml;

}