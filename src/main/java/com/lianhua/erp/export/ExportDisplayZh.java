package com.lianhua.erp.export;

import java.util.Locale;

/**
 * 匯出 xlsx／csv 時將後端慣用之英文 enum 字串轉成中文顯示。
 * 僅影響匯出列，不變更 API JSON 或資料庫存值。
 */
public final class ExportDisplayZh {

    private ExportDisplayZh() {
    }

    private static String key(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        return raw.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 訂單處理狀態：PENDING / CONFIRMED / DELIVERED / CANCELLED
     */
    public static String orderLifecycle(String raw) {
        return switch (key(raw)) {
            case "PENDING" -> "待確認";
            case "CONFIRMED" -> "已確認";
            case "DELIVERED" -> "已交貨";
            case "CANCELLED" -> "已取消";
            case "" -> "";
            default -> raw.trim();
        };
    }

    /**
     * 訂單收款狀態：UNPAID / PAID
     */
    public static String orderCollection(String raw) {
        return switch (key(raw)) {
            case "UNPAID" -> "未收款";
            case "PAID" -> "已收款";
            case "" -> "";
            default -> raw.trim();
        };
    }

    /**
     * 進貨單付款進度：PENDING / PARTIAL / PAID
     */
    public static String purchasePayment(String raw) {
        return switch (key(raw)) {
            case "PENDING" -> "待付款";
            case "PARTIAL" -> "部分付款";
            case "PAID" -> "已付清";
            case "" -> "";
            default -> raw.trim();
        };
    }

    /**
     * 支出／付款紀錄／收款紀錄等：ACTIVE（正常）／VOIDED（已作廢）
     */
    public static String recordActiveVoid(String raw) {
        return switch (key(raw)) {
            case "ACTIVE" -> "正常";
            case "VOIDED" -> "已作廢";
            case "" -> "";
            default -> raw.trim();
        };
    }

    /**
     * 員工狀態：ACTIVE / INACTIVE
     */
    public static String employeeActive(String raw) {
        return switch (key(raw)) {
            case "ACTIVE" -> "啟用";
            case "INACTIVE" -> "停用";
            case "" -> "";
            default -> raw.trim();
        };
    }

    /**
     * 付款方式（進貨付款、訂單收款等）：CASH / TRANSFER / CARD / CHECK
     */
    public static String paymentMethod(String raw) {
        return switch (key(raw)) {
            case "CASH" -> "現金";
            case "TRANSFER" -> "轉帳";
            case "CARD" -> "刷卡";
            case "CHECK" -> "支票";
            case "" -> "";
            default -> raw.trim();
        };
    }

    /**
     * 零售銷售付款方式：CASH / CARD / MOBILE
     */
    public static String retailPayMethod(String raw) {
        return switch (key(raw)) {
            case "CASH" -> "現金";
            case "CARD" -> "刷卡";
            case "MOBILE" -> "行動支付";
            case "" -> "";
            default -> raw.trim();
        };
    }

    /**
     * 帳單週期：WEEKLY / BIWEEKLY / MONTHLY
     */
    public static String billingCycle(String raw) {
        return switch (key(raw)) {
            case "WEEKLY" -> "每週";
            case "BIWEEKLY" -> "每兩週";
            case "MONTHLY" -> "每月";
            case "" -> "";
            default -> raw.trim();
        };
    }
}
