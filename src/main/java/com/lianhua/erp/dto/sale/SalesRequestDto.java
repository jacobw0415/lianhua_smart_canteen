package com.lianhua.erp.dto.sale;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Schema(description = "銷售資料請求 DTO")
public class SalesRequestDto {
    
    @Schema(description = "銷售日期", example = "2025-10-16")
    private LocalDate saleDate;
    
    @Schema(description = "商品 ID", example = "3")
    private Long productId;
    
    @Schema(description = "銷售數量", example = "10")
    private Integer qty;
    
    @Schema(
            description = "銷售總金額（可省略，系統會依照商品單價 × 數量自動計算）",
            example = "750.00",
            nullable = true
    )
    private BigDecimal amount;
    
    @Schema(description = "付款方式", example = "CASH")
    private String payMethod;
}
