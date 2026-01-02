package com.lianhua.erp.dto.expense;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "費用類別搜尋條件")
public class ExpenseCategorySearchRequest {
    
    @Schema(description = "費用類別名稱", example = "食材費")
    private String name;
    
    @Schema(description = "會計科目代碼", example = "EXP-001")
    private String accountCode;
    
    @Schema(description = "是否啟用", example = "true")
    private Boolean active;
}

