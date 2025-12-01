package com.lianhua.erp.dto.supplier;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "供應商 DTO")
public class SupplierDto {
    @Schema(description = "供應商 ID", example = "10")
    private Long id;

    @Schema(description = "名稱", example = "有機蔬菜行")
    private String name;

    @Schema(description = "聯絡人", example = "陳先生")
    private String contact;

    @Schema(description = "電話", example = "0912345678")
    private String phone;

    @Schema(description = "結帳週期", example = "MONTHLY")
    private String billingCycle;

    @Schema(description = "備註", example = "固定週三送貨")
    private String note;

    @Schema(description = "是否啟用（true=啟用、false=停用）", example = "true")
    private Boolean active;
}

