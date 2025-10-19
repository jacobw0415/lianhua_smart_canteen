package com.lianhua.erp.dto.employee;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 員工資料回應 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "員工資料回應 DTO")
public class EmployeeResponseDto {
    
    @Schema(description = "員工 ID", example = "1")
    private Long id;
    
    @Schema(description = "員工全名", example = "王小明")
    private String fullName;
    
    @Schema(description = "職位", example = "廚師")
    private String position;
    
    @Schema(description = "薪資金額（單位：新台幣）", example = "32000.00")
    private BigDecimal salary;
    
    @Schema(description = "聘用日期", example = "2024-05-01")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate hireDate;
    
    @Schema(description = "員工狀態（ACTIVE：在職；INACTIVE：離職）", example = "ACTIVE")
    private String status;
    
    @Schema(description = "建立時間")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Taipei")
    private LocalDateTime createdAt;
    
    @Schema(description = "最後更新時間")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Taipei")
    private LocalDateTime updatedAt;
}
