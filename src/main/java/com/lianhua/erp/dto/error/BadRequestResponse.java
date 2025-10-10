package com.lianhua.erp.dto.error;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/**
 * 400 Bad Request
 * 用於請求參數錯誤或驗證失敗（例如格式錯誤、缺少欄位）。
 */
@Getter
@Schema(description = "400 錯誤回應：請求參數錯誤或驗證失敗")
public class BadRequestResponse extends BaseErrorResponse {
    
    @Schema(description = "HTTP 狀態碼", example = "400")
    private final int status = 400;
    
    @Schema(description = "錯誤類型", example = "Bad Request")
    private final String error = "Bad Request";
    
    @Schema(description = "錯誤訊息", example = "請求參數格式不正確")
    private final String message;
    
    public BadRequestResponse(String message) {
        super(400, "Bad Request", message);
        this.message = message;
    }
    
    public int getStatus() {
        return 400;
    }
}
