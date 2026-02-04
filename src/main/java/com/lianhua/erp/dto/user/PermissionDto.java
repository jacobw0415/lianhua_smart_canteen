package com.lianhua.erp.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "權限定義資訊")
public class PermissionDto {

    @Schema(description = "權限 ID", example = "10")
    private Long id;

    @Schema(description = "權限識別碼", example = "purchase:void")
    private String name;

    @Schema(description = "權限中文描述", example = "作廢進貨單")
    private String description;

    @Schema(description = "所屬模組", example = "進貨管理")
    private String module;
}