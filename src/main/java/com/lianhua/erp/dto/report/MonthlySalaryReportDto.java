package com.lianhua.erp.dto.report;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "每月薪資支出報表 DTO")
public class MonthlySalaryReportDto {
    private String month;
    private Double totalSalaryExpense;
}

