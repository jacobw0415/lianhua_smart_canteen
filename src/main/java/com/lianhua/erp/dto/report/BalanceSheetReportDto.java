package com.lianhua.erp.dto.report;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "資產負債表回應 DTO")
public class BalanceSheetReportDto {

    @Schema(description = "會計期間 (YYYY-MM)")
    private String accountingPeriod;

    @Schema(description = "應收帳款（未收客戶款）")
    private BigDecimal accountsReceivable;

    @Schema(description = "現金及收款總額")
    private BigDecimal cash;

    @Schema(description = "應付帳款（未付供應商款）")
    private BigDecimal accountsPayable;

    @Schema(description = "總資產")
    private BigDecimal totalAssets;

    @Schema(description = "總負債")
    private BigDecimal totalLiabilities;

    @Schema(description = "業主權益（淨值）")
    private BigDecimal equity;
}
