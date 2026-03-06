package com.lianhua.erp.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 🔍 使用者搜尋參數 Request DTO
 * 所有欄位皆為可選，用於帳號管理模組的模糊搜尋。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "使用者搜尋條件")
public class UserSearchRequest {

    @Schema(description = "帳號（支援模糊搜尋）", example = "admin")
    private String username;

    @Schema(description = "姓名（支援模糊搜尋）", example = "王小明")
    private String fullName;

    @Schema(description = "Email（支援模糊搜尋）", example = "user@example.com")
    private String email;

    @Schema(description = "啟用狀態，true=啟用，false=未啟用，不給則不限制", example = "true")
    private Boolean enabled;
}

