package com.lianhua.erp.dto.report;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 📊 應收帳款總表 DTO（按期間彙總）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "應收帳款總表（按會計期間彙總）")
public class ARSummaryReportDto {

    @Schema(description = "會計期間 (YYYY-MM 或 yyyy-MM-dd)", example = "2025-10")
    private String accountingPeriod;

    @Schema(description = "應收總額", example = "35000.00")
    private BigDecimal totalReceivable;

    @Schema(description = "已收金額", example = "12000.00")
    private BigDecimal totalReceived;

    @Schema(description = "未收金額（餘額）", example = "23000.00")
    private BigDecimal totalOutstanding;
}
