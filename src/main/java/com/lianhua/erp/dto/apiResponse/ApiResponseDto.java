package com.lianhua.erp.dto.apiResponse;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;

/**
 * 統一 API 回應格式（支援成功與錯誤）
 * 所有時間自動以台北時區（+08:00）輸出
 */
@Schema(description = "統一 API 回應格式（支援成功與錯誤）")
@JsonPropertyOrder({"status", "message", "data", "timestamp"})
public record ApiResponseDto<T>(
        @Schema(description = "HTTP 狀態碼", example = "200") int status,
        @Schema(description = "訊息說明", example = "成功") String message,
        @Schema(description = "實際資料內容", nullable = true) T data,
        @Schema(description = "回應時間（台北時區）", example = "2025-10-10T19:54:52+08:00") String timestamp
) {
    // 統一時間格式方法
    private static String now() {
        return ZonedDateTime.now(ZoneId.of("Asia/Taipei"))
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }
    
    // ✅ 成功（200 OK）
    public static <T> ApiResponseDto<T> ok(T data) {
        return new ApiResponseDto<>(200, "成功", data, now());
    }
    
    // ✅ 建立成功（201 Created）
    public static <T> ApiResponseDto<T> created(T data) {
        return new ApiResponseDto<>(201, "建立成功", data, now());
    }
    
    // ✅ 刪除成功（204 No Content）
    public static <T> ApiResponseDto<T> deleted() {
        return new ApiResponseDto<>(204, "刪除成功", null, now());
    }
    
    // ✅ 錯誤（通用）
    public static <T> ApiResponseDto<T> error(int status, String message) {
        return new ApiResponseDto<>(status, message, null, now());
    }
}
