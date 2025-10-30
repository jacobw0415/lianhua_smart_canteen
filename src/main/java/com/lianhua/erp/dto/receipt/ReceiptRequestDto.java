package com.lianhua.erp.dto.receipt;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "收款建立／更新請求 DTO（金額自動由訂單帶入）")
public class ReceiptRequestDto {
    
    @NotNull
    @Schema(description = "訂單 ID", example = "1")
    private Long orderId;
    
    @Schema(description = "收款日期", example = "2025-10-30")
    private LocalDate receivedDate;
    
    @Schema(description = "收款方式", example = "TRANSFER", allowableValues = {"CASH", "TRANSFER", "CARD", "CHECK"})
    private String method;
    
    @Schema(description = "參考號碼（例如匯款編號或支票號）", example = "TRX-20251030-001")
    private String referenceNo;
    
    @Schema(description = "備註", example = "自動對帳入帳")
    private String note;
}
