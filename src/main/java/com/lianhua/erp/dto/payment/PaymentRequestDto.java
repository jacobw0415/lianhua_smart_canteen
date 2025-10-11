package com.lianhua.erp.dto.payment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "建立或更新付款紀錄的請求 DTO")
public class PaymentRequestDto {
    
    @NotNull(message = "付款金額不可為空")
    @DecimalMin(value = "0.01", message = "付款金額需大於 0")
    @Schema(description = "付款金額", example = "500.00")
    private BigDecimal amount;
    
    @NotNull(message = "付款日期不可為空")
    @Schema(description = "付款日期", example = "2025-10-11")
    private LocalDate payDate;
    
    @NotBlank(message = "付款方式不可為空")
    @Schema(description = "付款方式", example = "CASH", allowableValues = {"CASH", "TRANSFER", "CARD", "CHECK"})
    private String method;
    
    @Schema(description = "備註說明", example = "部分付款")
    private String note;
}
