package com.lianhua.erp.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "使用者請求 DTO")
public class UserRequestDto {
    
    @Schema(description = "帳號", example = "admin")
    private String username;
    
    @Schema(description = "全名", example = "系統管理員")
    private String fullName;
    
    @Schema(description = "密碼", example = "password123")
    private String password;
    
    @Schema(description = "是否啟用", example = "true")
    private Boolean enabled;
    
    @Schema(description = "角色 ID 清單", example = "[1, 2]")
    private Set<Long> roleIds;
}
