package com.lianhua.erp.dto.product;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@Schema(description = "商品回應資料")
public class ProductResponseDto {

    @Schema(description = "商品ID", example = "1")
    private Long id;

    @Schema(description = "商品名稱", example = "香菇素便當")
    private String name;

    @Schema(description = "商品類別", example = "VEG_LUNCHBOX")
    private String category;

    @Schema(description = "單價", example = "75.00")
    private BigDecimal unitPrice;

    @Schema(description = "是否啟用", example = "true")
    private Boolean active;

    @Schema(description = "建立時間")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Taipei")
    private LocalDateTime createdAt;

    @Schema(description = "最後更新時間")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Taipei")
    private LocalDateTime updatedAt;

    @Schema(description = "銷售紀錄 ID 清單（僅在查詢含關聯時返回）")
    private List<Long> saleIds;

    @Schema(description = "訂單明細 ID 清單（僅在查詢含關聯時返回）")
    private List<Long> orderItemIds;
}
