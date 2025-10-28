package com.lianhua.erp.dto.orderItem;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "訂單明細建立／更新請求 DTO")
public class OrderItemRequestDto {
    
    @NotNull
    @Schema(description = "商品 ID", example = "5")
    private Long productId;
    
    @NotNull
    @Min(1)
    @Schema(description = "商品數量", example = "10")
    private Integer qty;
    
    @Schema(
            description = "單價（系統自動帶入，無需輸入）",
            accessMode = Schema.AccessMode.READ_ONLY,
            example = "50.00"
    )
    private BigDecimal unitPrice;  // ✅ 後端自動設定
    
    @Builder.Default
    @DecimalMin(value = "0.00", inclusive = true)
    @Digits(integer = 10, fraction = 2)
    @Schema(description = "折扣金額（可為 0）", example = "0.00")
    private BigDecimal discount = BigDecimal.ZERO;
    
    @Builder.Default
    @DecimalMin(value = "0.00", inclusive = true)
    @Digits(integer = 10, fraction = 2)
    @Schema(description = "稅額（可為 0）", example = "0.00")
    private BigDecimal tax = BigDecimal.ZERO;
    
    @Schema(description = "備註", example = "午餐便當")
    private String note;
    
    @Schema(
            description = "小計金額（系統自動計算：qty × unitPrice - discount + tax）",
            accessMode = Schema.AccessMode.READ_ONLY,
            example = "500.00"
    )
    private BigDecimal subtotal; // ✅ 後端計算
}
