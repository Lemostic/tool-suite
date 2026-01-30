# Tool Suite

基于 [WorkbenchFX](https://github.com/dlsc-software-consulting-gmbh/WorkbenchFX) 的桌面工具箱，支持模块化扩展。构建后通过 **tool-suite.bat** 启动（Windows），或使用 `java` 命令运行。

---

## 一、环境准备

### 1.1 安装 JDK

- **版本**：JDK 17 或 21（推荐 21）。
- **下载**：
  - [Adoptium (Eclipse Temurin)](https://adoptium.net/)
  - [Oracle JDK](https://www.oracle.com/java/technologies/downloads/)
- **校验**：打开终端（PowerShell 或 CMD），执行：
  ```bash
  java -version
  ```
  能显示版本号即表示安装成功。

### 1.2 安装 Maven

- **版本**：Maven 3.6 及以上。
- **下载**：[Maven 官网](https://maven.apache.org/download.cgi) 下载 Binary zip，解压到任意目录（如 `C:\Program Files\Apache\maven`）。
- **配置 PATH**：将 Maven 的 `bin` 目录加入系统环境变量 PATH。  
  例如 Maven 在 `C:\Program Files\Apache\maven`，则把 `C:\Program Files\Apache\maven\bin` 加入 PATH。
- **校验**：新开一个终端，执行：
  ```bash
  mvn -v
  ```
  能显示 Maven 版本即表示配置正确。

---

## 二、构建步骤

### 2.1 进入项目目录

在资源管理器中打开项目根目录（能看到 `pom.xml` 的目录），在该目录下打开终端：

- **方式一**：在地址栏输入 `cmd` 或 `powershell` 回车。
- **方式二**：在 VS Code / Cursor 中打开项目后，使用集成终端，并确认当前目录为项目根目录。

### 2.2 执行构建命令

在项目根目录下执行：

```bash
mvn clean package -DskipTests
```

- **首次构建**：会下载大量依赖，耗时会比较长（几分钟到十几分钟），属正常现象。
- **后续构建**：依赖已缓存，速度会明显加快。
- 看到 `BUILD SUCCESS` 即表示构建成功。

### 2.3 构建产物位置

构建成功后，可用的启动方式在以下目录：

| 路径 | 说明 |
|------|------|
| `target\dist\bin\tool-suite.bat` | **推荐**：Windows 下双击或在该目录执行此脚本启动程序。 |
| `target\dist\lib\` | 应用 JAR（`tool-suite-1.0-SNAPSHOT-shaded.jar`）及 JavaFX 依赖（`javafx\` 子目录），由 `tool-suite.bat` 自动使用。 |

**目录结构示意：**

```
target/dist/
├── bin/
│   └── tool-suite.bat    ← 启动脚本
└── lib/
    ├── tool-suite-1.0-SNAPSHOT-shaded.jar
    └── javafx/           ← JavaFX 运行时 jar
```

---

## 三、运行程序（tool-suite 方式）

### 3.1 使用 tool-suite.bat 启动

- **方式一**：在资源管理器中进入 `target\dist\bin\`，双击 `tool-suite.bat`。
- **方式二**：在终端中进入 `target\dist\bin\` 后执行：
  ```bash
  .\tool-suite.bat
  ```

脚本会使用 `lib` 目录下的 JAR 和 JavaFX，无需本机单独安装 JavaFX。启动后 bat 窗口会自动关闭，仅保留程序窗口。

### 3.2 分发或备份

若需要把程序拷到其他电脑使用，请**整体复制** `target\dist` 目录（保持 `bin` 与 `lib` 的相对关系），在目标机器上同样通过 `dist\bin\tool-suite.bat` 启动。目标机器需已安装 **JDK 17+**（或 JRE）。

---

## 四、其他常用命令

| 用途 | 命令 |
|------|------|
| 开发时直接运行（不打包） | `mvn compile exec:java` |
| 只打 Fat JAR，不生成 dist | `mvn clean package -DskipTests "-Djpackage.skip=true"`（产物：`target\tool-suite-1.0-SNAPSHOT-shaded.jar`） |

---

## 五、常见问题

- **`mvn` 或 `java` 找不到**  
  说明 JDK 或 Maven 的 `bin` 未加入系统 PATH，请按「一、环境准备」重新配置并新开终端再试。

- **打包很慢**  
  首次会下载依赖，属正常；后续构建会快很多。

- **运行 bat 提示“缺少 JavaFX 运行时”**  
  请确认使用的是**本次构建生成的** `target\dist` 目录（内含 `lib\javafx`），且未删除 `lib\javafx` 下的 jar。若仍报错，可重新执行一次 `mvn clean package -DskipTests`。

- **中文乱码**  
  `tool-suite.bat` 已设置 `chcp 65001`（UTF-8），若终端仍乱码，可尝试在「终端属性」中将编码改为 UTF-8。
