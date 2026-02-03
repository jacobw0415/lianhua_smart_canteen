package com.lianhua.erp.dto.dashboard.analytics;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "客戶回購與沉睡分析")
public record CustomerRetentionDto(
        @Schema(description = "客戶名稱", example = "蓮華大飯店")
        String customerName,

        @Schema(description = "最後一次下單日期", example = "2025-12-20")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "Asia/Taipei")
        java.time.LocalDate lastOrderDate,

        @Schema(description = "距今天數", example = "41")
        int daysSinceLastOrder,

        @Schema(description = "狀態標籤", example = "沉睡風險", allowableValues = {"活躍", "沉睡風險", "流失"})
        String status
) {}