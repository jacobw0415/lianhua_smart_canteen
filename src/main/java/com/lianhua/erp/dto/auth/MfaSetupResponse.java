package com.lianhua.erp.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "MFA 綁定設定回應（供 Google Authenticator 等 App 掃描）")
public class MfaSetupResponse {

    @Schema(description = "TOTP 密鑰（Base32），可手動輸入至驗證器 App")
    private String secret;

    @Schema(description = "otpauth 連結，可轉成 QR Code 供 App 掃描")
    private String qrCodeUrl;
}
