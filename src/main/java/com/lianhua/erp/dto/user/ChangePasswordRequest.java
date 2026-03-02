package com.lianhua.erp.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 本人修改密碼 API 請求（§4.4）
 * 用於 PUT /api/users/me/password，驗證目前密碼後更新為新密碼。
 */
@Data
@Schema(description = "本人修改密碼請求")
public class ChangePasswordRequest {

    @NotBlank(message = "目前密碼不能為空")
    @Schema(description = "目前密碼", requiredMode = Schema.RequiredMode.REQUIRED)
    private String currentPassword;

    @NotBlank(message = "新密碼不能為空")
    @Size(min = 6, message = "新密碼長度至少需要 6 位")
    @Schema(description = "新密碼", requiredMode = Schema.RequiredMode.REQUIRED)
    private String newPassword;
}
