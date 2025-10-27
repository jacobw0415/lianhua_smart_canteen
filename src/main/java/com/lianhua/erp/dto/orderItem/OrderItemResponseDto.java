package com.lianhua.erp.dto.orderItem;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "訂單明細回應 DTO")
public class OrderItemResponseDto {

    @Schema(description = "明細 ID")
    private Long id;

    @Schema(description = "商品 ID")
    private Long productId;

    @Schema(description = "商品名稱")
    private String productName;

    @Schema(description = "數量")
    private Integer qty;

    @Schema(description = "單價")
    private BigDecimal unitPrice;

    @Schema(description = "折扣金額")
    private BigDecimal discount;

    @Schema(description = "稅額")
    private BigDecimal tax;

    @Schema(description = "小計")
    private BigDecimal subtotal;

    @Schema(description = "備註")
    private String note;

    @Schema(description = "建立時間")
    private LocalDateTime createdAt;
}
