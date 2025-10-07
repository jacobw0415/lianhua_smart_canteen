package com.lianhua.erp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "使用者角色關聯 DTO")
public class UserRoleDto {

    @Schema(description = "使用者角色關聯 ID", example = "1")
    private Long id;

    @Schema(description = "使用者 ID", example = "1")
    private Long userId;

    @Schema(description = "角色 ID", example = "2")
    private Long roleId;

    @Schema(description = "角色名稱", example = "ADMIN")
    private String roleName;
}
