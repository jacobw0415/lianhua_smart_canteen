package com.lianhua.erp.dto.ap;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "📌 供應商逐筆進貨應付帳款明細（AP Detail）")
public class APAgingPurchaseDetailDto {

    @Schema(description = "React-Admin 用 id（= purchaseId）")
    private Long id;

    @Schema(description = "進貨單 ID", example = "105")
    private Long purchaseId;

    @Schema(description = "進貨日期", example = "2025-01-08")
    private LocalDate purchaseDate;

    @Schema(description = "該筆進貨金額（total amount）", example = "5000.00")
    private BigDecimal totalAmount;

    @Schema(description = "已付款金額", example = "3000.00")
    private BigDecimal paidAmount;

    @Schema(description = "未付款金額（amount - paidAmount）", example = "2000.00")
    private BigDecimal balance;

    @Schema(description = "帳齡區間（以到期日為基準）", example = "31–60")
    private String agingBucket;

    @Schema(description = "付款狀態：PENDING / PARTIAL / PAID", example = "PARTIAL")
    private String status;
}