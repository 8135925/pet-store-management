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
    /** 首次启动灌入示例数据（演示/验收用），生产保持 false */
    private boolean sampleData = false;

    public String getDataDir() { return dataDir; }
    public void setDataDir(String dataDir) { this.dataDir = dataDir; }
    public String getBackupDir() { return backupDir; }
    public void setBackupDir(String backupDir) { this.backupDir = backupDir; }
    public int getBackupKeep() { return backupKeep; }
    public void setBackupKeep(int backupKeep) { this.backupKeep = backupKeep; }
    public int getBusinessDayCutoffHour() { return businessDayCutoffHour; }
    public void setBusinessDayCutoffHour(int v) {
        this.businessDayCutoffHour = v;
        // 配置绑定后立即生效，避免 yml 里改了切点却仍按硬编码 04:00 算营业日
        com.petshop.common.BusinessDate.setCutoffHour(v);
    }
    public String getDefaultAdminPassword() { return defaultAdminPassword; }
    public void setDefaultAdminPassword(String v) { this.defaultAdminPassword = v; }
    public boolean isSampleData() { return sampleData; }
    public void setSampleData(boolean v) { this.sampleData = v; }
}
