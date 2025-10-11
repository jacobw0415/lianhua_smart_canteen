package com.lianhua.erp.dto.supplier;

import com.lianhua.erp.domin.Supplier.BillingCycle;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "供應商請求物件（建立或更新用）")
public class SupplierRequestDto {
    
    @NotBlank
    @Schema(description = "供應商名稱", example = "蓮華蔬果供應行")
    private String name;
    
    @Schema(description = "聯絡人", example = "王先生")
    private String contact;
    
    @Schema(description = "電話", example = "0912-345-678")
    private String phone;
    
    @Schema(description = "帳單週期", example = "MONTHLY")
    private BillingCycle billingCycle = BillingCycle.MONTHLY;
    
    @Schema(description = "備註", example = "每月底結帳")
    private String note;
}
