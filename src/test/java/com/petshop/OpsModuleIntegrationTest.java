package com.petshop;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 临时集成测试：全部通过 HTTP 接口驱动（不直接调服务层）。
 * 覆盖：登录 → 接口造数（供应商=审计数据）→ 审计查询/过滤/翻页/非法参数
 *      → 备份接口造备份文件 → 恢复准备（已有备份/上传/路径穿越/坏文件）
 *      → 维护模式拦截 → 取消恢复 → 数据体检正常/异常/恢复。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite:./target/smoke-test.db",
        "app.data-dir=./target/smoke-data",
        "app.backup-dir=./target/smoke-data/backup"
})
@AutoConfigureMockMvc
class OpsModuleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeAll
    static void redirectMarkerAwayFromRealData() {
        // RestoreBootstrap 读的是 JVM 系统属性，必须与 Spring 配置对齐，
        // 否则测试会把 restore.pending 写进真实 ./data，下次启动会误替换真实库
        System.setProperty("app.data-dir", "./target/smoke-data");
    }

    @AfterAll
    static void cleanup() {
        try {
            Files.deleteIfExists(Paths.get("target/smoke-data/restore.pending"));
        } catch (Exception ignore) {
        }
        System.clearProperty("app.data-dir");
    }

    private Path marker() {
        return Paths.get("target/smoke-data/restore.pending");
    }

    private Path backupDir() {
        return Paths.get("target/smoke-data/backup");
    }

    @Test
    void fullFlow() throws Exception {
        // ---------- 1. 登录 ----------
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(post("/login").param("username", "admin").param("password", "123456")
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        // ---------- 2. 通过接口造审计数据：新增 3 个供应商 ----------
        for (String name : new String[]{"测试供应商甲", "测试供应商乙", "测试供应商丙"}) {
            jdbc.update("DELETE FROM supplier WHERE name = ?", name);
            mockMvc.perform(post("/suppliers/save").session(session).param("name", name))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/suppliers"));
        }
        Long auditCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM audit_log WHERE action='SUPPLIER_CREATE'", Long.class);
        assertTrue(auditCount >= 3, "应至少有 3 条新增供应商审计");

        // ---------- 3. 审计页：渲染 / 操作类型过滤 / 操作人过滤 / 翻页 ----------
        mockMvc.perform(get("/system/audit").session(session))
                .andExpect(status().isOk());
        String body = mockMvc.perform(get("/system/audit").session(session)
                        .param("action", "SUPPLIER_CREATE").param("operator", "店主"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertTrue(body.contains("新增供应商"), "操作类型应映射为中文");
        assertTrue(body.contains("测试供应商甲"), "after_json 应展示供应商名");
        assertTrue(body.contains("共 " + auditCount + " 条"), "过滤后总数应匹配");

        // 空结果 + 越界页码不报错
        mockMvc.perform(get("/system/audit").session(session)
                        .param("action", "SUPPLIER_CREATE").param("page", "999"))
                .andExpect(status().isOk());
        // 非法日期 → 业务异常重定向，而不是 500
        mockMvc.perform(get("/system/audit").session(session).param("from", "2026/01/01"))
                .andExpect(status().is3xxRedirection());

        // ---------- 4. 备份接口造数据 ----------
        mockMvc.perform(post("/system/backup").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/system/backup"));
        List<String> backups = Files.list(backupDir())
                .map(p -> p.getFileName().toString())
                .filter(n -> n.endsWith(".db")).collect(Collectors.toList());
        assertFalse(backups.isEmpty(), "备份目录应有备份文件");
        String latest = backups.stream().sorted().collect(Collectors.toList())
                .get(backups.size() - 1);

        // ---------- 5. 恢复准备：非法输入都要被拦下 ----------
        // 5.1 路径穿越
        mockMvc.perform(post("/system/restore/prepare").session(session)
                        .param("fileName", "../../pet.db"))
                .andExpect(status().is3xxRedirection());
        assertFalse(Files.exists(marker()), "路径穿越不应产生恢复标记");
        // 5.2 不存在的备份文件
        mockMvc.perform(post("/system/restore/prepare").session(session)
                        .param("fileName", "pet-19990101-000000.db"))
                .andExpect(status().is3xxRedirection());
        // 5.3 未选择任何文件
        mockMvc.perform(post("/system/restore/prepare").session(session))
                .andExpect(status().is3xxRedirection());
        // 5.4 伪 .db（内容是文本）
        mockMvc.perform(multipart("/system/restore/prepare")
                        .file(new MockMultipartFile("file", "fake.db", "application/octet-stream",
                                "this is not sqlite".getBytes(StandardCharsets.UTF_8)))
                        .session(session))
                .andExpect(status().is3xxRedirection());
        assertFalse(Files.exists(marker()), "坏文件不应产生恢复标记");

        // ---------- 6. 恢复准备：选择已有备份（合法） ----------
        mockMvc.perform(post("/system/restore/prepare").session(session).param("fileName", latest))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/maintenance"));
        assertTrue(Files.exists(marker()), "合法备份应产生恢复标记");
        // 维护模式下 /system/restore 也被拦截，待恢复提示在 /maintenance 上；审计里留痕
        String maintPage = mockMvc.perform(get("/maintenance"))
                .andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        assertTrue(maintPage.contains("备份恢复已准备就绪"));
        Integer preBackupAudit = jdbc.queryForObject(
                "SELECT IFNULL(MAX(CASE WHEN after_json LIKE 'prepare %' THEN 1 ELSE 0 END),0) "
                        + "FROM audit_log WHERE action='BACKUP_RESTORE'", Integer.class);
        assertEquals(Integer.valueOf(1), preBackupAudit, "恢复准备应写审计");

        // ---------- 7. 维护模式：其它页面被重定向到 /maintenance ----------
        mockMvc.perform(get("/").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/maintenance"));
        mockMvc.perform(get("/maintenance"))
                .andExpect(status().isOk());
        // 取消恢复，退出维护模式
        mockMvc.perform(post("/system/restore/cancel").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/system/restore"));
        assertFalse(Files.exists(marker()), "取消后标记应删除");
        mockMvc.perform(get("/system/health").session(session)).andExpect(status().isOk());

        // ---------- 8. 恢复准备：上传合法整库 ----------
        Path validDb = Paths.get("target/smoke-data/upload-valid.db").toAbsolutePath();
        Files.deleteIfExists(validDb);
        jdbc.execute("VACUUM INTO '" + validDb.toString().replace("\\", "/").replace("'", "''") + "'");
        MvcResult upload = mockMvc.perform(multipart("/system/restore/prepare")
                        .file(new MockMultipartFile("file", "upload-valid.db", "application/octet-stream",
                                Files.readAllBytes(validDb)))
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/maintenance"))
                .andReturn();
        assertTrue(Files.exists(marker()));
        mockMvc.perform(post("/system/restore/cancel").session(session))
                .andExpect(status().is3xxRedirection());
        assertFalse(Files.exists(marker()));

        // ---------- 9. 数据体检：正常 → 注入不一致 → 异常 → 清理恢复 ----------
        String healthy = mockMvc.perform(get("/system/health").session(session))
                .andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        assertTrue(healthy.contains("未发现数据一致性问题"), "干净库应全部通过");

        jdbc.update("INSERT INTO product(name, stock_qty, is_active, created_at) "
                + "VALUES('T-体检不一致', 5, 1, datetime('now','localtime'))");
        String dirty = mockMvc.perform(get("/system/health").session(session))
                .andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        assertTrue(dirty.contains("项存在异常"), "注入不一致后应报异常");
        assertTrue(dirty.contains("库存结存 vs 流水求和"), "应命中结存 vs 流水检查");

        jdbc.update("DELETE FROM product WHERE name = 'T-体检不一致'");
        String healed = mockMvc.perform(get("/system/health").session(session))
                .andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        assertTrue(healed.contains("未发现数据一致性问题"), "清理后应恢复正常");
    }
}
