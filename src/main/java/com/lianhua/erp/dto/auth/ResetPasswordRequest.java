package com.lianhua.erp.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "重設密碼提交")
public class ResetPasswordRequest {
    @NotBlank
    @Schema(description = "郵件收到的重設權杖 (Token)")
    private String token;

    @NotBlank
    @Schema(description = "新密碼", example = "newPassword123")
    private String newPassword;
}
