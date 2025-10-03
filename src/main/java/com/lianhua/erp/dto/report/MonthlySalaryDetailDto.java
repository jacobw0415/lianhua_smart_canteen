package com.lianhua.erp.dto.report;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "當月薪資明細報表 DTO")
public class MonthlySalaryDetailDto {
    private Long employeeId;
    private String fullName;
    private String expenseDate;
    private Double salaryPaid;
    private String note;
}
