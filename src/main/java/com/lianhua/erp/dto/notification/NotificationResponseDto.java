package com.lianhua.erp.dto.notification;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponseDto {

    // 對應 user_notifications 表的 ID (用於標記已讀)
    @JsonProperty("id")
    private Long userNotificationId;

    // 渲染後的資訊 (由後端解析 TemplateCode + Payload 產生)
    private String title;
    private String content;

    // 業務關聯資訊 (用於前端點擊後跳轉)
    private String targetType; // e.g., "purchases", "orders"
    private Long targetId;     // e.g., 101
    private String actionUrl;  // 預留的跳轉路徑

    // 狀態資訊
    private Integer priority;  // 1:一般, 2:重要, 3:緊急

    @JsonProperty("read")
    private boolean isRead;

    @Schema(description = "建立時間", example = "2025-10-25T09:30:00")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Taipei")
    private LocalDateTime createdAt;
}