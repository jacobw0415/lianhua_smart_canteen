package com.lianhua.erp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "角色 DTO")
public class RoleDto {
    @Schema(description = "角色 ID", example = "1")
    private Long id;

    @Schema(description = "角色名稱", example = "ADMIN")
    private String name;
}

