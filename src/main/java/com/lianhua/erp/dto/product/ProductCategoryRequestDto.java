package com.lianhua.erp.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(description = "商品分類建立／更新請求 DTO")
public class ProductCategoryRequestDto {

    @NotBlank
    @Schema(description = "分類名稱", example = "素食便當")
    private String name;

    @NotBlank
    @Pattern(regexp = "^[A-Z0-9_-]{2,20}$", message = "代碼僅能包含大寫英數字、底線或連字號，長度 2-20。")
    @Schema(description = "分類代碼（唯一）", example = "VEG")
    private String code;

    @Schema(description = "分類描述", example = "提供素食便當類商品")
    private String description;

    @Schema(description = "是否啟用", example = "true")
    private Boolean active = true;
}
