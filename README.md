# pet-store-management · 宠物店管理系统（单店版 P0）

给单店宠物店用的**客户资产台账**：一人多宠、卡里还剩几次、疫苗哪天到期，全部说得清、查得到、能追溯。

## 技术栈

JDK 1.8 · Spring Boot 2.7.18 · MyBatis（手写 XML）· SQLite（WAL）· Thymeleaf 服务端渲染 · 零 CDN（断网可用）

## 快速开始

```bat
:: 1. 构建（需要 JDK 1.8 + Maven）
mvn clean package -DskipTests

:: 2. 双击 start.bat（或手动执行）
java -Dfile.encoding=UTF-8 -jar pet-shop.jar --spring.config.additional-location=file:./config/
```

- 浏览器访问 <http://localhost:8080>
- 默认账号 `admin / 123456`（首次启动自动创建，请尽快修改）
- 数据文件：`data/pet.db`；每日 03:00 自动备份到 `data/backup/`（保留 7 份 + 每月 1 号 1 份）

## 功能模块

| 模块 | 内容 |
|---|---|
| M1 档案 | 客户建档（手机号唯一）、一人多宠、聚合详情页、停用（禁物理删除） |
| M2 次卡 | 卡类型（付费/赠次分开存）、售卡生成收款单、核销流水、赠次优先、延期/作废、余额表（剩余实时算不落库） |
| M3 开单 | 商品+服务混单、体型四档价（<10kg 小 / 10–25 中 / 25–40 大 / ≥40 巨）、次卡抵扣金额记 0、整单折扣按占比分摊尾差归最大明细、表单 token 幂等、反结账（仅 ADMIN+当日+必填原因，同步回滚次数与库存） |
| M4 提醒 | 疫苗/狂犬/驱虫/体检记录、到期日自动推算、三档到期清单、逾期标红、联系标记 |
| M5 库存 | 商品档案、出入库流水（只增不改）、临期/低库存预警、页面无直接改库存入口 |
| M6 报表 | 首页 6 数字看板、日营业汇总（售卡/商品/服务三列分列）、次卡余额表 |
| M7 系统 | 登录（BCrypt）、用户管理、门店参数、每日+手动备份、启动脚本 |

## 业务红线（不可妥协）

- 金额全库用「分」存整数（`_cent` 后缀），禁止浮点入库
- 卡剩余次数不落库，实时 `paid - used_paid + gift - used_gift`
- `card_consume_log` / `product_ledger` 只允许 INSERT
- 核销与库存变更在同一事务；反结账按流水 `from_gift` 精确还原
- 不接支付通道、不做退款、不做储值余额

## 目录结构

```
pet-shop.jar / start.bat / start.sh / stop.sh   交付运行件
config/application.yml                          外置配置（端口等）
data/pet.db · data/backup/                      数据与备份
logs/                                           按天滚动日志（30 天）
doc/                                            需求规格说明书 / 设计说明书 / schema.sql
src/main/java/com/petshop                       按业务垂直分层（common/config/interceptor/handler/module/*）
src/main/resources/mapper                       MyBatis XML
src/main/resources/templates                    Thymeleaf 页面
```
