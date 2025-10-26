package com.lianhua.erp.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "訂單建立／更新請求 DTO")
public class OrderRequestDto {
    
    @NotNull
    @Schema(description = "客戶 ID", example = "1")
    private Long customerId;
    
    @NotNull
    @Schema(description = "訂單日期", example = "2025-10-26")
    private LocalDate orderDate;
    
    @NotNull
    @Schema(description = "交貨日期", example = "2025-10-28")
    private LocalDate deliveryDate;
    
    @NotNull
    @DecimalMin(value = "0.00", inclusive = false)
    @Schema(description = "總金額", example = "15000.00")
    private BigDecimal totalAmount;
    
    @Schema(description = "會計期間", example = "2025-10")
    private String accountingPeriod;
    
    @Schema(description = "訂單狀態", example = "PENDING", allowableValues = {"PENDING", "CONFIRMED", "DELIVERED", "CANCELLED", "PAID"})
    private String status;
    
    @Schema(description = "備註", example = "由王小姐訂購")
    private String note;
}
