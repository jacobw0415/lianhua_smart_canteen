package com.lianhua.erp.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "忘記密碼請求")
public class ForgotPasswordRequest {
    @NotBlank
    @Email
    @Schema(description = "註冊時綁定的 Email", example = "user@example.com")
    private String email;

}