package com.lianhua.erp.dto.user;

import com.lianhua.erp.dto.RoleDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "使用者 DTO")
public class UserDto {
    @Schema(description = "使用者 ID", example = "1")
    private Long id;

    @Schema(description = "帳號", example = "admin")
    private String username;

    @Schema(description = "全名", example = "系統管理員")
    private String fullName;

    @Schema(description = "是否啟用", example = "true")
    private Boolean enabled;
    
    @Schema(description = "角色清單")
    private Set<RoleDto> roles;
}
