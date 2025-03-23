package com.lemostic.work.modules.datamigrator;

import cn.hutool.core.util.ObjectUtil;
import com.dlsc.workbenchfx.model.WorkbenchModule;
import com.lemostic.work.modules.datamigrator.service.MigrationService;
import com.lemostic.work.modules.datamigrator.service.impl.MigrationServiceImpl;
import com.lemostic.work.modules.datamigrator.view.DataMigrateView;
import javafx.scene.Node;
import org.kordamp.ikonli.materialdesign.MaterialDesign;

public class DataMigrateModule extends WorkbenchModule {

    /**
     * 数据迁移视图
     */
    private DataMigrateView dataMigrateView;
    private MigrationService migrationService;

    public DataMigrateModule() {
        super("数据迁移", MaterialDesign.MDI_BOMB);
    }

    @Override
    public Node activate() {
        if (ObjectUtil.isNull(migrationService)) {
            migrationService = new MigrationServiceImpl();
        }
        if (ObjectUtil.isNull(dataMigrateView)) {
            dataMigrateView = new DataMigrateView(migrationService);
        }
        return dataMigrateView;
    }
}
