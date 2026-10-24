# pet-store-management · 宠物店管理系统（单店版）

给单店宠物店用的**客户资产台账**：一人多宠、卡里还剩几次、疫苗哪天到期，全部说得清、查得到、能追溯。

- **v1.1** 新增寄养与预约：旺季笼位不会乱，周末不会撞单
- **v1.2** 新增库存批次/盘点、员工提成、操作审计，以及数据体检与备份恢复等运维能力

## 技术栈

JDK 1.8 · Spring Boot 2.7.18 · MyBatis（手写 XML）· SQLite（WAL）· Thymeleaf 服务端渲染 · 零 CDN（断网可用）

## 快速开始

```bat
:: 1. 构建（需要 JDK 1.8 + Maven）
mvn clean package -DskipTests

:: 2. 解压 target/pet-shop_1.0.0_Bin.zip 到任意目录（内置三平台 JRE，无需安装 Java）
:: 3. Windows 双击 start.bat；Linux 执行 ./start.sh（停止用 ./stop.sh）
```

- 浏览器访问 <http://localhost:8080>
- 默认账号 `admin / 123456`（首次启动自动创建，请尽快修改；生产环境用环境变量
  `APP_DEFAULT_ADMIN_PASSWORD` 覆盖初始密码）
- 数据文件：`data/pet.db`；每日 03:00 自动备份到 `data/backup/`（保留 7 份 + 每月 1 号 1 份）
- 演示/验收环境可在 config 里加 `app.sample-data: true`，首次启动自动灌入示例数据

## 功能模块

| 模块 | 内容 |
|---|---|
| M1 档案 | 客户建档（手机号唯一）、一人多宠、聚合详情页、停用（禁物理删除） |
| M2 次卡 | 卡类型（付费/赠次分开存）、售卡生成收款单、核销流水、赠次优先、延期/作废、余额表（剩余实时算不落库） |
| M3 开单 | 商品+服务混单、体型四档价、**条码扫码开单（v1.1）**、次卡抵扣金额记 0、整单折扣分摊、token 幂等、反结账 |
| M4 提醒 | 疫苗/狂犬/体内驱虫(+90)/**体外驱虫(+30，v1.1)**/体检、到期日自动推算、三档清单、**一键拨号+复制（v1.1）** |
| M5 库存 | 商品档案、出入库流水（只增不改，**含成本快照 v1.1**）、临期/低库存预警 |
| M6 报表 | 首页 **8 数字看板（v1.1 +在店寄养/今日预约）**、日营业汇总三列、**月度汇总+商品毛利（v1.1）**、次卡余额表 |
| M7 系统 | 登录（BCrypt + 会话固定防护）、用户管理、门店参数、每日+手动备份、启动脚本 |
| **M8 寄养（v1.1）** | 笼位管理、新建寄养（**疫苗拦截 BR-09 + 笼位互斥 BR-10**）、按晚计费（BR-11）、每日喂养记录、退住结算自动生成账单、超期标红 |
| **M9 预约（v1.1）** | 预约看板（按时段）、时段容量限制、到店 → 转开单（预填服务）、寄养互斥提示 |
| **v1.2 库存增强** | 商品批次与效期（FEFO 出库）、采购单入库/作废、盘点（确认才调账）、低库存补货建议 |
| **v1.2 提成** | 提成规则（服务/商品/售卡/寄养）、结账自动生成、反结账自动冲销、月度结算与确认 |
| **v1.2 运维** | 操作审计（关键动作留痕，只增不改）、数据体检（结存 vs 流水/批次、优惠分摊、卡次数、寄养结算、提成缺失）、备份恢复（上传/选择备份 → 校验 → 重启生效 → 维护模式） |

## 角色与权限

| 操作 | 店主（ADMIN） | 店员（STAFF） |
|---|:---:|:---:|
| 开单收银、结账、反结账 | ✅ | ✅ / ❌ |
| 客户/宠物档案维护 | ✅ | ✅ |
| 寄养、预约、到期提醒处理 | ✅ | ✅ |
| 服务/商品价目、寄养看板、报表查看 | ✅ | ✅ |
| 售卡 | ✅ | ❌ |
| 商品新增/编辑/上下架、手工库存调整 | ✅ | ❌ |
| 供应商、采购、批次报损、盘点 | ✅ | ❌ |
| 卡延期/作废 | ✅ | ❌ |
| 员工与提成管理、提成结算 | ✅ | ❌ |
| 用户管理、门店参数、备份/恢复/体检/审计 | ✅ | ❌ |

服务端双重校验：登录拦截器（未登录跳登录页）+ 管理员拦截器（`ADMIN` 之外一律拒绝），不信任前端隐藏。

## 业务红线（不可妥协）

- 金额全库用「分」存整数（`_cent` 后缀），禁止浮点入库
- 卡剩余次数不落库，实时 `paid - used_paid + gift - used_gift`
- `card_consume_log` / `product_ledger` / `boarding_log` / `audit_log` 只允许 INSERT
- 核销与库存变更在同一事务；反结账按流水 `from_gift` 精确还原、提成写负数冲销
- 寄养入住必须疫苗有效（无记录视为过期）；笼位时间区间互斥
- 不接支付通道、不做退款、不做储值余额

## 打包与部署

`mvn package` 在 `target/` 产出 **`pet-shop_{version}_Bin.zip`**（`maven-assembly-plugin`，见 `assembly/assembly.xml`）：

```
pet-shop_{version}_Bin/
├── jdk/windows | linux-x64 | linux-arm64   # 内置 JRE 8，解压即用
├── config/application.yml                  # 外置配置（端口/默认管理员密码等）
├── pet-shop.jar                            # 可执行主程序
├── start.bat / start.sh / stop.sh          # 启停脚本（Linux 自动识别架构）
└── 使用说明.txt
```

解压即用，目标机器无需安装 Java。

## 目录结构

```
assembly/                                       打包原料：三平台内置 JRE + 启停脚本 + assembly.xml
config/application.yml                          外置配置（端口等）
data/pet.db · data/backup/                      数据与备份
logs/                                           按天滚动日志（30 天）
doc/                                            需求规格 / 设计说明书 v1.0–v1.2 / schema.sql / Design.md
src/main/java/com/petshop                       按业务垂直分层（module/ 下每模块 controller/service/mapper）
src/main/resources/mapper                       MyBatis XML
src/main/resources/templates                    Thymeleaf 页面（Tesla 风格设计系统，见 doc/Design.md）
src/test                                        Money/BusinessDate/CardDeduct/ReportSql 等单元与集成测试
target/pet-shop_{version}_Bin.zip               交付运行件（mvn package 产出）
```

## 开发

```bat
mvn test        :: 运行全部测试（含运维模块集成测试）
mvn spring-boot:run :: 本地启动（数据落在 ./data）
```

- 页面主题遵循 `doc/Design.md`（Tesla 风格）：白色画布 + 电光蓝唯一强调色、无阴影无渐变、控件 4px 圆角
- 图标使用本地打包的 [Lucide](https://lucide.dev/)（`static/js/lucide.min.js`），保持断网可用

## 文档

| 文档 | 说明 |
|---|---|
| `doc/宠物店管理系统-需求规格说明书-v1.0.md` | P0/P1 需求（FR/BR 编号） |
| `doc/宠物店管理系统-需求与设计说明书-v1.1.md` | 寄养与预约 |
| `doc/宠物店管理系统-三期需求与设计说明书-v1.2.md` | 库存/提成/审计/运维 |
| `doc/Design.md` | 前端设计系统规范 |
| `doc/schema.sql` · `src/main/resources/schema/schema.sql` | 数据库结构 |
