package com.lianhua.erp.dto.order;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.lianhua.erp.domain.OrderStatus;
import com.lianhua.erp.dto.orderItem.OrderItemRequestDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "訂單建立／更新請求 DTO")
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderRequestDto {

    @NotNull
    @Schema(description = "客戶 ID", example = "1")
    private Long customerId;

    /**
     * 建單時允許：
     * - PENDING
     * - CONFIRMED
     *
     * 禁止：
     * - DELIVERED
     * - CANCELLED
     * - paymentStatus（不存在）
     */
    @NotNull
    @Schema(description = "訂單業務狀態（僅允許 PENDING / CONFIRMED）", example = "PENDING")
    private OrderStatus orderStatus;

    @NotNull
    @Schema(description = "訂單日期", example = "2025-10-26")
    private LocalDate orderDate;

    @NotNull
    @Schema(description = "交貨日期", example = "2025-10-28")
    private LocalDate deliveryDate;

    @Schema(description = "備註", example = "每日午餐配送")
    private String note;
    
    @NotEmpty
    @Schema(description = "訂單明細清單")
    private List<OrderItemRequestDto> items;
}
