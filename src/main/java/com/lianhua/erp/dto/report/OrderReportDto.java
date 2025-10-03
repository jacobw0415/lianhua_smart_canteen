package com.lianhua.erp.dto.report;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderReportDto {
    private Long customerId;
    private String customerName;
    private String period;        // 例如 "2025" 或 "2025-01"
    private String billingCycle;  // 僅應收帳款用
    private Long totalOrders;
    private Double totalAmount;
}
