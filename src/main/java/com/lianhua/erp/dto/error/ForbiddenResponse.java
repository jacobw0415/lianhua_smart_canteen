package com.lianhua.erp.dto.error;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/**
 * 403 Forbidden
 * 用於權限不足（例如沒有存取某資源的權限）。
 */
@Getter
@Schema(description = "403 錯誤回應：權限不足或禁止存取")
public class ForbiddenResponse extends BaseErrorResponse {
    
    @Schema(description = "HTTP 狀態碼", example = "403")
    private final int status = 403;
    
    @Schema(description = "錯誤類型", example = "Forbidden")
    private final String error = "Forbidden";
    
    @Schema(description = "錯誤訊息", example = "您沒有權限執行此操作")
    private final String message;
    
    public ForbiddenResponse(String message) {
        super(403, "Forbidden", message);
        this.message = message;
    }
    
    public int getStatus() {
        return 403;
    }
}
