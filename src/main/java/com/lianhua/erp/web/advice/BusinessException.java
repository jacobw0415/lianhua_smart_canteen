package com.lianhua.erp.web.advice;

public class BusinessException extends RuntimeException {

    private final String code;   // (可選) 錯誤代碼，用於大型 ERP 延伸

    public BusinessException(String message) {
        super(message);
        this.code = "BUSINESS_ERROR";
    }

    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
