package com.lianhua.erp.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Set;

@Data
public class UserRequestDto {

    @Schema(description = "使用者帳號", example = "admin")
    private String username;

    @Schema(description = "使用者姓名", example = "系統管理員")
    private String fullName;

    @Schema(description = "登入密碼", example = "password123")
    private String password;

    @Schema(description = "是否啟用帳號", example = "true")
    private Boolean enabled;

    @Schema(description = "角色名稱列表，例如 ['ADMIN', 'USER']（限管理員可用）")
    private Set<String> roleNames;
}
