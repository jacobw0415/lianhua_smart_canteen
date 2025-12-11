package com.lianhua.erp.dto.ap;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "📌 應付帳款彙總（依供應商彙總）")
public class APAgingSummaryDto {

    @Schema(description = "React-Admin 專用 id（= supplierId）", example = "12")
    private Long id;

    @Schema(description = "供應商 ID", example = "12")
    private Long supplierId;

    @Schema(description = "供應商名稱", example = "大樹農場")
    private String supplierName;

    @Schema(description = "帳齡 0–30 天金額", example = "15000.00")
    private BigDecimal aging0to30;

    @Schema(description = "帳齡 31–60 天金額", example = "8000.00")
    private BigDecimal aging31to60;

    @Schema(description = "帳齡 60 天以上金額", example = "12000.00")
    private BigDecimal aging60plus;

    @Schema(description = "應付總額（所有帳齡加總）", example = "35000.00")
    private BigDecimal totalAmount;

    @Schema(description = "已付款總額", example = "10000.00")
    private BigDecimal paidAmount;

    @Schema(description = "未付款總額（totalAmount - paidAmount）", example = "25000.00")
    private BigDecimal balance;
}
