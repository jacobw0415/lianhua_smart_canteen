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
    @Schema(description = "數量", example = "10")
    private Integer qty;

    @NotNull
    @DecimalMin(value = "0.00", inclusive = false)
    @Schema(description = "單價", example = "50.00")
    private BigDecimal unitPrice;

    @Schema(description = "折扣金額", example = "0.00")
    private BigDecimal discount = BigDecimal.ZERO;

    @Schema(description = "稅額", example = "0.00")
    private BigDecimal tax = BigDecimal.ZERO;

    @Schema(description = "備註", example = "午餐便當")
    private String note;
}
