package com.lianhua.erp.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;
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

    @Schema(description = "全名", example = "Jacob Huang")
    private String fullName;

    @Schema(description = "是否啟用", example = "true")
    private Boolean enabled;
    
    @Schema(description = "角色清單")
    private List<String> roles;
}
