package com.petshop.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** app.* 配置项 */
@Component
@ConfigurationProperties(prefix = "app")
public class AppProps {

    private String dataDir = "./data";
    private String backupDir = "./data/backup";
    private int backupKeep = 7;
    private int businessDayCutoffHour = 4;
    private String defaultAdminPassword = "123456";

    public String getDataDir() { return dataDir; }
    public void setDataDir(String dataDir) { this.dataDir = dataDir; }
    public String getBackupDir() { return backupDir; }
    public void setBackupDir(String backupDir) { this.backupDir = backupDir; }
    public int getBackupKeep() { return backupKeep; }
    public void setBackupKeep(int backupKeep) { this.backupKeep = backupKeep; }
    public int getBusinessDayCutoffHour() { return businessDayCutoffHour; }
    public void setBusinessDayCutoffHour(int v) { this.businessDayCutoffHour = v; }
    public String getDefaultAdminPassword() { return defaultAdminPassword; }
    public void setDefaultAdminPassword(String v) { this.defaultAdminPassword = v; }
}
