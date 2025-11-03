package com.lianhua.erp.dto.report;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;

/**
 * 應收帳齡報表 DTO
 * 顯示各客戶未收金額與逾期天數。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "應收帳齡報表回應 DTO")
public class ARAgingReportDto {

    @Schema(description = "客戶名稱", example = "立安餐飲")
    private String customerName;

    @Schema(description = "訂單編號", example = "1024")
    private Long orderId;

    @Schema(description = "訂單日期", example = "2025-09-20")
    private String orderDate;

    @Schema(description = "交貨日期", example = "2025-09-22")
    private String deliveryDate;

    @Schema(description = "訂單總金額", example = "15000.00")
    private BigDecimal totalAmount;

    @Schema(description = "已收金額", example = "10000.00")
    private BigDecimal receivedAmount;

    @Schema(description = "未收金額 (應收餘額)", example = "5000.00")
    private BigDecimal balance;

    @Schema(description = "逾期天數", example = "42")
    private Integer daysOverdue;

    @Schema(description = "帳齡區間 (0–30天、31–60天、61–90天、90天以上)")
    private String agingBucket;
}

