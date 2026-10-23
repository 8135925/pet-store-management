package com.petshop.config;

import com.petshop.module.system.BackupService;
import com.petshop.module.system.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Paths;

/** 首次启动：建目录、建 admin、打印启动横幅。 */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserService userService;
    private final AppProps props;
    private final BackupService backupService;

    public DataInitializer(UserService userService, AppProps props, BackupService backupService) {
        this.userService = userService;
        this.props = props;
        this.backupService = backupService;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Files.createDirectories(Paths.get(props.getDataDir()));
        Files.createDirectories(Paths.get(props.getBackupDir()));
        Files.createDirectories(Paths.get("logs"));

        userService.initAdmin("admin", props.getDefaultAdminPassword());

        log.info("============================================================");
        log.info("  宠物店管理系统启动完成");
        log.info("  访问地址:  http://localhost:8080");
        log.info("  数据文件:  {}", backupService.dbPath());
        log.info("  默认账号:  admin / {} （请登录后尽快修改密码）", props.getDefaultAdminPassword());
        log.info("============================================================");
    }
}
