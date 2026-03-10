package com.lianhua.erp.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserRegisterDto {
    @Schema(description = "使用者帳號", example = "jacob")
    @NotBlank
    private String username;

    @Schema(description = "使用者姓名", example = "Jacob Huang")
    private String fullName;

    @Schema(description = "登入密碼", example = "Password123")
    @NotBlank
    private String password;

    @Schema(description = "電子郵件", example = "user@example.com")
    @NotBlank(message = "Email 不可為空")
    @Email(message = "Email 格式不正確")
    private String email;

}
