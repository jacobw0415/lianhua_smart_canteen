package com.lianhua.erp.dto.report;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OperatingCostCategoryDto {
    private String category;
    private Double totalAmount;
}
