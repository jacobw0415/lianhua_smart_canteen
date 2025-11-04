package com.lianhua.erp.dto.report;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;

/**
 * 應付帳齡報表 DTO
 * 顯示各供應商未付款金額與逾期天數。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "應付帳齡報表回應 DTO")
public class APAgingReportDto {

    @Schema(description = "供應商名稱", example = "安興蔬果行")
    private String supplierName;

    @Schema(description = "採購單 ID", example = "102")
    private Long purchaseId;

    @Schema(description = "採購日期", example = "2025-10-01")
    private String purchaseDate;

    @Schema(description = "採購總金額", example = "15000.00")
    private BigDecimal totalAmount;

    @Schema(description = "已付款金額", example = "5000.00")
    private BigDecimal paidAmount;

    @Schema(description = "未付款餘額", example = "10000.00")
    private BigDecimal balance;

    @Schema(description = "逾期天數", example = "45")
    private Integer daysOverdue;

    @Schema(description = "帳齡區間", example = "31–60天")
    private String agingBucket;
}

