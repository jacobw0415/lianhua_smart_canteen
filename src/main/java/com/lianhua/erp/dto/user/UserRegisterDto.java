package com.lianhua.erp.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserRegisterDto {
    @Schema(description = "使用者帳號", example = "jacob")
    @NotBlank
    private String username;

    @Schema(description = "使用者姓名", example = "Jacob Huang")
    private String fullName;

    @Schema(description = "登入密碼", example = "password123")
    @NotBlank
    private String password;

}
