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
    requires com.querydsl.core;
    requires com.querydsl.jpa;
    
    // ES查询模块相关
    requires java.net.http;
    requires com.fasterxml.jackson.databind;
    requires org.apache.poi.poi;
    requires org.apache.poi.ooxml;

    exports io.github.lemostic.toolsuite;
    exports io.github.lemostic.toolsuite.core;
    exports io.github.lemostic.toolsuite.core.module;
    exports io.github.lemostic.toolsuite.core.spi;  // 导出 SPI 接口供外部插件使用
    
    // 声明 SPI 服务接口
    uses io.github.lemostic.toolsuite.core.spi.ToolModuleProvider;
    
    // 提供 SPI 实现（内置插件）
    provides io.github.lemostic.toolsuite.core.spi.ToolModuleProvider 
        with io.github.lemostic.toolsuite.modules.devtools.BuiltinDevToolsProvider;

    // 开放包给JavaFX进行反射访问
    opens io.github.lemostic.toolsuite to javafx.fxml;
    opens io.github.lemostic.toolsuite.modules.preferences to javafx.fxml;

    opens io.github.lemostic.toolsuite.modules.helloworld to javafx.fxml;
    opens io.github.lemostic.toolsuite.modules.file.zipclean to javafx.fxml, javafx.base;
    opens io.github.lemostic.toolsuite.modules.search.es to javafx.fxml, javafx.base;
    
    // 开放部署模块的包给Hibernate、QueryDSL和JavaFX
    opens io.github.lemostic.toolsuite.modules.devops.deploy.entity to org.hibernate.orm.core, com.querydsl.jpa;
    opens io.github.lemostic.toolsuite.modules.devops.deploy.repository to org.hibernate.orm.core;
    opens io.github.lemostic.toolsuite.modules.devops.deploy.view to javafx.fxml, javafx.base;
    opens io.github.lemostic.toolsuite.modules.devops.deploy.view.components to javafx.fxml, javafx.base;
    opens io.github.lemostic.toolsuite.modules.devops.deploy.view.dialogs to javafx.fxml, javafx.base;

}