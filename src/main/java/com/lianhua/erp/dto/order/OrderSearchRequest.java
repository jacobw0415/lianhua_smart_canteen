package com.lianhua.erp.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "訂單查詢條件 DTO")
public class OrderSearchRequest {

    /* =====================================================
     * 🔍 基本識別條件
     * ===================================================== */

    @Schema(description = "訂單 ID（內部用，非主要搜尋）", example = "1")
    private Long id;

    @Schema(description = "訂單編號（模糊搜尋）", example = "SO-202510")
    private String orderNo;

    @Schema(description = "客戶 ID", example = "1001")
    private Long customerId;

    @Schema(description = "客戶名稱（模糊搜尋）", example = "聯華")
    private String customerName;

    /* =====================================================
     * 📅 日期區間條件
     * ===================================================== */

    @Schema(description = "訂單日期（起）", example = "2025-10-01")
    private LocalDate orderDateFrom;

    @Schema(description = "訂單日期（迄）", example = "2025-10-31")
    private LocalDate orderDateTo;

    @Schema(description = "交貨日期（起）", example = "2025-10-20")
    private LocalDate deliveryDateFrom;

    @Schema(description = "交貨日期（迄）", example = "2025-11-05")
    private LocalDate deliveryDateTo;

    /* =====================================================
     * 📌 訂單 / 收款狀態
     * ===================================================== */

    @Schema(description = "訂單狀態（order_status）", example = "CONFIRMED")
    private String orderStatus;

    @Schema(description = "收款狀態（payment_status）", example = "UNPAID")
    private String paymentStatus;

    @Schema(description = "會計期間（YYYY-MM）", example = "2025-10")
    private String accountingPeriod;

    /* =====================================================
     * 💰 金額區間
     * ===================================================== */

    @Schema(description = "訂單總金額（最小）", example = "10000")
    private BigDecimal totalAmountMin;

    @Schema(description = "訂單總金額（最大）", example = "50000")
    private BigDecimal totalAmountMax;

    /* =====================================================
     * 📝 其他
     * ===================================================== */

    @Schema(description = "備註（模糊搜尋）", example = "急件")
    private String note;
}
