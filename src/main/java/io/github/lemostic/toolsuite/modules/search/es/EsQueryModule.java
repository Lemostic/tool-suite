package io.github.lemostic.toolsuite.modules.search.es;

import io.github.lemostic.toolsuite.core.module.BaseToolModule;
import io.github.lemostic.toolsuite.core.module.ModuleCategory;
import io.github.lemostic.toolsuite.core.module.ToolModule;
import javafx.scene.Node;
import org.kordamp.ikonli.materialdesign.MaterialDesign;

/**
 * ES数据查询模块
 * 用于连接Elasticsearch，执行查询并导出数据为Excel
 */
@ToolModule(
    name = "ES数据查询",
    category = ModuleCategory.SEARCH_ENGINE,
    menuGroup = "搜索引擎",
    menuGroupOrder = 50,
    description = "连接Elasticsearch执行查询，支持字段搜索、列筛选和Excel导出",
    version = "1.0.0",
    author = "lemostic",
    requiresPreferences = false,
    priority = 50
)
public class EsQueryModule extends BaseToolModule {
    
    public EsQueryModule() {
        super("ES数据查询", MaterialDesign.MDI_MAGNIFY_PLUS);
    }
    
    @Override
    protected Node createView() {
        return new EsQueryView();
    }
    
    @Override
    protected void onDestroy() {
        logger.debug("ES数据查询模块已销毁");
    }
}
