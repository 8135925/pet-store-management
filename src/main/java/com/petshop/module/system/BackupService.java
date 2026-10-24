package com.petshop.module.system;

import com.petshop.common.BizException;
import com.petshop.common.ErrorCode;
import com.petshop.config.AppProps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

/** 备份：每日 03:00 自动 + 手动按钮；VACUUM INTO 生成一致性快照；保留最近 N 份 + 每月 1 号 1 份。 */
@Service
public class BackupService {

    private static final Logger log = LoggerFactory.getLogger(BackupService.class);
    private static final Pattern FILE_PAT = Pattern.compile("pet-\\d{8}-\\d{4,6}\\.db");
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final AppProps props;
    private final JdbcTemplate jdbc;

    public BackupService(AppProps props, JdbcTemplate jdbc) {
        this.props = props;
        this.jdbc = jdbc;
    }

    /** 每天凌晨 3 点自动备份 */
    @Scheduled(cron = "0 0 3 * * ?")
    public void scheduledBackup() {
        try {
            doBackup();
        } catch (Exception e) {
            log.error("自动备份失败", e);
        }
    }

    /** 手动备份，返回备份文件名 */
    public String manualBackup() {
        return doBackup();
    }

    public List<String> listBackups() {
        Path dir = Paths.get(props.getBackupDir());
        List<String> names = new ArrayList<>();
        if (!Files.isDirectory(dir)) {
            return names;
        }
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir, "pet-*.db")) {
            for (Path p : ds) {
                if (FILE_PAT.matcher(p.getFileName().toString()).matches()) {
                    names.add(p.getFileName().toString());
                }
            }
        } catch (IOException e) {
            log.error("读取备份目录失败", e);
        }
        names.sort(Comparator.reverseOrder());
        return names;
    }

    private String doBackup() {
        // 文件名精确到秒，避免同一分钟内多次备份互相覆盖（VACUUM INTO 不允许覆盖已存在文件）；
        // 同一秒内连续两次备份（如恢复前的预备份）会撞名，等到下一秒再生成
        String name = null;
        Path dir = Paths.get(props.getBackupDir());
        try {
            Files.createDirectories(dir);
            Path target;
            int waited = 0;
            do {
                name = "pet-" + LocalDateTime.now().format(TS_FMT) + ".db";
                target = dir.resolve(name);
                if (Files.exists(target)) {
                    Thread.sleep(50);
                    waited++;
                }
            } while (Files.exists(target) && waited < 40);
            if (Files.exists(target)) {
                throw new IllegalStateException("同一秒内备份过于频繁，无法生成唯一文件名");
            }
            // VACUUM INTO 的目标是字符串字面量，单引号必须转义，否则配置路径含 ' 会破坏语句
            String path = target.toAbsolutePath().toString().replace("\\", "/").replace("'", "''");
            jdbc.execute("VACUUM INTO '" + path + "'");
            cleanOldBackups(dir);
            log.info("备份完成：{}", path);
            return name;
        } catch (Exception e) {
            log.error("备份失败：{}", name, e);
            throw new BizException(ErrorCode.BACKUP_FAILED);
        }
    }

    /** 保留最近 backupKeep 份 + 每月 1 号的那份长期保留 */
    private void cleanOldBackups(Path dir) throws IOException {
        List<Path> files = new ArrayList<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir, "pet-*.db")) {
            for (Path p : ds) {
                if (FILE_PAT.matcher(p.getFileName().toString()).matches()) {
                    files.add(p);
                }
            }
        }
        files.sort(Comparator.comparing((Path p) -> p.getFileName().toString()).reversed());
        int keep = Math.max(props.getBackupKeep(), 1);
        for (int i = keep; i < files.size(); i++) {
            String n = files.get(i).getFileName().toString();
            // 每月 1 号备份长期保留（pet-YYYYMM01-*.db）
            boolean monthly = n.matches("pet-\\d{6}01-\\d{4,6}\\.db");
            if (!monthly) {
                Files.deleteIfExists(files.get(i));
            }
        }
    }

    /** 数据目录绝对路径（启动横幅与排障用） */
    public String dbPath() {
        return Paths.get(props.getDataDir(), "pet.db").toAbsolutePath().toString();
    }

    public LocalDate today() {
        return LocalDate.now();
    }
}
