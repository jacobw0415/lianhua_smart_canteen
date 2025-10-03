package com.lianhua.erp.dto.report;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "供應商月度對帳報表 DTO")
public class SupplierMonthlyReportDto {
    private String supplierName;
    private String month;
    private Double totalPurchase;
    private Double totalPayment;
    private Double balanceDue;
}
