-- ============================================================================
-- 宠物店管理系统 · 数据库结构（SQLite 3）
-- 文件位置：src/main/resources/schema/schema.sql
--
-- 【关于注释】SQLite 不支持 MySQL 那种 COMMENT ON COLUMN / 列级 COMMENT 语法，
-- 因此本文件把字段说明统一写成行内 `--` 注释。执行时会被 SQL 解释器忽略，
-- 但对人和 AI 阅读是有效的。任何只认「建表语句本身」的工具也能正常执行。
--
-- 【命名约定】
--   *_cent       金额，单位「分」，INTEGER。禁止 REAL/FLOAT/DOUBLE。
--   *_g          重量，单位「克」，INTEGER。
--   *_on         日期，TEXT 'yyyy-MM-dd'。
--   *_at         时间点，TEXT 'yyyy-MM-dd HH:mm:ss'。
--   is_*         布尔，INTEGER 0/1（SQLite 无布尔类型）。
--   无后缀        数量，INTEGER。
--
-- 【为什么要这么多注释】字段名的语义比类型重要。「used_paid 是已用付费次数
--   还是余额」这种歧义，写在注释里是 5 秒钟的事，写错代码是半天的事。
-- ============================================================================


-- ============================================================================
-- 1. customer 客户档案
--    付费主体。宠物是「被服务对象」，客户是「付钱的人」，一人可多宠。
-- ============================================================================
CREATE TABLE IF NOT EXISTS customer (
  id         INTEGER PRIMARY KEY AUTOINCREMENT, -- 主键，自增
  name       TEXT    NOT NULL,                  -- 客户姓名（或称呼），单据与提醒里显示这个
  phone      TEXT    NOT NULL,                  -- 手机号，全局唯一；查重、召回、熟客识别都靠它
  source     TEXT,                              -- 客户来源渠道（自然到店/朋友介绍/美团/抖音…），自由文本，仅用于统计拉新效果
  remark     TEXT,                              -- 备注：住址、接送习惯、砍价记录、投诉历史等
  is_active  INTEGER NOT NULL DEFAULT 1,        -- 是否有效：1=正常 0=已停用。逻辑删除，名下可能有单据，不可物理删除
  created_at TEXT    NOT NULL                   -- 建档时间 'yyyy-MM-dd HH:mm:ss'
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_customer_phone ON customer(phone); -- 手机号唯一：同一部手机不允许建两个客户，否则次卡余额会分裂


-- ============================================================================
-- 2. pet 宠物档案
--    一个人可以有多只宠物 → owner_id 指向 customer，父子结构。
-- ============================================================================
CREATE TABLE IF NOT EXISTS pet (
  id           INTEGER PRIMARY KEY AUTOINCREMENT, -- 主键，自增
  owner_id     INTEGER NOT NULL REFERENCES customer(id), -- 主人 customer.id；一人多宠靠这个字段体现
  name         TEXT    NOT NULL,                  -- 宠物昵称，叫「豆豆」而不是「编号 7」
  species      TEXT    NOT NULL,                  -- 物种：DOG 狗 / CAT 猫 / OTHER 其他；决定可选品种与疫苗模板
  breed        TEXT,                              -- 品种，如 金毛/泰迪/英短；决定毛型与美容方案
  gender       TEXT,                              -- 性别：MALE 公 / FEMALE 母 / UNKNOWN 未知
  birthday     TEXT,                              -- 生日 'yyyy-MM-dd'。★不存「年龄」字段：年龄必须实时算，否则每天都得过期一次
  weight_g     INTEGER,                           -- 体重，单位克（12.5kg 存 12500）；决定洗护体型档与用药剂量
  is_neutered  INTEGER NOT NULL DEFAULT 0,        -- 是否已绝育：1=是 0=否；影响护理建议与部分服务禁忌
  color        TEXT,                              -- 毛色，美容师认狗用（「白色比熊」比「豆豆」更好认）
  habit        TEXT,                              -- 习性/禁忌：怕吹风机、会咬人、某浴液过敏、不配合剪指甲……出事前的唯一提示
  is_active    INTEGER NOT NULL DEFAULT 1,        -- 是否有效：1=在册 0=已离世/已转让。离世的宠物不能物理删，否则历史单据无法解释
  created_at   TEXT    NOT NULL                   -- 建档时间
);
CREATE INDEX IF NOT EXISTS idx_pet_owner ON pet(owner_id); -- 按主人查宠物列表（开单选宠物、档案详情页）


-- ============================================================================
-- 3. pet_event 免疫/驱虫/体检记录
--    这是整个系统的「复购引擎」：宠物店的回头客不靠促销，靠到期提醒。
-- ============================================================================
CREATE TABLE IF NOT EXISTS pet_event (
  id             INTEGER PRIMARY KEY AUTOINCREMENT, -- 主键，自增
  pet_id         INTEGER NOT NULL REFERENCES pet(id), -- 属于哪只宠物
  event_type     TEXT    NOT NULL,                  -- 事件类型：VACCINE 疫苗 / RABIES 狂犬 / DEWORM 驱虫 / CHECKUP 体检
  happened_on    TEXT    NOT NULL,                  -- 本次发生日期 'yyyy-MM-dd'（上次打针/驱虫那天）
  next_due_on    TEXT,                              -- 下次到期日 'yyyy-MM-dd'。为空表示一次性记录、不进提醒清单；提醒只扫非空行
  hospital       TEXT,                              -- 接种/就诊机构；出问题时客户一定会问「在哪打的」，必须可追溯
  remark         TEXT,                              -- 备注：疫苗批号、品牌、不良反应等
  contact_status TEXT,                              -- 提醒跟进状态：NULL=未处理 / CONTACTED=已联系未成交 / BOOKED=已预约 / DONE=已完成 / IGNORED=不再提醒（客户明确拒绝）
  contacted_at   TEXT,                              -- 最近一次跟进时间。为空表示还没人打过电话
  contacted_by   TEXT,                              -- 跟进人（员工显示名），业绩与追责用
  created_at     TEXT    NOT NULL                   -- 记录创建时间
);
CREATE INDEX IF NOT EXISTS idx_event_due ON pet_event(next_due_on, contact_status); -- 首页「今日到期提醒」按此索引扫描，只取未处理的行


-- ============================================================================
-- 4. service_item 服务项目
--    洗护/美容/药浴等项目，按体型分四档价 —— 这是宠物行业与普通零售最大的差别。
-- ============================================================================
CREATE TABLE IF NOT EXISTS service_item (
  id             INTEGER PRIMARY KEY AUTOINCREMENT, -- 主键，自增
  name           TEXT    NOT NULL,                  -- 服务名称，如「精洗」「剪毛造型」「药浴」
  duration_min   INTEGER NOT NULL DEFAULT 60,       -- 标准耗时（分钟），用于 P1 预约看板算占位；体型越大实际越久，此处为基准值
  price_s_cent   INTEGER NOT NULL DEFAULT 0,        -- 小体型价（≤5kg），单位分；0 表示该体型不接此服务，不是免费
  price_m_cent   INTEGER NOT NULL DEFAULT 0,        -- 中体型价（5–15kg），单位分
  price_l_cent   INTEGER NOT NULL DEFAULT 0,        -- 大体型价（15–30kg），单位分
  price_xl_cent  INTEGER NOT NULL DEFAULT 0,        -- 超大体型价（>30kg），单位分；金毛/阿拉斯加按这档收
  sort_no        INTEGER NOT NULL DEFAULT 0,        -- 排序号，小的在前；开单页按此顺序排列，常用项目排前面
  is_active      INTEGER NOT NULL DEFAULT 1         -- 是否在售：1=可开单 0=已下架。停售不影响历史单据（单据存的是名称快照）
);


-- ============================================================================
-- 5. product 商品
--    狗粮、零食、用品等实物。库存数量存这里，流水记在 product_ledger。
-- ============================================================================
CREATE TABLE IF NOT EXISTS product (
  id              INTEGER PRIMARY KEY AUTOINCREMENT, -- 主键，自增
  name            TEXT    NOT NULL,                  -- 商品名称，含规格（如「皇家A3 幼犬粮 2kg」）；规格要写进名称，否则库存没法区分
  barcode         TEXT,                              -- 条码，可空；扫码枪录入用。一物多码时只存主条码
  sale_price_cent INTEGER NOT NULL DEFAULT 0,        -- 零售价，单位分
  cost_price_cent INTEGER NOT NULL DEFAULT 0,        -- 参考成本价，单位分；仅用于毛利估算与进货提醒，不参与库存计价
  stock_qty       INTEGER NOT NULL DEFAULT 0,        -- 当前库存数量。★这是「结存表」，必须恒等于该商品在 product_ledger 中的 qty_change 之和
  safety_qty      INTEGER NOT NULL DEFAULT 0,        -- 安全库存，低于此值进首页「低库存预警」；0 表示不预警
  expire_on       TEXT,                              -- 有效期至 'yyyy-MM-dd'。★仅适用于「整店单一效期」；同商品多批次不同效期时此字段不可靠，需留空并在 P2 上批次表
  is_active       INTEGER NOT NULL DEFAULT 1,        -- 是否在售：1=可开单 0=已停售
  created_at      TEXT    NOT NULL                   -- 建档时间
);
CREATE INDEX IF NOT EXISTS idx_product_barcode ON product(barcode); -- 扫码枪按条码定位商品


-- ============================================================================
-- 6. product_ledger 库存流水
--    ★ 只允许 INSERT，禁止 UPDATE / DELETE。流水就是证据，改了就无法自证清白。
-- ============================================================================
CREATE TABLE IF NOT EXISTS product_ledger (
  id          INTEGER PRIMARY KEY AUTOINCREMENT, -- 主键，自增
  product_id  INTEGER NOT NULL REFERENCES product(id), -- 变动的是哪个商品
  biz_type    TEXT    NOT NULL,                  -- 变动原因：IN 采购入库 / SALE 销售出库 / RETURN 退货入库 / CANCEL 反结账回滚 / ADJUST 盘点调整
  qty_change  INTEGER NOT NULL,                  -- 变动数量：入库为正、出库为负，不允许 0
  before_qty  INTEGER NOT NULL,                  -- 变动前库存快照；与 after_qty 一起构成完整证据链
  after_qty   INTEGER NOT NULL,                  -- 变动后库存快照；必须满足 after_qty = before_qty + qty_change
  unit_cost_cent INTEGER NOT NULL DEFAULT 0,     -- v1.1 增列：出库时成本单价快照（分），毛利报表用；v1.0 历史数据为 0
  ref_type    TEXT,                              -- 关联单据类型：BILL 销售单 / MANUAL 手工调整
  ref_id      INTEGER,                           -- 关联单据 id；ref_type=BILL 时指向 bill.id
  operator    TEXT,                              -- 操作人（员工显示名）
  remark      TEXT,                              -- 备注：手工调整必须写原因
  created_at  TEXT    NOT NULL                   -- 发生时间；与 product.stock_qty 同事务写入
);
CREATE INDEX IF NOT EXISTS idx_ledger_product ON product_ledger(product_id, created_at); -- 按商品查流水、对账（流水求和 = 结存）


-- ============================================================================
-- 7. card_type 卡类型（卡模板/货架商品）
--    这是「货架上卖的卡」，不是「客户手里那张卡」。改这里不影响已售出的卡。
-- ============================================================================
CREATE TABLE IF NOT EXISTS card_type (
  id          INTEGER PRIMARY KEY AUTOINCREMENT, -- 主键，自增
  name        TEXT    NOT NULL,                  -- 卡名称，如「洗护10次卡（送2次）」
  service_ids TEXT    NOT NULL,                  -- 适用服务项目 id 列表，逗号分隔，如 '3,4,7'；核销时校验，不匹配则拒绝 CARD_NOT_MATCH_SERVICE
  paid_qty    INTEGER NOT NULL,                  -- 付费次数（客户真正掏钱买的部分）
  gift_qty    INTEGER NOT NULL DEFAULT 0,        -- 赠送次数；★必须与 paid_qty 分开记：退款只退 paid 部分，营收只确认 paid 部分
  price_cent  INTEGER NOT NULL,                  -- 售价，单位分
  valid_days  INTEGER NOT NULL DEFAULT 365,      -- 有效天数，自发卡日起算；到期日 = issued_on + valid_days
  is_active   INTEGER NOT NULL DEFAULT 1         -- 是否可售：1=货架可见 0=已停售（不影响已发出的卡）
);


-- ============================================================================
-- 8. customer_card 客户持有的卡
--    ★ 卡是「资产」不是「优惠券」。这里记录的是客户已购得的权利。
--    ★ 剩余次数不存字段！实时算：paid_qty - used_paid + gift_qty - used_gift
--      存了 remaining 就一定会有不同步的那天，而那正是客户投诉「你们这卡有鬼」的来源。
-- ============================================================================
CREATE TABLE IF NOT EXISTS customer_card (
  id               INTEGER PRIMARY KEY AUTOINCREMENT, -- 主键，自增
  card_no          TEXT    NOT NULL,                  -- 卡号，唯一，人可读（规则见设计文档 §7.7）；客户报卡号、纠纷时对账都靠它
  customer_id      INTEGER NOT NULL REFERENCES customer(id), -- 持卡客户
  card_type_id     INTEGER NOT NULL REFERENCES card_type(id), -- 来源卡类型；仅用于展示「这卡是怎么卖的」，次数与金额已在本表固化，不随卡类型改动
  paid_qty         INTEGER NOT NULL,                  -- 本卡付费次数（发卡时从卡类型复制过来的快照，之后不再变）
  gift_qty         INTEGER NOT NULL DEFAULT 0,        -- 本卡赠送次数（同样为发卡时快照）
  used_paid        INTEGER NOT NULL DEFAULT 0,        -- 已核销的付费次数；★只由核销/反结账变更，且必须与 card_consume_log 对应
  used_gift        INTEGER NOT NULL DEFAULT 0,        -- 已核销的赠送次数；扣减顺序由 store_config.gift_first 决定，默认先扣这项
  paid_amount_cent INTEGER NOT NULL DEFAULT 0,        -- 本卡实收金额，单位分；可能低于 card_type.price_cent（打折/抹零），售卡收入以本字段为准
  issued_on        TEXT    NOT NULL,                  -- 发卡日期 'yyyy-MM-dd'
  expire_on        TEXT    NOT NULL,                  -- 到期日 'yyyy-MM-dd'；延期时改这个字段并写 card_extend_log
  status           TEXT    NOT NULL DEFAULT 'ACTIVE', -- 卡状态：ACTIVE 可用 / EXPIRED 已过期（★余额不清零，可查可延期，只是不能核销）/ VOID 已作废（终态）
  source_bill_id   INTEGER,                           -- 售卡单 bill.id；反查「这笔钱是哪天收的」
  remark           TEXT,                              -- 备注
  created_at       TEXT    NOT NULL                   -- 创建时间
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_card_no ON customer_card(card_no); -- 卡号唯一
CREATE INDEX IF NOT EXISTS idx_card_customer ON customer_card(customer_id, status); -- 客户详情页列卡、开单时筛可用卡
CREATE INDEX IF NOT EXISTS idx_card_expire ON customer_card(expire_on); -- 批量把到期卡置为 EXPIRED（定时任务用）


-- ============================================================================
-- 9. card_consume_log 卡核销流水
--    ★ 只允许 INSERT。反结账靠读这里的 from_gift 还原，不能靠当前开关重算。
-- ============================================================================
CREATE TABLE IF NOT EXISTS card_consume_log (
  id           INTEGER PRIMARY KEY AUTOINCREMENT, -- 主键，自增
  card_id      INTEGER NOT NULL REFERENCES customer_card(id), -- 被核销的卡
  bill_id      INTEGER NOT NULL,                  -- 核销发生在哪张销售单 bill.id（★不设外键，避免反结账删单时级联问题）
  bill_item_id INTEGER NOT NULL,                  -- 对应哪一行明细 bill_item.id；反结账按行精确还原次数
  qty          INTEGER NOT NULL DEFAULT 1,        -- 本次核销次数，当前固定 1；保留字段以支持将来一次核销多次
  from_gift    INTEGER NOT NULL DEFAULT 0,        -- ★本次扣的是哪一部分：1=赠送次数 0=付费次数。反结账必须按本字段还原，不能按当前 gift_first 开关重算（开关可能改过，重算会让 used_gift 变负数）
  operator     TEXT,                              -- 操作人
  created_at   TEXT    NOT NULL                   -- 核销时间
);
CREATE INDEX IF NOT EXISTS idx_consume_card ON card_consume_log(card_id, created_at); -- 查某张卡的完整使用历史（纠纷时出示）
CREATE INDEX IF NOT EXISTS idx_consume_bill ON card_consume_log(bill_id); -- 反结账时按单号捞本轮核销


-- ============================================================================
-- 10. card_extend_log 卡延期记录
--     ★ 延期等于单方面送钱给客户，必须留痕：谁改的、为什么、改前是几号。
-- ============================================================================
CREATE TABLE IF NOT EXISTS card_extend_log (
  id            INTEGER PRIMARY KEY AUTOINCREMENT, -- 主键，自增
  card_id       INTEGER NOT NULL REFERENCES customer_card(id), -- 被延期的卡
  old_expire_on TEXT    NOT NULL,                  -- 延期前的到期日
  new_expire_on TEXT    NOT NULL,                  -- 延期后的到期日（必须晚于旧值）
  reason        TEXT    NOT NULL,                  -- 延期原因，必填：客户投诉、疫情停业、员工失误补偿……这是唯一的追责凭证
  operator      TEXT    NOT NULL,                  -- 操作人（仅 ADMIN 有权限）
  created_at    TEXT    NOT NULL                   -- 操作时间
);


-- ============================================================================
-- 11. bill 单据
--     两种单共表：SALE 销售/服务单（有明细）、CARD 售卡收款单（无明细）。
-- ============================================================================
CREATE TABLE IF NOT EXISTS bill (
  id              INTEGER PRIMARY KEY AUTOINCREMENT, -- 主键，自增
  bill_no         TEXT    NOT NULL,                  -- 单号，唯一，人可读（前缀+营业日+当日序号）；客户报单号、打印小票都用它
  bill_type       TEXT    NOT NULL,                  -- 单据类型：SALE 销售/服务单 / CARD 售卡收款单（★售卡单不写 bill_item，金额直接从 received_cent 取）
  customer_id     INTEGER NOT NULL REFERENCES customer(id), -- 客户；售卡单也必须挂客户（否则卡没有主人）
  pet_id          INTEGER,                           -- 本次服务的主宠物；售卡单为空。混合单（一单多宠）以明细行的 pet_id 为准，此字段仅作默认值
  status          TEXT    NOT NULL DEFAULT 'OPEN',   -- 单据状态：OPEN 未结账 / SETTLED 已结账 / CANCELLED 已反结账（终态，需恢复请重开一单）
  total_cent      INTEGER NOT NULL DEFAULT 0,        -- 明细金额合计（优惠前），单位分
  discount_cent   INTEGER NOT NULL DEFAULT 0,        -- 整单优惠金额，单位分；分摊到明细的尾差归金额最大那一行
  receivable_cent INTEGER NOT NULL DEFAULT 0,        -- 应收 = total_cent - discount_cent
  received_cent   INTEGER NOT NULL DEFAULT 0,        -- 实收金额。★找零在系统外处理：现金收 100 应收 88，这里仍记 88，不记 100
  pay_method      TEXT,                              -- 收款方式：CASH 现金 / WECHAT 微信 / ALIPAY 支付宝 / BANK_CARD 银行卡 / OTHER 其他。★只做记账，不接任何支付通道。（刻意不用 'CARD' 以免与 bill_type 的 CARD 混淆）
  business_date   TEXT    NOT NULL,                  -- 营业日 'yyyy-MM-dd'：凌晨 04:00 前算前一天。★所有报表按本字段分组，不看 created_at
  settled_at      TEXT,                              -- 结账时间；未结账为 NULL
  cancelled_at    TEXT,                              -- 反结账时间；反结账后可查但不可再结
  cancel_reason   TEXT,                              -- 反结账原因，必填；这是对账时唯一能解释差异的字段
  request_id      TEXT,                              -- 提交幂等键，前端每次打开结账页生成一个 UUID；★重复提交撞唯一索引即拒绝，防止双击出两张单
  operator        TEXT,                              -- 开单人（员工显示名）
  remark          TEXT,                              -- 备注
  created_at      TEXT    NOT NULL                   -- 创建时间
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_bill_no ON bill(bill_no); -- 单号唯一
CREATE UNIQUE INDEX IF NOT EXISTS uk_bill_request ON bill(request_id); -- 幂等键唯一：并发重复提交时由数据库兜底拦截
CREATE INDEX IF NOT EXISTS idx_bill_date ON bill(business_date, status); -- 日报/营业汇总按营业日+状态扫
CREATE INDEX IF NOT EXISTS idx_bill_customer ON bill(customer_id); -- 客户消费历史


-- ============================================================================
-- 12. bill_item 单据明细
--     ★ 只存快照，不存引用：名字、单价、金额在下单时冻结，之后改价改名都不回改历史单。
-- ============================================================================
CREATE TABLE IF NOT EXISTS bill_item (
  id              INTEGER PRIMARY KEY AUTOINCREMENT, -- 主键，自增
  bill_id         INTEGER NOT NULL REFERENCES bill(id), -- 所属单据
  item_type       TEXT    NOT NULL,                  -- 明细类型：SERVICE 服务 / PRODUCT 商品
  ref_id          INTEGER NOT NULL,                  -- 指向 service_item.id 或 product.id，按 item_type 解释（★不设外键：两个目标表共用一个字段）
  name_snapshot   TEXT    NOT NULL,                  -- 名称快照，必填；改名后历史单据不能跟着变，否则客户会说「我当时买的不是这个」
  size_tier       TEXT,                              -- 服务体型档：S / M / L / XL，决定用哪一档价；商品行为空
  pet_id          INTEGER,                           -- 本行服务的宠物；一单多宠必须按行记录，否则不知道钱花在谁身上
  qty             INTEGER NOT NULL DEFAULT 1,        -- 数量（服务次数 / 商品件数）
  unit_price_cent INTEGER NOT NULL DEFAULT 0,        -- 成交单价，单位分；已含体型档价与手工改价，不是挂牌价
  amount_cent     INTEGER NOT NULL DEFAULT 0,        -- 行金额 = unit_price_cent × qty（优惠前）
  discount_cent   INTEGER NOT NULL DEFAULT 0,        -- 分摊到本行的优惠金额；各行之和必须等于 bill.discount_cent
  paid_by_card    INTEGER NOT NULL DEFAULT 0,        -- 本行是否用次卡抵扣：1=是 0=否。★=1 时该行不计营业收入（钱在售卡当天已收过，再算一次会让当天营收虚高）
  card_id         INTEGER,                           -- 抵扣所用的 customer_card.id；paid_by_card=0 时为空
  sort_no         INTEGER NOT NULL DEFAULT 0         -- 打印/显示顺序
);
CREATE INDEX IF NOT EXISTS idx_item_bill ON bill_item(bill_id); -- 打开单据详情、反结账按单捞明细


-- ============================================================================
-- 13. store_config 门店参数
--     键值对，避免为几个开关改表结构。值统一存字符串，读取时再转换类型。
-- ============================================================================
CREATE TABLE IF NOT EXISTS store_config (
  k TEXT PRIMARY KEY,  -- 参数名，如 store_name（店名，打印在单据抬头）/ gift_first（true=核销先扣赠送次数）/ expire_warn_days（提醒提前天数，默认 30）/ low_stock_warn（低库存阈值，默认 3）
  v TEXT NOT NULL      -- 参数值，字符串形式；布尔用 'true'/'false'，数字直接写
);


-- ============================================================================
-- 14. sys_user 系统用户（店员）
--     ★ 管理员不写在 data.sql 里：BCrypt 哈希无法手工维护，改由 DataInitializer
--       在启动时判断 sys_user 为空再创建，否则会出现「哈希与编码器版本不匹配，密码永远不对」。
-- ============================================================================
CREATE TABLE IF NOT EXISTS sys_user (
  id            INTEGER PRIMARY KEY AUTOINCREMENT, -- 主键，自增
  username      TEXT    NOT NULL,                  -- 登录账号，唯一
  password_hash TEXT    NOT NULL,                  -- BCrypt 哈希（strength 10），★绝不存明文，也绝不回显
  display_name  TEXT    NOT NULL,                  -- 显示名，会写进单据的 operator 字段和操作日志
  role          TEXT    NOT NULL DEFAULT 'STAFF',  -- 角色：ADMIN 管理员（可作废卡、延期、反结账）/ STAFF 店员（只能开单结账核销）
  is_active     INTEGER NOT NULL DEFAULT 1,        -- 是否启用：1=可登录 0=已停用（离职不删账号，否则单据上的操作人就成了孤儿）
  created_at    TEXT    NOT NULL,                  -- 创建时间
  last_login_at TEXT                               -- 最近登录时间；长期未登录的账号可以回收权限
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_user ON sys_user(username); -- 账号唯一


-- ============================================================================
-- 自检 SQL（可选，建议部署后手工跑一次，确认流水与结存一致）
-- ----------------------------------------------------------------------------
-- ① 库存结存是否等于流水求和（正常应返回 0 行）：
--    SELECT p.id, p.name, p.stock_qty, IFNULL(SUM(l.qty_change),0) AS ledger_sum
--      FROM product p LEFT JOIN product_ledger l ON l.product_id = p.id
--     GROUP BY p.id HAVING p.stock_qty <> IFNULL(SUM(l.qty_change),0);
--
-- ② 单据优惠是否等于明细分摊之和（正常应返回 0 行）：
--    SELECT b.id, b.discount_cent, IFNULL(SUM(i.discount_cent),0) AS item_sum
--      FROM bill b LEFT JOIN bill_item i ON i.bill_id = b.id
--     GROUP BY b.id HAVING b.discount_cent <> IFNULL(SUM(i.discount_cent),0);
--
-- ③ 卡核销次数是否等于流水条数（正常应返回 0 行）：
--    SELECT c.id, c.card_no, c.used_paid, c.used_gift,
--           SUM(CASE WHEN l.from_gift=0 THEN l.qty ELSE 0 END) AS log_paid,
--           SUM(CASE WHEN l.from_gift=1 THEN l.qty ELSE 0 END) AS log_gift
--      FROM customer_card c LEFT JOIN card_consume_log l ON l.card_id = c.id
--     GROUP BY c.id
--    HAVING c.used_paid <> IFNULL(log_paid,0) OR c.used_gift <> IFNULL(log_gift,0);
-- ============================================================================


-- ============================================================================
-- v1.1 新增表（2026-10）：寄养 / 预约。全部幂等，v1.0 库直接升级。
-- ============================================================================

-- ============================================================================
-- 15. boarding_room 笼位
--     寄养的『库存』：编号唯一、类型对齐体型档（S/M/L/XL/CAT）。
-- ============================================================================
CREATE TABLE IF NOT EXISTS boarding_room (
  id               INTEGER PRIMARY KEY AUTOINCREMENT, -- 主键，自增
  code             TEXT    NOT NULL,                -- 笼位编号 R01/R02/C01…（C 开头为猫位）
  room_type        TEXT    NOT NULL,                -- 适用类型 S/M/L/XL/CAT
  daily_price_cent INTEGER NOT NULL DEFAULT 0,      -- 每晚挂牌价，单位分；寄养单可覆盖
  note             TEXT,                            -- 位置说明（靠窗/带跑笼）
  is_active        INTEGER NOT NULL DEFAULT 1,      -- 维修中可停用；在住的不受影响
  created_at       TEXT    NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_room_code ON boarding_room(code);

-- ============================================================================
-- 16. boarding_order 寄养单
--     状态机 BOOKED → CHECKED_IN → CHECKED_OUT / CANCELLED；按晚计费 BR-11。
-- ============================================================================
CREATE TABLE IF NOT EXISTS boarding_order (
  id               INTEGER PRIMARY KEY AUTOINCREMENT, -- 主键，自增
  order_no         TEXT    NOT NULL,                -- 单号 B + yyyyMMdd + 4位序号
  customer_id      INTEGER NOT NULL REFERENCES customer(id), -- 主人
  pet_id           INTEGER NOT NULL REFERENCES pet(id),      -- 被寄养的宠物
  room_id          INTEGER NOT NULL REFERENCES boarding_room(id), -- 笼位
  start_at         TEXT    NOT NULL,                -- 入住时间 'yyyy-MM-dd HH:mm'
  planned_end_on   TEXT    NOT NULL,                -- 预计离店日 'yyyy-MM-dd'
  end_at           TEXT,                            -- 实际退住时间；未退住 NULL
  days             INTEGER,                         -- 计费晚数（结算时落库，≥1）
  unit_price_cent  INTEGER NOT NULL,                -- 实际每晚价（下单时快照）
  deposit_cent     INTEGER NOT NULL DEFAULT 0,      -- 押金，仅记账展示，不进营收
  status           TEXT    NOT NULL DEFAULT 'BOOKED', -- BOOKED/CHECKED_IN/CHECKED_OUT/CANCELLED
  bill_id          INTEGER,                         -- 退住结算生成的 bill.id
  remark           TEXT,
  created_at       TEXT    NOT NULL,
  created_by       TEXT
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_boarding_no ON boarding_order(order_no);
CREATE INDEX IF NOT EXISTS idx_bo_room_time ON boarding_order(room_id, start_at); -- 笼位冲突检测
CREATE INDEX IF NOT EXISTS idx_bo_status    ON boarding_order(status);            -- 在店清单
CREATE INDEX IF NOT EXISTS idx_bo_customer  ON boarding_order(customer_id);

-- ============================================================================
-- 17. boarding_log 寄养每日记录
--     一天一条（唯一索引兜底），只增不改（BR-14）。
-- ============================================================================
CREATE TABLE IF NOT EXISTS boarding_log (
  id         INTEGER PRIMARY KEY AUTOINCREMENT,
  order_id   INTEGER NOT NULL REFERENCES boarding_order(id),
  log_date   TEXT    NOT NULL,              -- 记录的是哪一天（允许补录）
  fed_count  INTEGER NOT NULL DEFAULT 0,    -- 喂食次数
  walked     INTEGER NOT NULL DEFAULT 0,    -- 是否遛弯 0/1
  note       TEXT,                          -- 精神状态/大便/异常
  operator   TEXT,
  created_at TEXT    NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_blog_order_date ON boarding_log(order_id, log_date);

-- ============================================================================
-- 18. appointment 预约
--     纯日程记录：不占库存不收钱，转开单（DONE）才走收款（BR-12）。
-- ============================================================================
CREATE TABLE IF NOT EXISTS appointment (
  id           INTEGER PRIMARY KEY AUTOINCREMENT,
  customer_id  INTEGER NOT NULL REFERENCES customer(id),
  pet_id       INTEGER NOT NULL REFERENCES pet(id),
  service_id   INTEGER NOT NULL REFERENCES service_item(id),
  appoint_date TEXT    NOT NULL,             -- 预约日 'yyyy-MM-dd'
  slot         TEXT    NOT NULL,             -- 时段 'HH:mm'，30 分钟粒度
  size_tier    TEXT,                         -- 建单时体型档快照（估价用）
  status       TEXT    NOT NULL DEFAULT 'BOOKED', -- BOOKED/ARRIVED/DONE/CANCELLED/NO_SHOW
  bill_id      INTEGER,                      -- 转开单后的 bill.id
  remark       TEXT,
  created_at   TEXT    NOT NULL,
  created_by   TEXT
);
CREATE INDEX IF NOT EXISTS idx_appt_date ON appointment(appoint_date, slot);
CREATE INDEX IF NOT EXISTS idx_appt_customer ON appointment(customer_id);

INSERT OR IGNORE INTO store_config(k, v) VALUES ('slot_capacity', '3');


-- ============================================================================
-- v1.2 新增表（2026-10）：采购 / 批次 / 盘点 / 提成 / 客户经营 / 审计
-- 全部 IF NOT EXISTS，v1.0/v1.1 库启动即升级；存量表增列由 DataInitializer 完成。
-- ============================================================================

-- ============================================================================
-- 19. supplier 供应商
-- ============================================================================
CREATE TABLE IF NOT EXISTS supplier (
  id            INTEGER PRIMARY KEY AUTOINCREMENT,
  name          TEXT    NOT NULL,
  contact       TEXT,
  phone         TEXT,
  address       TEXT,
  main_category TEXT,
  remark        TEXT,
  is_active     INTEGER NOT NULL DEFAULT 1,
  created_at    TEXT    NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_supplier_name ON supplier(name);

-- ============================================================================
-- 20. purchase_order 采购入库单
--     DRAFT 草稿 → RECEIVED 已入库（终态）；草稿可 CANCELLED。
-- ============================================================================
CREATE TABLE IF NOT EXISTS purchase_order (
  id              INTEGER PRIMARY KEY AUTOINCREMENT,
  order_no        TEXT    NOT NULL,
  supplier_id     INTEGER NOT NULL REFERENCES supplier(id),
  status          TEXT    NOT NULL DEFAULT 'DRAFT',
  total_cost_cent INTEGER NOT NULL DEFAULT 0,
  item_count      INTEGER NOT NULL DEFAULT 0,
  received_at     TEXT,
  operator        TEXT,
  remark          TEXT,
  created_at      TEXT    NOT NULL,
  created_by      TEXT
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_purchase_no ON purchase_order(order_no);
CREATE INDEX IF NOT EXISTS idx_purchase_supplier ON purchase_order(supplier_id, created_at);
CREATE INDEX IF NOT EXISTS idx_purchase_status ON purchase_order(status, created_at);

-- ============================================================================
-- 21. purchase_item 采购明细
-- ============================================================================
CREATE TABLE IF NOT EXISTS purchase_item (
  id             INTEGER PRIMARY KEY AUTOINCREMENT,
  purchase_id    INTEGER NOT NULL REFERENCES purchase_order(id),
  product_id     INTEGER NOT NULL REFERENCES product(id),
  qty            INTEGER NOT NULL,
  unit_cost_cent INTEGER NOT NULL,
  amount_cent    INTEGER NOT NULL,
  batch_no       TEXT,
  expire_on      TEXT,
  remark         TEXT
);
CREATE INDEX IF NOT EXISTS idx_purchase_item_pid ON purchase_item(purchase_id);

-- ============================================================================
-- 22. product_batch 商品批次
--     ★ qty_left 是余额表（可 UPDATE），但每次变动必须同时写 product_ledger；
--       所有批次 qty_left 之和必须等于 product.stock_qty（数据体检项 2）。
-- ============================================================================
CREATE TABLE IF NOT EXISTS product_batch (
  id               INTEGER PRIMARY KEY AUTOINCREMENT,
  product_id       INTEGER NOT NULL REFERENCES product(id),
  purchase_item_id INTEGER,
  batch_no         TEXT,
  expire_on        TEXT,
  qty_in           INTEGER NOT NULL,
  qty_left         INTEGER NOT NULL,
  unit_cost_cent   INTEGER NOT NULL DEFAULT 0,
  source           TEXT    NOT NULL DEFAULT 'PURCHASE',
                   -- PURCHASE 采购 / OPENING 期初 / STOCK_TAKE 盘盈 / RETURN 反结账回补
  status           TEXT    NOT NULL DEFAULT 'NORMAL',
                   -- NORMAL / EXHAUSTED / EXPIRED / VOID
  received_at      TEXT    NOT NULL,
  created_at       TEXT    NOT NULL,
  CHECK (qty_left >= 0 AND (qty_left <= qty_in OR source = 'STOCK_TAKE'))
);
CREATE INDEX IF NOT EXISTS idx_batch_fefo ON product_batch(product_id, status, expire_on, received_at);
CREATE INDEX IF NOT EXISTS idx_batch_expire ON product_batch(expire_on, status);

-- ============================================================================
-- 23. stock_take 盘点单
-- ============================================================================
CREATE TABLE IF NOT EXISTS stock_take (
  id               INTEGER PRIMARY KEY AUTOINCREMENT,
  take_no          TEXT    NOT NULL,
  scope            TEXT    NOT NULL DEFAULT 'ALL',
  scope_value      TEXT,
  status           TEXT    NOT NULL DEFAULT 'COUNTING',
  item_count       INTEGER NOT NULL DEFAULT 0,
  diff_count       INTEGER NOT NULL DEFAULT 0,
  diff_amount_cent INTEGER NOT NULL DEFAULT 0,
  started_at       TEXT    NOT NULL,
  confirmed_at     TEXT,
  operator         TEXT,
  remark           TEXT,
  created_at       TEXT    NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_take_no ON stock_take(take_no);
CREATE INDEX IF NOT EXISTS idx_take_status ON stock_take(status, started_at);

-- ============================================================================
-- 24. stock_take_item 盘点明细
-- ============================================================================
CREATE TABLE IF NOT EXISTS stock_take_item (
  id          INTEGER PRIMARY KEY AUTOINCREMENT,
  take_id     INTEGER NOT NULL REFERENCES stock_take(id),
  product_id  INTEGER NOT NULL REFERENCES product(id),
  system_qty  INTEGER NOT NULL,
  counted_qty INTEGER,
  diff_qty    INTEGER,
  cost_cent   INTEGER NOT NULL DEFAULT 0,
  reason      TEXT,
  remark      TEXT
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_take_item ON stock_take_item(take_id, product_id);

-- ============================================================================
-- 25. staff 员工（与登录账号解耦：美容师可以没有系统账号）
-- ============================================================================
CREATE TABLE IF NOT EXISTS staff (
  id         INTEGER PRIMARY KEY AUTOINCREMENT,
  name       TEXT    NOT NULL,
  role       TEXT    NOT NULL DEFAULT 'GROOMER', -- GROOMER/ASSISTANT/CASHIER/MANAGER
  phone      TEXT,
  hire_on    TEXT,
  leave_on   TEXT,
  is_active  INTEGER NOT NULL DEFAULT 1,
  created_at TEXT    NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_staff_active ON staff(is_active, role);

-- ============================================================================
-- 26. commission_rule 提成规则（比例用万分比整数，全库无浮点）
-- ============================================================================
CREATE TABLE IF NOT EXISTS commission_rule (
  id             INTEGER PRIMARY KEY AUTOINCREMENT,
  scope          TEXT    NOT NULL,             -- SERVICE/PRODUCT/BOARDING/CARD/ALL
  ref_id         INTEGER,
  staff_role     TEXT,
  ratio_bp       INTEGER NOT NULL DEFAULT 0,   -- 1500 = 15%
  fixed_cent     INTEGER NOT NULL DEFAULT 0,
  effective_from TEXT    NOT NULL,
  effective_to   TEXT,
  is_active      INTEGER NOT NULL DEFAULT 1,
  remark         TEXT,
  created_at     TEXT    NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_rule_scope ON commission_rule(scope, ref_id, is_active);

-- ============================================================================
-- 27. commission_record 提成记录（只增；反结账写负数冲销）
-- ============================================================================
CREATE TABLE IF NOT EXISTS commission_record (
  id            INTEGER PRIMARY KEY AUTOINCREMENT,
  staff_id      INTEGER NOT NULL REFERENCES staff(id),
  bill_id       INTEGER NOT NULL,
  bill_item_id  INTEGER,
  biz_type      TEXT    NOT NULL,
  base_cent     INTEGER NOT NULL,
  ratio_bp      INTEGER NOT NULL DEFAULT 0,
  fixed_cent    INTEGER NOT NULL DEFAULT 0,
  amount_cent   INTEGER NOT NULL,
  business_date TEXT    NOT NULL,
  settle_id     INTEGER,
  reverse_of_id INTEGER,
  remark        TEXT,
  created_at    TEXT    NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_comm_staff_date ON commission_record(staff_id, business_date);
CREATE INDEX IF NOT EXISTS idx_comm_settle ON commission_record(settle_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_comm_item
  ON commission_record(bill_item_id, staff_id, biz_type, IFNULL(reverse_of_id, 0))
  WHERE bill_item_id IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_comm_bill_card
  ON commission_record(bill_id, staff_id, biz_type, IFNULL(reverse_of_id, 0))
  WHERE biz_type = 'CARD';

-- ============================================================================
-- 28. commission_settle 提成结算单
-- ============================================================================
CREATE TABLE IF NOT EXISTS commission_settle (
  id           INTEGER PRIMARY KEY AUTOINCREMENT,
  settle_no    TEXT    NOT NULL,
  month        TEXT    NOT NULL,
  staff_id     INTEGER NOT NULL REFERENCES staff(id),
  total_cent   INTEGER NOT NULL DEFAULT 0,
  record_count INTEGER NOT NULL DEFAULT 0,
  status       TEXT    NOT NULL DEFAULT 'DRAFT',
  confirmed_at TEXT,
  operator     TEXT,
  remark       TEXT,
  created_at   TEXT    NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_settle_staff_month ON commission_settle(staff_id, month);

-- ============================================================================
-- 29. pet_tag 宠物标签（凶猛/怕吹风/老年…）
-- ============================================================================
CREATE TABLE IF NOT EXISTS pet_tag (
  id         INTEGER PRIMARY KEY AUTOINCREMENT,
  pet_id     INTEGER NOT NULL REFERENCES pet(id),
  tag_code   TEXT    NOT NULL,
  note       TEXT,
  created_at TEXT    NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_pet_tag ON pet_tag(pet_id, tag_code);

-- ============================================================================
-- 30. customer_tag 客户标签
-- ============================================================================
CREATE TABLE IF NOT EXISTS customer_tag (
  id          INTEGER PRIMARY KEY AUTOINCREMENT,
  customer_id INTEGER NOT NULL REFERENCES customer(id),
  tag_code    TEXT    NOT NULL,
  note        TEXT,
  created_at  TEXT    NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_customer_tag ON customer_tag(customer_id, tag_code);

-- ============================================================================
-- 31. follow_up_task 回访任务（唯一索引防重复生成，表达式索引兜住 pet_id 为空）
-- ============================================================================
CREATE TABLE IF NOT EXISTS follow_up_task (
  id          INTEGER PRIMARY KEY AUTOINCREMENT,
  customer_id INTEGER NOT NULL REFERENCES customer(id),
  pet_id      INTEGER,
  type        TEXT    NOT NULL,
  due_on      TEXT    NOT NULL,
  status      TEXT    NOT NULL DEFAULT 'PENDING',
  source_type TEXT,
  source_id   INTEGER,
  note        TEXT,
  result      TEXT,
  operator    TEXT,
  done_at     TEXT,
  created_at  TEXT    NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_follow_up
  ON follow_up_task(customer_id, IFNULL(pet_id, 0), type, due_on);
CREATE INDEX IF NOT EXISTS idx_follow_up_due ON follow_up_task(status, due_on);

-- ============================================================================
-- 32. contact_log 触达记录
-- ============================================================================
CREATE TABLE IF NOT EXISTS contact_log (
  id             INTEGER PRIMARY KEY AUTOINCREMENT,
  customer_id    INTEGER NOT NULL REFERENCES customer(id),
  pet_id         INTEGER,
  task_id        INTEGER,
  channel        TEXT    NOT NULL,
  result         TEXT    NOT NULL,
  content        TEXT,
  next_follow_on TEXT,
  operator       TEXT,
  created_at     TEXT    NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_contact_customer ON contact_log(customer_id, created_at);

-- ============================================================================
-- 33. audit_log 操作审计
-- ============================================================================
CREATE TABLE IF NOT EXISTS audit_log (
  id          INTEGER PRIMARY KEY AUTOINCREMENT,
  action      TEXT    NOT NULL,
  target_type TEXT,
  target_id   INTEGER,
  before_json TEXT,
  after_json  TEXT,
  operator    TEXT,
  operator_id INTEGER,
  ip          TEXT,
  created_at  TEXT    NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_audit_time ON audit_log(created_at);
CREATE INDEX IF NOT EXISTS idx_audit_target ON audit_log(target_type, target_id);

INSERT OR IGNORE INTO store_config(k, v) VALUES ('inventory_batch_enabled', '1');
INSERT OR IGNORE INTO store_config(k, v) VALUES ('recall_days', '90');
INSERT OR IGNORE INTO store_config(k, v) VALUES ('follow_up_days', '3');
INSERT OR IGNORE INTO store_config(k, v) VALUES ('receipt_width', '58');
INSERT OR IGNORE INTO store_config(k, v) VALUES ('replenish_days', '7');
