package com.petshop.common;

/** 业务异常：继承 RuntimeException 保证事务默认回滚。 */
public class BizException extends RuntimeException {

    private final ErrorCode code;

    public BizException(ErrorCode code) {
        super(code.getMessage());
        this.code = code;
    }

    public BizException(ErrorCode code, String msg) {
        super(msg);
        this.code = code;
    }

    public ErrorCode getCode() {
        return code;
    }
}
