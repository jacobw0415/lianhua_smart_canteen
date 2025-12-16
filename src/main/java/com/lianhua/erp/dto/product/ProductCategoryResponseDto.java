package com.lianhua.erp.dto.product;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "商品分類回應 DTO")
public class ProductCategoryResponseDto {

    @Schema(description = "分類ID", example = "1")
    private Long id;

    @Schema(description = "分類名稱", example = "素食便當")
    private String name;

    @Schema(description = "分類代碼", example = "VEG")
    private String code;

    @Schema(description = "分類描述", example = "提供素食便當類商品")
    private String description;

    @Schema(description = "是否啟用（true=啟用、false=停用）", example = "true")
    private Boolean active;

    @Schema(description = "建立時間")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Taipei")
    private LocalDateTime createdAt;

    @Schema(description = "最後更新時間")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Taipei")
    private LocalDateTime updatedAt;
}
