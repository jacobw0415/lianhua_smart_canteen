package com.lianhua.erp.dto.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "忘記密碼請求")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ForgotPasswordRequest {
    @NotBlank
    @Email
    @Schema(description = "註冊時綁定的 Email", example = "user@example.com")
    private String email;

    @Schema(
            description = "重設連結的基底網址（由前端根據當下網址傳入，例如 http://10.18.2.103:5173 或 http://localhost:5173）",
            example = "http://10.18.2.103:5173"
    )
    private String resetLinkBaseUrl;

}