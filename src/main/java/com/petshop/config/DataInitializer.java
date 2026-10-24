package com.petshop.config;

import com.petshop.module.system.BackupService;
import com.petshop.module.system.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Paths;

/** 首次启动：建目录、建 admin、v1.1 增列迁移、可选示例数据。 */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserService userService;
    private final AppProps props;
    private final BackupService backupService;
    private final JdbcTemplate jdbc;

    public DataInitializer(UserService userService, AppProps props,
                           BackupService backupService, JdbcTemplate jdbc) {
        this.userService = userService;
        this.props = props;
        this.backupService = backupService;
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Files.createDirectories(Paths.get(props.getDataDir()));
        Files.createDirectories(Paths.get(props.getBackupDir()));
        Files.createDirectories(Paths.get("logs"));

        userService.initAdmin("admin", props.getDefaultAdminPassword());
        migrateV11();
        migrateV12();
        if (props.isSampleData()) {
            seedSampleData();
        }

        log.info("============================================================");
        log.info("  宠物店管理系统启动完成");
        log.info("  访问地址:  http://localhost:8080");
        log.info("  数据文件:  {}", backupService.dbPath());
        log.info("  默认账号:  admin（初始密码见 app.default-admin-password / 环境变量 APP_DEFAULT_ADMIN_PASSWORD，请登录后立即修改）");
        log.info("============================================================");
    }

    /** v1.0 库升级：product_ledger 补 unit_cost_cent 列（新库由 schema.sql 直接建出）。 */
    private void migrateV11() {
        boolean exists = false;
        for (java.util.Map<String, Object> col : jdbc.queryForList("PRAGMA table_info(product_ledger)")) {
            if ("unit_cost_cent".equals(col.get("name"))) {
                exists = true;
                break;
            }
        }
        if (!exists) {
            jdbc.execute("ALTER TABLE product_ledger ADD COLUMN unit_cost_cent INTEGER NOT NULL DEFAULT 0");
            log.info("v1.1 迁移：product_ledger 已增列 unit_cost_cent");
        }
        jdbc.execute("INSERT OR IGNORE INTO store_config(k, v) VALUES ('slot_capacity', '3')");
    }

    /**
     * v1.2 迁移：存量表增列（幂等）+ 迁移版本登记。
     * 新表由 schema.sql 的 IF NOT EXISTS 建出，这里只处理 ALTER。
     */
    private void migrateV12() {
        jdbc.execute("CREATE TABLE IF NOT EXISTS schema_version(version TEXT PRIMARY KEY, applied_at TEXT NOT NULL)");

        addColumnIfMissing("product", "spec", "TEXT");
        addColumnIfMissing("product", "category", "TEXT DEFAULT 'OTHER'");
        addColumnIfMissing("product", "shelf_life_days", "INTEGER");
        addColumnIfMissing("product", "default_supplier_id", "INTEGER");
        addColumnIfMissing("bill_item", "staff_id", "INTEGER");
        addColumnIfMissing("bill_item", "batch_id", "INTEGER");
        addColumnIfMissing("product_ledger", "batch_id", "INTEGER");
        addColumnIfMissing("sys_user", "staff_id", "INTEGER");
        addColumnIfMissing("customer", "birthday", "TEXT");

        // 索引在列存在后创建，避免老库 ALTER 前建索引失败
        jdbc.execute("CREATE INDEX IF NOT EXISTS idx_ledger_batch ON product_ledger(batch_id)");
        jdbc.execute("CREATE INDEX IF NOT EXISTS idx_item_staff ON bill_item(staff_id)");

        Integer done = jdbc.queryForObject(
                "SELECT COUNT(*) FROM schema_version WHERE version = 'v1.2'", Integer.class);
        if (done == null || done == 0) {
            jdbc.update("INSERT INTO schema_version(version, applied_at) VALUES('v1.2', ?)",
                    java.time.LocalDateTime.now().format(
                            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            log.info("v1.2 迁移完成：批次/盘点/提成/客户经营/审计表就绪");
        }
    }

    private void addColumnIfMissing(String table, String column, String definition) {
        boolean exists = false;
        for (java.util.Map<String, Object> col : jdbc.queryForList("PRAGMA table_info(" + table + ")")) {
            if (column.equals(col.get("name"))) {
                exists = true;
                break;
            }
        }
        if (!exists) {
            jdbc.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
            log.info("v1.2 迁移：{}.{} 已增列", table, column);
        }
    }

    /** 示例数据（app.sample-data=true 且库为空时灌入），演示/验收用。 */
    private void seedSampleData() {
        Integer customers = jdbc.queryForObject("SELECT COUNT(*) FROM customer", Integer.class);
        Integer services = jdbc.queryForObject("SELECT COUNT(*) FROM service_item", Integer.class);
        if (customers != null && customers > 0 && services != null && services > 0) {
            return; // 已有数据不重复灌
        }
        log.info("sample-data=true：灌入示例数据…");
        String now = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        // 客户 1 名 + 宠物 1 只
        jdbc.update("INSERT INTO customer(name, phone, source, remark, is_active, created_at) " +
                "VALUES('演示客户', '13800009999', '示例数据', 'app.sample-data=true 自动创建', 1, ?)", now);
        jdbc.update("INSERT INTO pet(owner_id, name, species, breed, gender, birthday, weight_g, " +
                "is_neutered, color, habit, is_active, created_at) " +
                "VALUES(1, '豆豆', 'DOG', '金毛', 'MALE', '2023-03-15', 12500, 1, '白色', '怕吹风机', 1, ?)", now);
        // 服务 2 项
        jdbc.update("INSERT INTO service_item(name, duration_min, price_s_cent, price_m_cent, " +
                "price_l_cent, price_xl_cent, is_active, sort_no) " +
                "VALUES('精洗', 60, 5000, 8000, 12000, 18000, 1, 1)");
        jdbc.update("INSERT INTO service_item(name, duration_min, price_s_cent, price_m_cent, " +
                "price_l_cent, price_xl_cent, is_active, sort_no) " +
                "VALUES('剪毛造型', 90, 8000, 12000, 20000, 30000, 1, 2)");
        // 笼位 4 个
        for (String[] r : new String[][]{{"R01", "M"}, {"R02", "L"}, {"R03", "XL"}, {"C01", "CAT"}}) {
            jdbc.update("INSERT OR IGNORE INTO boarding_room(code, room_type, daily_price_cent, " +
                    "is_active, created_at) VALUES(?, ?, ?, 1, ?)",
                    r[0], r[1], "M".equals(r[1]) ? 6000 : "L".equals(r[1]) ? 8000
                            : "XL".equals(r[1]) ? 10000 : 5000, now);
        }
        log.info("示例数据灌入完成");
    }
}
