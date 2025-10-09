package com.lianhua.erp.dto.error;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "統一錯誤回傳格式")
public record ErrorResponse(
        @Schema(description = "HTTP 狀態碼", example = "404") int status,
        @Schema(description = "錯誤類型", example = "Not Found") String error,
        @Schema(description = "錯誤訊息", example = "User not found with id 5") String message,
        @Schema(description = "發生時間", example = "2025-10-09T10:05:43Z") String timestamp
) {}

