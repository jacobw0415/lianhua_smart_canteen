package com.lianhua.erp.dto.user;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 使用者上線資訊 DTO，供 REST 與 WebSocket 推送使用。
 * 包含自己與其他目前線上使用者。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "使用者上線資訊")
public class OnlineUserDto {

    @Schema(description = "使用者 ID")
    private Long id;

    @Schema(description = "帳號")
    private String username;

    @Schema(description = "全名")
    private String fullName;

    @Schema(description = "上線時間")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime onlineAt;
}
