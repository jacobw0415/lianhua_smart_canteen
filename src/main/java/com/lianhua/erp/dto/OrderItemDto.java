package com.lianhua.erp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "訂單明細 DTO")
public class OrderItemDto {
    @Schema(description = "明細 ID", example = "1")
    private Long id;

    @Schema(description = "訂單 ID", example = "1001")
    private Long orderId;

    @Schema(description = "商品 ID", example = "501")
    private Long productId;

    @Schema(description = "數量", example = "100")
    private Integer qty;

    @Schema(description = "單價", example = "85.00")
    private Double unitPrice;

    @Schema(description = "小計", example = "8500.00")
    private Double subtotal;

    @Schema(description = "折扣", example = "0")
    private Double discount;

    @Schema(description = "稅額", example = "0")
    private Double tax;

    @Schema(description = "備註", example = "加飯")
    private String note;
}
