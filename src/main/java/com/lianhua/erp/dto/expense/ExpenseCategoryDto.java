package com.lianhua.erp.dto.expense;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

/**
 * 用於回傳費用類別詳細資訊
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "費用類別資料傳輸物件（回傳用）")
public class ExpenseCategoryDto {
    
    @Schema(description = "費用類別 ID", example = "1")
    private Long id;
    
    @Schema(description = "費用類別名稱", example = "食材費")
    private String name;
    
    @Schema(description = "系統自動產生之費用科目代碼（唯讀）", example = "EXP-003", readOnly = true)
    private String accountCode;
    
    @Schema(description = "費用說明或備註", example = "每日採購蔬菜、水果支出")
    private String description;
    
    @Schema(description = "是否啟用此費用類別", example = "true")
    private Boolean active;
    
    @Schema(description = "上層費用類別 ID（若為階層分類）", example = "null")
    private Long parentId;
    
    @Schema(description = "上層費用類別名稱", example = "原物料支出")
    private String parentName;
}
