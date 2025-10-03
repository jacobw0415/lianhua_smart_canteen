package com.lianhua.erp.dto.report;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "供應商應付帳款報表 DTO")
public class SupplierBalanceReportDto {
    private Long supplierId;
    private String supplierName;
    private Double totalPurchase;
    private Double totalPayment;
    private Double balanceDue;
}
