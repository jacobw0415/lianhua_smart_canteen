package com.lianhua.erp.dto.supplier;


import com.lianhua.erp.dto.validation.BaseRequestDto;
import com.lianhua.erp.validation.ValidName;
import com.lianhua.erp.validation.ValidNote;
import com.lianhua.erp.validation.ValidPhone;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "供應商新增 / 更新請求 DTO")
public class SupplierRequestDto extends BaseRequestDto {

    @Schema(description = "供應商名稱", example = "有機蔬菜行")
    @NotBlank(message = "供應商名稱不可為空")
    @ValidName
    private String name;

    @Schema(description = "聯絡人", example = "陳先生")
    @ValidName   // 允許英文/中文，但不允許奇怪符號
    private String contact;

    @Schema(description = "聯絡電話", example = "0912345678")
    @ValidPhone  // 使用你的 PhoneValidator
    private String phone;

    @Schema(description = "結帳週期", example = "MONTHLY")
    @NotBlank(message = "結帳週期不可為空")
    private String billingCycle;

    @Schema(description = "是否啟用", example = "true")
    private Boolean active;

    @Schema(description = "備註", example = "固定週三送貨")
    @ValidNote
    @Size(max = 100, message = "備註最多 100 字")
    private String note;
}
