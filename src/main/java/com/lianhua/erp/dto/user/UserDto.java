package com.lianhua.erp.dto.user;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "使用者資訊回應 DTO")
public class UserDto {

    @Schema(description = "使用者 ID", example = "1")
    private Long id;

    @Schema(description = "帳號", example = "admin")
    private String username;

    @Schema(description = "全名", example = "Jacob Huang")
    private String fullName;

    @Schema(description = "電子郵件", example = "jacob@lianhua.com")
    private String email; // 🌿 配合 v2.7 加強版新增

    @Schema(description = "關聯員工 ID", example = "101")
    private Long employeeId; // 🌿 關鍵修正：解決 MapStruct 編譯錯誤點

    @Schema(description = "是否啟用", example = "true")
    private Boolean enabled;

    @Schema(description = "是否已啟用 MFA（雙因素認證）", example = "true")
    private Boolean mfaEnabled;

    @Schema(description = "角色名稱清單", example = "[\"ROLE_ADMIN\", \"ROLE_USER\"]")
    private List<String> roles;

    @Schema(description = "帳號建立時間")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @Schema(description = "最後登入時間")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastLoginAt;
}