package com.lianhua.erp.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "用於換發 Access Token 的 Refresh Token 請求")
public class RefreshTokenRequest {

    @NotBlank(message = "Refresh Token 不可為空")
    @Schema(description = "登入時取得的 Refresh Token", requiredMode = Schema.RequiredMode.REQUIRED)
    private String refreshToken;
}
