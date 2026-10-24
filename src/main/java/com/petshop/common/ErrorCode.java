package com.petshop.common;

/** 错误码枚举：中文提示给用户看，错误码只写日志。 */
public enum ErrorCode {
    LOGIN_FAILED("用户名或密码错误"),
    LOGIN_REQUIRED("请先登录"),
    NO_PERMISSION("没有权限执行此操作"),
    CUSTOMER_EXISTS("该手机号已存在客户档案"),
    CUSTOMER_IN_USE("该客户有历史单据，只能停用不能删除"),
    PET_REQUIRED("含服务项目时必须选择宠物"),
    PET_BIRTHDAY_INVALID("生日不能晚于今天"),
    CARD_NO_BALANCE("该卡次数已用完"),
    CARD_EXPIRED("该卡已过期，请先延期"),
    CARD_VOID("该卡已作废，无法使用"),
    CARD_NOT_FOUND("卡不存在"),
    CARD_NOT_MATCH_SERVICE("该卡不适用于此服务项目"),
    DISCOUNT_EXCEED_TOTAL("折扣金额不能大于商品总额"),
    STOCK_NOT_ENOUGH("库存不足"),
    BILL_NOT_TODAY("只能反结账当天的单据"),
    BILL_ALREADY_SETTLED("该单已结账，请勿重复提交"),
    BILL_NOT_FOUND("单据不存在"),
    BILL_CANCELLED("该单据已作废"),
    BILL_ALREADY_CANCELLED("该单已反结账，请刷新后查看"),
    LOGIN_LOCKED("登录失败次数过多，请 10 分钟后再试"),
    PARAM_INVALID("参数不正确"),
    USER_EXISTS("该用户名已存在"),
    USER_LAST_ADMIN("系统至少需要保留一个管理员"),
    USER_DELETE_SELF("不能删除（停用）自己的账号"),
    USER_DELETE_ADMIN("管理员账号不能删除，可改为停用"),
    DB_LOCKED("系统繁忙，请稍后重试"),
    BACKUP_FAILED("备份失败，请检查磁盘空间"),
    SYSTEM_ERROR("系统出错，请稍后重试"),
    // v1.1 寄养 / 预约
    ROOM_NOT_FOUND("笼位不存在或已停用"),
    ROOM_OCCUPIED("该笼位在所选时段已被占用"),
    VACCINE_EXPIRED("该宠物疫苗已过期（或无疫苗记录），不能办理入住"),
    BOARDING_NOT_IN("该寄养单尚未入住"),
    BOARDING_SETTLED("该寄养单已结算"),
    SLOT_FULL("该时段预约已满"),
    APPOINTMENT_STATE_INVALID("该预约当前状态不允许此操作"),
    APPOINTMENT_NOT_FOUND("预约不存在"),
    // v1.2 采购 / 批次 / 盘点
    SUPPLIER_NOT_FOUND("供应商不存在或已停用"),
    SUPPLIER_EXISTS("供应商名称已存在"),
    PURCHASE_NOT_FOUND("采购单不存在"),
    PURCHASE_ALREADY_RECEIVED("该采购单已入库，不能重复操作"),
    PURCHASE_EMPTY("采购单至少需要一行商品"),
    BATCH_NOT_FOUND("批次不存在"),
    BATCH_STOCK_NOT_ENOUGH("批次库存不足，请先做数据体检或盘点"),
    BATCH_EXPIRED("该批次已过期，不能出库"),
    STOCK_TAKE_NOT_FOUND("盘点单不存在"),
    STOCK_TAKE_ALREADY_CONFIRMED("该盘点单已确认，不能修改"),
    STOCK_TAKE_DUPLICATE("该商品已存在未确认的盘点单"),
    STOCK_TAKE_EMPTY("请至少录入一个实盘数量"),
    // v1.2 提成
    STAFF_NOT_FOUND("员工不存在或已离职"),
    COMMISSION_RULE_CONFLICT("提成规则存在重叠，请检查生效区间"),
    COMMISSION_ALREADY_SETTLED("该月提成已结算，不能重复结算"),
    FOLLOW_UP_NOT_FOUND("待办任务不存在或已处理"),
    // v1.2 运维
    RESTORE_FILE_INVALID("备份文件损坏或不是本系统数据"),
    MAINTENANCE_MODE("系统正在维护（数据恢复），请稍后再试");

    private final String message;

    ErrorCode(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
