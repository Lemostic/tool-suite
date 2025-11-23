package io.github.lemostic.toolsuite.modules;

/**
 * ========================================
 * 模块开发模板和规范
 * ========================================
 * 
 * 目录结构规范：
 * modules/
 *   ├── system/          - 系统管理类模块
 *   │   └── preferences/ - 偏好设置模块
 *   │
 *   ├── database/        - 数据库工具类模块
 *   │   ├── mysql/       - MySQL客户端
 *   │   ├── redis/       - Redis客户端
 *   │   ├── mongodb/     - MongoDB客户端
 *   │   └── postgres/    - PostgreSQL客户端
 *   │
 *   ├── data/            - 数据处理类模块
 *   │   ├── migrate/     - 数据迁移工具
 *   │   ├── transform/   - 数据转换工具
 *   │   └── export/      - 数据导出工具
 *   │
 *   ├── devtools/        - 开发工具类模块
 *   │   ├── json/        - JSON格式化/验证
 *   │   ├── encoder/     - 编码/解码工具
 *   │   ├── generator/   - 代码生成器
 *   │   └── crypto/      - 加解密工具
 *   │
 *   ├── network/         - 网络工具类模块
 *   │   ├── http/        - HTTP客户端
 *   │   ├── websocket/   - WebSocket客户端
 *   │   └── api/         - API测试工具
 *   │
 *   ├── file/            - 文件工具类模块
 *   │   ├── compare/     - 文件对比
 *   │   ├── batch/       - 批量处理
 *   │   └── convert/     - 格式转换
 *   │
 *   ├── devops/          - 运维工具类模块
 *   │   ├── ssh/         - SSH客户端
 *   │   ├── deploy/      - 部署工具
 *   │   └── monitor/     - 监控工具
 *   │
 *   ├── search/          - 搜索引擎工具类模块
 *   │   ├── elasticsearch/ - Elasticsearch客户端
 *   │   └── solr/        - Solr客户端
 *   │
 *   └── mq/              - 消息队列工具类模块
 *       ├── kafka/       - Kafka客户端
 *       └── rabbitmq/    - RabbitMQ客户端
 *
 * ========================================
 * 模块开发步骤：
 * ========================================
 * 
 * 1. 创建模块目录
 *    例如：modules/database/mysql/
 * 
 * 2. 创建模块主类（继承 WorkbenchModule）
 *    例如：MySQLClientModule.java
 * 
 * 3. 添加 @ToolModule 注解
 *    示例代码如下：
 * 
 * ========================================
 * 模块代码模板：
 * ========================================
 */

/*
package io.github.lemostic.toolsuite.modules.database.mysql;

import com.dlsc.workbenchfx.model.WorkbenchModule;
import io.github.lemostic.toolsuite.core.module.ModuleCategory;
import io.github.lemostic.toolsuite.core.module.ToolModule;
import javafx.scene.Node;
import org.kordamp.ikonli.materialdesign.MaterialDesign;

@ToolModule(
    name = "MySQL客户端",
    category = ModuleCategory.DATABASE,
    description = "MySQL数据库连接、查询和管理工具",
    version = "1.0.0",
    author = "lemostic",
    requiresPreferences = false,  // 如果需要访问全局配置则设为 true
    enabled = true,
    priority = 50  // 数字越小优先级越高
)
public class MySQLClientModule extends WorkbenchModule {
    
    public MySQLClientModule() {
        super("MySQL客户端", MaterialDesign.MDI_DATABASE);
    }
    
    @Override
    public Node activate() {
        // 返回模块的主界面
        return new MySQLClientView();
    }
    
    @Override
    public boolean destroy() {
        // 模块关闭时的清理工作
        // 例如：关闭数据库连接、释放资源等
        return super.destroy();
    }
}
*/

/**
 * ========================================
 * 4. 在 ModuleLoader 中注册模块
 * ========================================
 * 
 * 打开：io.github.lemostic.toolsuite.core.ModuleLoader
 * 在 registerModules() 方法中添加：
 * 
 * ModuleRegistry.register(io.github.lemostic.toolsuite.modules.database.mysql.MySQLClientModule.class);
 * 
 * ========================================
 * 5. 启动应用，模块自动加载
 * ========================================
 * 
 * 新模块会按照优先级自动加载，无需修改其他代码！
 * 
 * ========================================
 * 高级功能：
 * ========================================
 * 
 * 1. 如果模块需要访问全局配置（如主题、语言等）：
 *    - 设置 requiresPreferences = true
 *    - 添加带 Preferences 参数的构造函数
 * 
 * 2. 如果模块需要工具栏按钮：
 *    - 使用 getToolbarControlsLeft() 或 getToolbarControlsRight()
 * 
 * 3. 如果模块需要菜单项：
 *    - 覆盖 getMenuItems() 方法
 * 
 * 4. 如果模块需要状态保存：
 *    - 覆盖 destroy() 方法保存状态
 *    - 在 activate() 中恢复状态
 */
class ModuleTemplate {}
