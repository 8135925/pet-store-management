package com.petshop;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.petshop.module")
@EnableScheduling
public class PetShopApplication {
    public static void main(String[] args) throws Exception {
        // SQLite 数据文件所在目录必须先存在（sql init 早于 ApplicationRunner 执行）
        java.nio.file.Files.createDirectories(java.nio.file.Paths.get("./data"));
        java.nio.file.Files.createDirectories(java.nio.file.Paths.get("./logs"));
        SpringApplication.run(PetShopApplication.class, args);
    }
}
