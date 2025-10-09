package com.lianhua.erp.dto.apiResponse;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "統一 API 回應格式（支援成功與錯誤）")
public record ApiResponseDto<T>(
        @Schema(description = "HTTP 狀態碼", example = "200") int status,
        @Schema(description = "訊息說明", example = "成功") String message,
        @Schema(description = "實際資料內容", nullable = true) T data,
        @Schema(description = "回應時間", example = "2025-10-09T10:00:00Z") String timestamp
) {
    // 成功回傳
    public static <T> ApiResponseDto<T> ok(T data) {
        return new ApiResponseDto<>(200, "成功", data, Instant.now().toString());
    }

    // 建立成功
    public static <T> ApiResponseDto<T> created(T data) {
        return new ApiResponseDto<>(201, "建立成功", data, Instant.now().toString());
    }

    // 刪除成功（204）
    public static <T> ApiResponseDto<T> deleted() {
        return new ApiResponseDto<>(204, "刪除成功", null, Instant.now().toString());
    }

    // 錯誤通用方法（讓 ExceptionHandler 使用）
    public static <T> ApiResponseDto<T> error(int status, String message) {
        return new ApiResponseDto<>(status, "失敗", null, Instant.now().toString());
    }
}
