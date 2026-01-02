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
    
    @NotBlank(message = "員工全名不可為空")
    @Schema(description = "員工全名", example = "王小明")
    private String fullName;
    
    @NotBlank(message = "職位不可為空")
    @Schema(description = "職位", example = "廚師")
    private String position;
    
    @NotNull(message = "薪資不可為空")
    @DecimalMin(value = "0.00", message = "薪資不得為負數")
    @Schema(description = "薪資", example = "32000.00")
    private BigDecimal salary;
    
    @NotNull(message = "聘用日期不可為空")
    @Schema(description = "聘用日期", example = "2024-05-01")
    private LocalDate hireDate;
    
    @Pattern(regexp = "^(ACTIVE|INACTIVE)$", message = "狀態必須為 ACTIVE 或 INACTIVE")
    @Schema(description = "狀態", example = "ACTIVE", allowableValues = {"ACTIVE", "INACTIVE"})
    private String status;
}
