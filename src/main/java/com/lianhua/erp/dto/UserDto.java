package com.lianhua.erp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

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
}
