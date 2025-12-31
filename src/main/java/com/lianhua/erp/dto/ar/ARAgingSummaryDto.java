package com.lianhua.erp.dto.ar;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "📌 應收帳款彙總（依客戶彙總）")
public class ARAgingSummaryDto {

    @Schema(description = "React-Admin 專用 id（= customerId）", example = "12")
    private Long id;

    @Schema(description = "客戶 ID", example = "12")
    private Long customerId;

    @Schema(description = "客戶名稱", example = "立安餐飲")
    private String customerName;

    @Schema(description = "帳齡 0–30 天金額", example = "15000.00")
    private BigDecimal aging0to30;

    @Schema(description = "帳齡 31–60 天金額", example = "8000.00")
    private BigDecimal aging31to60;

    @Schema(description = "帳齡 60 天以上金額", example = "12000.00")
    private BigDecimal aging60plus;

    @Schema(description = "應收總額（所有帳齡加總）", example = "35000.00")
    private BigDecimal totalAmount;

    @Schema(description = "已收款總額", example = "10000.00")
    private BigDecimal receivedAmount;

    @Schema(description = "未收款總額（totalAmount - receivedAmount）", example = "25000.00")
    private BigDecimal balance;
}

