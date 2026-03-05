package com.lianhua.erp.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(description = "關閉 MFA 請求（須提供當前 TOTP 驗證碼以確認身分）")
public class MfaDisableRequest {

    @NotBlank(message = "驗證碼不可為空")
    @Pattern(regexp = "^[0-9]{6}$", message = "驗證碼須為 6 位數字")
    @Schema(description = "當前 TOTP 驗證碼（6 位數字），用於確認身分", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
    private String code;
}
