package com.lemostic.work.enums;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 数据库枚举
 */
public enum DBEnum {
    ORACLE("Oracle", "oracle.jdbc.driver.OracleDriver"),
    MYSQL("MySQL", "com.mysql.cj.jdbc.Driver"),
    OSCAR("Oscar", "com.oscar.jdbc.driver.OscarDriver"),
    POSTGRESQL("PostgreSQL", "org.postgresql.Driver");
    ;

    private final String name;

    private final String className;

    private DBEnum(String name, String className) {
        this.name = name;
        this.className = className;
    }

    public String getName() {
        return name;
    }

    public String getClassName() {
        return className;
    }

    // of
    public static DBEnum of(String name) {
        for (DBEnum dbEnum : values()) {
            if (dbEnum.getName().equals(name)) {
                return dbEnum;
            }
        }
        return null;
    }

    // is
    public boolean is(String name) {
        return this.getName().equals(name);
    }

    // 获取name列表
    public static List<String> nameList() {
        return Arrays.stream(values()).map(DBEnum::getName).collect(Collectors.toList());
    }

    // 获取className列表
    public static List<String> classNameList() {
        return Arrays.stream(values()).map(DBEnum::getClassName).collect(Collectors.toList());
    }
}
