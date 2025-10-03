package com.lianhua.erp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "付款 DTO")
public class PaymentDto {
    @Schema(description = "付款 ID", example = "3001")
    private Long id;

    @Schema(description = "對應採購 ID", example = "2001")
    private Long purchaseId;

    @Schema(description = "金額", example = "1000.00")
    private Double amount;

    @Schema(description = "付款日期", example = "2025-10-05")
    private String payDate;

    @Schema(description = "付款方式", example = "TRANSFER")
    private String method;

    @Schema(description = "備註", example = "匯款至國泰銀行")
    private String note;
}

