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

    // 数据库相关
    requires java.naming;
    requires java.desktop;
    requires com.zaxxer.hikari;
    requires jsch;

    // Spring框架 6.x
    requires spring.beans;
    requires spring.context;
    requires spring.core;
    requires spring.aop;
    requires spring.expression;
    requires com.baomidou.mybatis.plus.annotation;
    requires com.baomidou.mybatis.plus.core;
    requires org.mybatis;
    requires com.h2database;
    requires org.mybatis.spring;

    exports com.lemostic.work;

    // 开放包给Spring进行反射访问和依赖注入
    opens com.lemostic.work.config to spring.core, spring.beans, spring.context, spring.aop, spring.expression;
    opens com.lemostic.work.database.config to spring.core, spring.beans, spring.context, spring.aop, spring.expression;
    opens com.lemostic.work.database.service.impl to spring.core, spring.beans, spring.context, spring.aop, spring.expression;
    opens com.lemostic.work.modules.deployment.service.impl to spring.core, spring.beans, spring.context, spring.aop, spring.expression;

    // 开放包给JavaFX进行反射访问
    opens com.lemostic.work to javafx.fxml;
    opens com.lemostic.work.modules.deployment to javafx.fxml;
    opens com.lemostic.work.modules.deployment.view to javafx.fxml;
    opens com.lemostic.work.modules.preferences to javafx.fxml;
    opens com.lemostic.work.modules.datamigrator to javafx.fxml;

    // 开放实体包给MyBatis和其他框架进行反射访问
    opens com.lemostic.work.database.entity to spring.core, spring.beans, spring.context, spring.aop, spring.expression;
    opens com.lemostic.work.modules.deployment.model to spring.core, spring.beans, spring.context, spring.aop, spring.expression;

}