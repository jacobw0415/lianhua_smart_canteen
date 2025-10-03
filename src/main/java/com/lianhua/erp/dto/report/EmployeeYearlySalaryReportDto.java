package com.lianhua.erp.dto.report;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "員工年度薪資報表 DTO")
public class EmployeeYearlySalaryReportDto {
    private Long employeeId;
    private String fullName;
    private Integer year;
    private Double totalSalary;
}
