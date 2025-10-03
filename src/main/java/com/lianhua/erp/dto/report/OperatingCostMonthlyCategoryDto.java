package com.lianhua.erp.dto.report;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OperatingCostMonthlyCategoryDto {
    private String month;
    private String category;
    private Double totalAmount;
}
