package com.lianhua.erp.dto.employee;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "建立或更新員工資料的請求 DTO")
public class EmployeeRequestDto {
    
    @NotBlank
    @Schema(description = "員工全名", example = "王小明")
    private String fullName;
    
    @Schema(description = "職位", example = "廚師")
    private String position;
    
    @DecimalMin(value = "0.00", message = "薪資不得為負數")
    @Schema(description = "薪資", example = "32000.00")
    private BigDecimal salary;
    
    @Schema(description = "聘用日期", example = "2024-05-01")
    private LocalDate hireDate;
    
    @Schema(description = "狀態", example = "ACTIVE")
    private String status;
}
