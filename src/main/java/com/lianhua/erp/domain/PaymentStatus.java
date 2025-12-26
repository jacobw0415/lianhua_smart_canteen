package com.lianhua.erp.domain;

/**
 * 訂單付款狀態（用於 Order 實體）
 */
public enum PaymentStatus {
    UNPAID,   // 尚未收款
    PAID      // 已全額收款
}
