package com.lianhua.erp.dto.expense;

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
public class ExpenseCategoryRequestDto {
    
    @NotBlank
    @Schema(description = "費用類別名稱", example = "水電費")
    private String name;
    
    @Schema(description = "系統自動生成之會計科目代碼（EXP-001 形式）", example = "EXP-001", accessMode = Schema.AccessMode.READ_ONLY)
    private String accountCode;
    
    @Schema(description = "費用說明或備註", example = "包含電費與自來水費用")
    private String description;
    
    @Schema(description = "是否啟用此類別", example = "true")
    private Boolean active = true;
    
    @Schema(description = "上層類別 ID（可選）", example = "1")
    private Long parentId;
}
