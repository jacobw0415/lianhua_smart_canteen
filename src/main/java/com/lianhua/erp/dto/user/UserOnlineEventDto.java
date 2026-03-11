package com.lianhua.erp.dto.user;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * WebSocket 推送：單一使用者上線/下線事件。
 * 前端訂閱 /topic/online-users 可即時更新列表。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "使用者上線/下線事件")
public class UserOnlineEventDto {

    @Schema(description = "事件類型：ONLINE / OFFLINE")
    private String eventType;

    @Schema(description = "使用者 ID")
    private Long userId;

    @Schema(description = "帳號")
    private String username;

    @Schema(description = "全名")
    private String fullName;

    @Schema(description = "上線或最後活動時間")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime at;
}
