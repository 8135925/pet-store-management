package com.petshop.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 备份恢复的「启动前替换」。
 *
 * <p>SQLite 无法在连接池运行中安全替换底层文件，因此 Web 端只做校验并写
 * {@code data/restore.pending}，真正替换发生在下次启动、Spring 初始化之前。
 * 替换前会把当前库另存为 pre-restore-*.db，避免恢复错了无法回头。</p>
 */
public final class RestoreBootstrap {

    private static final Logger log = LoggerFactory.getLogger(RestoreBootstrap.class);
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private static volatile boolean maintenance = false;

    private RestoreBootstrap() {
    }

    public static Path marker() {
        return Paths.get(dataDir(), "restore.pending");
    }

    public static boolean isMaintenance() {
        return maintenance;
    }

    public static void enterMaintenance() {
        maintenance = true;
    }

    public static void clearMaintenance() {
        maintenance = false;
    }

    /** main() 里、Spring 启动之前调用 */
    public static void applyIfPending() {
        Path marker = marker();
        if (!Files.isRegularFile(marker)) {
            return;
        }
        Path dataDir = Paths.get(dataDir());
        Path db = dataDir.resolve("pet.db");
        Path backupDir = dataDir.resolve("backup");
        try {
            String target = new String(Files.readAllBytes(marker), StandardCharsets.UTF_8).trim();
            Path src = Paths.get(target);
            if (!Files.isRegularFile(src)) {
                log.error("待恢复文件不存在，已忽略：{}", target);
                Files.deleteIfExists(marker);
                return;
            }
            Files.createDirectories(backupDir);
            if (Files.isRegularFile(db)) {
                Path keep = backupDir.resolve("pre-restore-" + LocalDateTime.now().format(TS) + ".db");
                Files.move(db, keep, StandardCopyOption.REPLACE_EXISTING);
                log.warn("恢复前已备份当前库：{}", keep);
            }
            Files.deleteIfExists(dataDir.resolve("pet.db-wal"));
            Files.deleteIfExists(dataDir.resolve("pet.db-shm"));
            Files.copy(src, db, StandardCopyOption.REPLACE_EXISTING);
            Files.deleteIfExists(marker);
            log.warn("备份恢复完成，已使用 {}", target);
        } catch (IOException e) {
            log.error("恢复失败，保留原库与标记文件", e);
        }
    }

    private static String dataDir() {
        String v = System.getProperty("app.data-dir");
        return v == null || v.trim().isEmpty() ? "./data" : v.trim();
    }
}
