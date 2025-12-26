package com.lianhua.erp.domain;

/**
 * 付款單記錄狀態（用於 Payment 實體）
 */
public enum PaymentRecordStatus {
    ACTIVE,   // 正常付款（已生效）
    VOIDED    // 已作廢
}

