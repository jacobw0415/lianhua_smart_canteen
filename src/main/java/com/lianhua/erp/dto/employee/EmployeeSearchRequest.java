package com.lianhua.erp.dto.employee;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 🔍 員工搜尋參數 Request DTO
 * 所有欄位皆為「可選」，支援模糊搜尋
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "員工搜尋條件")
public class EmployeeSearchRequest {

    @Schema(description = "員工姓名（支援模糊搜尋）", example = "王小明")
    private String fullName;

    @Schema(description = "職位（支援模糊搜尋）", example = "廚師")
    private String position;

    @Schema(description = "狀態（精確搜尋）", 
            example = "ACTIVE",
            allowableValues = {"ACTIVE", "INACTIVE"})
    private String status;
}

