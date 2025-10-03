package com.lianhua.erp.dto.report;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "採購付款狀態報表 DTO")
public class PurchasePaymentStatusDto {
    private Long purchaseId;
    private String supplierName;
    private String purchaseDate;
    private Double purchaseAmount;
    private Double paidAmount;
    private Double unpaidAmount;
    private String status;
}

