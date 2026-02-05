package com.lianhua.erp.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "角色權限資訊")
public class RoleDto {

    @Schema(description = "角色 ID", example = "1")
    private Long id;

    @Schema(description = "角色代碼 (需以 ROLE_ 開頭)", example = "ROLE_ADMIN")
    private String name;

    @Schema()
    private String displayName;

    @Schema(description = "角色顯示名稱/描述", example = "系統管理員")
    private String description;

    @Schema(description = "該角色擁有的權限識別碼清單", example = "[\"user:view\", \"order:create\"]")
    private Set<String> permissions;
}