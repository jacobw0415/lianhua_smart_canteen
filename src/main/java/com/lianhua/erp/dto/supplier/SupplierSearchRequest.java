package com.lianhua.erp.dto.supplier;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 🔍 供應商搜尋參數 Request DTO
 * 所有欄位皆為「可選」，支援模糊搜尋
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "供應商搜尋條件")
public class SupplierSearchRequest {

    @Schema(description = "供應商名稱（支援模糊搜尋）", example = "美和蔬品")
    private String supplierName;

    @Schema(description = "聯絡人姓名（支援模糊搜尋）", example = "王先生")
    private String contact;

    @Schema(description = "電話（支援模糊搜尋）", example = "0912")
    private String phone;

    @Schema(description = "結帳週期（精確搜尋）",
            example = "MONTHLY",
            allowableValues = {"DAILY", "WEEKLY", "BIWEEKLY", "MONTHLY"})
    private String billingCycle;

    @Schema(description = "備註內容（支援模糊搜尋）", example = "月底結帳")
    private String note;
}
