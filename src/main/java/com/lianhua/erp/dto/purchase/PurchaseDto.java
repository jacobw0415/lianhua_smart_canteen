package com.lianhua.erp.dto.purchase;

import com.lianhua.erp.dto.payment.PaymentDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "採購 DTO")
public class PurchaseDto {
    @Schema(description = "採購 ID", example = "2001")
    private Long id;

    @Schema(description = "供應商 ID", example = "10")
    private Long supplierId;

    @Schema(description = "進貨日期", example = "2025-10-01")
    private String purchaseDate;

    @Schema(description = "品項", example = "高麗菜")
    private String item;

    @Schema(description = "數量", example = "50")
    private Integer qty;

    @Schema(description = "單價", example = "20.5")
    private Double unitPrice;

    @Schema(description = "稅金", example = "0")
    private Double tax;

    @Schema(description = "狀態", example = "PENDING")
    private String status;

    @Schema(description = "付款紀錄清單")
    private List<PaymentDto> payments;
}

