# 🔌 外部插件开发指南

## 📦 创建独立的插件项目

### 第一步：创建 Maven 项目

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.yourcompany</groupId>
    <artifactId>toolsuite-mysql-plugin</artifactId>
    <version>1.0.0</version>

    <properties>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
    </properties>

    <dependencies>
        <!-- 依赖主工程（提供的 API） -->
        <dependency>
            <groupId>io.github.lemostic.toolsuite</groupId>
            <artifactId>tool-suite</artifactId>
            <version>1.0-SNAPSHOT</version>
            <scope>provided</scope>
        </dependency>

        <!-- WorkbenchFX -->
        <dependency>
            <groupId>com.dlsc.workbenchfx</groupId>
            <artifactId>workbenchfx-core</artifactId>
            <version>11.3.1</version>
            <scope>provided</scope>
        </dependency>

        <!-- JavaFX -->
        <dependency>
            <groupId>org.openjfx</groupId>
            <artifactId>javafx-controls</artifactId>
            <version>17.0.1</version>
            <scope>provided</scope>
        </dependency>

        <!-- MySQL 驱动 -->
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <version>9.2.0</version>
        </dependency>
    </dependencies>
</project>
```

### 第二步：创建模块类

```java
package com.yourcompany.toolsuite.plugins.mysql;

import io.github.lemostic.toolsuite.core.module.BaseToolModule;
import io.github.lemostic.toolsuite.core.module.ModuleCategory;
import io.github.lemostic.toolsuite.core.module.ToolModule;
import javafx.scene.Node;
import org.kordamp.ikonli.materialdesign.MaterialDesign;

@ToolModule(
    name = "MySQL客户端",
    category = ModuleCategory.DATABASE,
    description = "MySQL数据库连接和查询工具",
    version = "1.0.0",
    author = "YourName"
)
public class MySQLClientModule extends BaseToolModule {
    
    public MySQLClientModule() {
        super("MySQL客户端", MaterialDesign.MDI_DATABASE);
    }
    
    @Override
    protected Node createView() {
        // 你的 UI 界面
        return new MySQLClientView();
    }
    
    @Override
    protected void onDestroy() {
        // 清理资源（关闭数据库连接等）
    }
}
```

### 第三步：创建 SPI 提供者

```java
package com.yourcompany.toolsuite.plugins.mysql;

import com.dlsc.workbenchfx.model.WorkbenchModule;
import io.github.lemostic.toolsuite.core.spi.ToolModuleProvider;

import java.util.Arrays;
import java.util.List;

public class MySQLPluginProvider implements ToolModuleProvider {
    
    @Override
    public List<Class<? extends WorkbenchModule>> getModuleClasses() {
        return Arrays.asList(
            MySQLClientModule.class
        );
    }
    
    @Override
    public String getProviderName() {
        return "MySQL 数据库工具插件";
    }
    
    @Override
    public String getVersion() {
        return "1.0.0";
    }
    
    @Override
    public String getDescription() {
        return "提供 MySQL 数据库连接、查询和管理功能";
    }
    
    @Override
    public void initialize() {
        System.out.println("MySQL 插件初始化...");
        // 可以在这里进行初始化操作
    }
    
    @Override
    public void destroy() {
        System.out.println("MySQL 插件清理...");
        // 可以在这里进行清理操作
    }
}
```

### 第四步：创建 SPI 配置文件

在 `src/main/resources/META-INF/services/` 目录下创建文件：

**文件名：** `io.github.lemostic.toolsuite.core.spi.ToolModuleProvider`

**文件内容：**
```
com.yourcompany.toolsuite.plugins.mysql.MySQLPluginProvider
```

### 第五步：创建 module-info.java

```java
module toolsuite.mysql.plugin {
    requires io.github.lemostic.toolsuite;
    requires com.dlsc.workbenchfx.core;
    requires javafx.controls;
    requires org.kordamp.ikonli.materialdesign;
    requires mysql.connector.j;
    
    // 提供 SPI 实现
    provides io.github.lemostic.toolsuite.core.spi.ToolModuleProvider 
        with com.yourcompany.toolsuite.plugins.mysql.MySQLPluginProvider;
}
```

### 第六步：打包并部署

```bash
# 打包插件
mvn clean package

# 将生成的 JAR 放入主工程的 lib 目录或 classpath
cp target/toolsuite-mysql-plugin-1.0.0.jar /path/to/tool-suite/plugins/
```

### 第七步：启动主程序

插件会自动被发现和加载！

---

## 🎯 完整示例项目结构

```
toolsuite-mysql-plugin/
├── pom.xml
├── src/
│   └── main/
│       ├── java/
│       │   ├── module-info.java
│       │   └── com/yourcompany/toolsuite/plugins/mysql/
│       │       ├── MySQLClientModule.java       # 模块主类
│       │       ├── MySQLClientView.java         # UI 界面
│       │       ├── MySQLPluginProvider.java     # SPI 提供者
│       │       └── service/
│       │           └── MySQLService.java        # 业务逻辑
│       └── resources/
│           └── META-INF/
│               └── services/
│                   └── io.github.lemostic.toolsuite.core.spi.ToolModuleProvider
└── target/
    └── toolsuite-mysql-plugin-1.0.0.jar        # 打包后的插件
```

---

## 📝 最佳实践

### 1. 版本管理
- 使用语义化版本号（如 1.0.0）
- 在 `@ToolModule` 注解中声明版本

### 2. 资源清理
- 在 `onDestroy()` 方法中关闭数据库连接
- 释放文件句柄和网络资源

### 3. 异常处理
- 使用 `logger` 记录错误
- 使用 `showError()` 向用户展示错误信息

### 4. 依赖管理
- 主工程 API 使用 `provided` 作用域
- 插件特有的依赖打包进 JAR

### 5. 模块分类
使用合适的 `ModuleCategory`：
- DATABASE - 数据库工具
- NETWORK - 网络工具
- FILE_TOOLS - 文件工具
- DEV_TOOLS - 开发工具
- DEVOPS - 运维工具

---

## 🚀 快速开发模板

```java
@ToolModule(
    name = "你的模块名",
    category = ModuleCategory.XXX,
    description = "模块描述",
    version = "1.0.0"
)
public class YourModule extends BaseToolModule {
    
    public YourModule() {
        super("你的模块名", MaterialDesign.MDI_ICON);
    }
    
    @Override
    protected Node createView() {
        // 快速占位
        return createPlaceholder("开发中...");
        
        // 或者返回你的UI
        // return new YourView();
    }
}
```

---

## 🔍 调试技巧

### 查看加载日志
```
21:35:09 [JavaFX] INFO SpiModuleLoader -- 发现 SPI 提供者: MySQL 数据库工具插件
21:35:09 [JavaFX] INFO SpiModuleLoader --   提供 1 个模块:
21:35:09 [JavaFX] INFO SpiModuleLoader --     - MySQLClientModule
21:35:10 [JavaFX] INFO ModuleRegistry -- 成功加载模块: MySQL客户端 (优先级: 50)
```

### 常见问题

**Q: 插件没有被加载？**
- 检查 `META-INF/services` 文件路径和内容
- 确认 `module-info.java` 中有 `provides` 声明
- 查看日志中是否有异常信息

**Q: 模块加载顺序不对？**
- 使用 `@ToolModule(priority = X)` 调整优先级
- 数字越小优先级越高

**Q: 无法访问主工程的类？**
- 确认主工程已经 `exports` 对应的包
- 在插件的 `module-info.java` 中添加 `requires`

---

打造你的专属工具插件，享受模块化开发的乐趣！🎉
