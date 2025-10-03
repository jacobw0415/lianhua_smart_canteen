package com.lianhua.erp.dto.report;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OperatingCostReportDto {
    private String month;
    private Double totalOperatingExpense;
}
