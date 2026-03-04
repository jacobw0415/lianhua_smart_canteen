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
@Schema(description = "登入需 MFA 時的回應（尚未發放 JWT，需再呼叫 /mfa/verify）")
public class MfaPendingResponse {

    @Schema(description = "是否需進行 MFA 驗證", example = "true")
    private boolean mfaRequired;

    @Schema(description = "暫存 Token，呼叫 POST /api/auth/mfa/verify 時帶上此 Token 與 code")
    private String pendingToken;
}
