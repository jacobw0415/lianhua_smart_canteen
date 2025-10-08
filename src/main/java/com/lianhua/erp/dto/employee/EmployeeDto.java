package com.lianhua.erp.dto.employee;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "員工 DTO")
public class EmployeeDto {
    @Schema(description = "員工 ID", example = "1")
    private Long id;

    @Schema(description = "員工姓名", example = "王小明")
    private String fullName;

    @Schema(description = "職位", example = "廚房")
    private String position;

    @Schema(description = "薪水", example = "32000")
    private Double salary;

    @Schema(description = "入職日期", example = "2024-01-01")
    private String hireDate;

    @Schema(description = "狀態", example = "ACTIVE")
    private String status;
}
