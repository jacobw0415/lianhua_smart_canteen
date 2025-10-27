package com.lianhua.erp.dto.order;

import com.lianhua.erp.dto.orderItem.OrderItemResponseDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "訂單回應 DTO（含明細與客戶資訊）")
public class OrderResponseDto {

    @Schema(description = "訂單 ID")
    private Long id;

    @Schema(description = "客戶 ID")
    private Long customerId;

    @Schema(description = "客戶名稱")
    private String customerName;

    @Schema(description = "訂單日期")
    private LocalDate orderDate;

    @Schema(description = "交貨日期")
    private LocalDate deliveryDate;

    @Schema(description = "訂單狀態")
    private String status;

    @Schema(description = "會計期間")
    private String accountingPeriod;

    @Schema(description = "訂單總金額")
    private BigDecimal totalAmount;

    @Schema(description = "備註")
    private String note;

    @Schema(description = "明細列表")
    private List<OrderItemResponseDto> items;

    @Schema(description = "建立時間")
    private LocalDateTime createdAt;

    @Schema(description = "更新時間")
    private LocalDateTime updatedAt;
}
