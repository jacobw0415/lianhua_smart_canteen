package com.lianhua.erp.dto.error;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/**
 * 409 Conflict
 * 用於資源衝突（例如帳號已存在、資料重複）。
 */
@Getter
@Schema(description = "409 錯誤回應：資源衝突或資料重複")
public class ConflictResponse extends BaseErrorResponse {
    
    @Schema(description = "HTTP 狀態碼", example = "409")
    private final int status = 409;
    
    @Schema(description = "錯誤類型", example = "Conflict")
    private final String error = "Conflict";
    
    @Schema(description = "錯誤訊息", example = "資料已存在，請嘗試其他名稱")
    private final String message;
    
    public ConflictResponse(String message) {
        super(409, "Conflict", message);
        this.message = message;
    }
    
    public int getStatus() {
        return 409;
    }
    
}
