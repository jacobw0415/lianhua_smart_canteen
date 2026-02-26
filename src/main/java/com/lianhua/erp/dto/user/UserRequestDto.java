package com.lianhua.erp.dto.user;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Set;

@Data
@Schema(description = "使用者請求 DTO (用於新增/更新使用者)")
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserRequestDto {

    @NotBlank(message = "帳號不能為空")
    @Size(min = 3, max = 50, message = "帳號長度需在 3 到 50 字元之間")
    @Schema(description = "使用者帳號", example = "admin")
    private String username;

    @Schema(description = "使用者姓名", example = "系統管理員")
    private String fullName;

    @Email(message = "電子郵件格式不正確")
    @Schema(description = "電子郵件", example = "admin@lianhua.com")
    private String email; // 🌿 新增：對應加強版 SQL

    @Schema(description = "關聯員工 ID", example = "101")
    private Long employeeId; // 🌿 新增：對應員工關聯

    @Size(min = 6, message = "密碼長度至少需要 6 位")
    @Schema(description = "登入密碼 (更新時若不修改可為空)", example = "password123")
    private String password;

    @Schema(description = "是否啟用帳號", example = "true")
    private Boolean enabled;

    @Schema(
            description = "角色代碼列表，須傳完整代碼（如 ROLE_ADMIN、ROLE_USER），後端會以大寫比對。可從 GET /api/roles 取得可用角色。",
            example = "[\"ROLE_ADMIN\"]"
    )
    private Set<String> roleNames;
}