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
    PARAM_INVALID("参数不正确"),
    USER_EXISTS("该用户名已存在"),
    USER_LAST_ADMIN("系统至少需要保留一个管理员"),
    USER_DELETE_SELF("不能删除（停用）自己的账号"),
    DB_LOCKED("系统繁忙，请稍后重试"),
    BACKUP_FAILED("备份失败，请检查磁盘空间"),
    SYSTEM_ERROR("系统出错，请稍后重试");

    private final String message;

    ErrorCode(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
