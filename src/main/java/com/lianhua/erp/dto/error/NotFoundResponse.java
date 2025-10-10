package com.lianhua.erp.dto.error;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/**
 * 404 Not Found
 * 用於資源不存在（例如找不到使用者、資料 ID 不存在）。
 */
@Getter
@Schema(description = "404 錯誤回應：找不到指定資源")
public class NotFoundResponse extends BaseErrorResponse {
    
    @Schema(description = "HTTP 狀態碼", example = "404")
    private final int status = 404;
    
    @Schema(description = "錯誤類型", example = "Not Found")
    private final String error = "Not Found";
    
    @Schema(description = "錯誤訊息", example = "找不到指定的使用者資料")
    private final String message;
    
    public NotFoundResponse(String message) {
        super(404, "Not Found", message);
        this.message = message;
    }
    
    public int getStatus() {
        return 404;
    }
}
