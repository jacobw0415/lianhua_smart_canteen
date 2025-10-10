package com.lianhua.erp.dto.error;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/**
 * 401 Unauthorized
 * 用於認證失敗（例如帳號密碼錯誤、JWT 無效或未登入狀態）。
 */
@Getter
@Schema(description = "401 錯誤回應：認證失敗或未登入")
public class UnauthorizedResponse extends BaseErrorResponse {
    
    @Schema(description = "HTTP 狀態碼", example = "401")
    private final int status = 401;
    
    @Schema(description = "錯誤類型", example = "Unauthorized")
    private final String error = "Unauthorized";
    
    @Schema(description = "錯誤訊息", example = "使用者認證失敗，請重新登入或檢查憑證")
    private final String message;
    
    public UnauthorizedResponse(String message) {
        super(401, "Unauthorized", message);
        this.message = message;
    }
    
    public int getStatus() {
        return 401;
    }
}
