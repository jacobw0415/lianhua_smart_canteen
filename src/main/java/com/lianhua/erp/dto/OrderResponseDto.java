package com.lianhua.erp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "訂單回應 DTO（含客戶與產品資訊）")
public class OrderResponseDto {
    @Schema(description = "訂單 ID", example = "1001")
    private Long id;

    @Schema(description = "訂單日期", example = "2025-10-01")
    private String orderDate;

    @Schema(description = "送餐日期", example = "2025-10-02")
    private String deliveryDate;

    @Schema(description = "狀態", example = "CONFIRMED")
    private String status;

    @Schema(description = "總金額", example = "8500.00")
    private Double totalAmount;

    @Schema(description = "備註", example = "週會午餐訂單")
    private String note;

    @Schema(description = "客戶資訊")
    private OrderCustomerDto customer;

    @Schema(description = "訂單明細")
    private List<OrderItemDetailDto> items;
}
