package com.lianhua.erp.dto.report;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSalesReportDto {
    private Long productId;
    private String productName;
    private Long totalQty;
    private Double totalSales;
}

