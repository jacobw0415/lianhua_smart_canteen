package com.lianhua.erp.dto.error;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.Instant;

/**
 * 所有錯誤回應 DTO 的基底類別。
 * 統一結構：status、error、message、timestamp。
 */
@Getter
@Schema(description = "基底錯誤格式")
public class BaseErrorResponse {

    @Schema(description = "HTTP 狀態碼")  // 🚫 不設定 example
    protected int status;

    @Schema(description = "錯誤類型（對應 HTTP 狀態名稱")
    protected String error;

    @Schema(description = "錯誤訊息詳細內容")
    protected String message;

    @Schema(description = "錯誤發生時間", example = "2025-10-09T10:00:00Z")
    protected String timestamp = Instant.now().toString();

    public BaseErrorResponse(int status, String error, String message) {
        this.status = status;
        this.error = error;
        this.message = message;
    }

}
