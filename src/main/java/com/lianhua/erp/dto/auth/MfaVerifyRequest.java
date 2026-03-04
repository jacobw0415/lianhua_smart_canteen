package com.lianhua.erp.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(description = "MFA 驗證請求（登入第二階段或啟用 MFA 時提交驗證碼）")
public class MfaVerifyRequest {

    @Schema(description = "登入後若需 MFA，會取得的暫存 Token；若為啟用 MFA 則不需傳")
    private String pendingToken;

    @NotBlank(message = "驗證碼不可為空")
    @Pattern(regexp = "^[0-9]{6}$", message = "驗證碼須為 6 位數字")
    @Schema(description = "TOTP 驗證碼（6 位數字）", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
    private String code;
}
