package com.lianhua.erp.dto.expense;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.lianhua.erp.domain.ExpenseFrequency;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * 用於建立或更新費用類別請求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "費用類別建立／更新請求 DTO")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExpenseCategoryRequestDto {
    
    @NotBlank(message = "費用類別名稱不可為空")
    @Schema(
        description = "費用類別名稱（必填，需唯一）", 
        example = "水電費",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String name;
    
    @Schema(description = "費用說明或備註（選填）", example = "包含電費與自來水費用")
    private String description;
    
    @Builder.Default
    @Schema(description = "是否啟用此類別（選填，預設為 true）", example = "true")
    private Boolean active = true;
    
    @Builder.Default
    @Schema(description = "是否為薪資類別（選填，預設為 false）", example = "false")
    private Boolean isSalary = false;
    
    @Builder.Default
    @Schema(description = "費用頻率類型（選填，預設為 DAILY）", 
            example = "MONTHLY",
            allowableValues = {"DAILY", "WEEKLY", "BIWEEKLY", "MONTHLY", "UNLIMITED"})
    private ExpenseFrequency frequencyType = ExpenseFrequency.DAILY;
    
    // 注意：accountCode 由系統自動生成，不需要在此 DTO 中提供
}
