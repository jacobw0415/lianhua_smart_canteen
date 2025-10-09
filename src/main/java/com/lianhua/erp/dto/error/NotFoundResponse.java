package com.lianhua.erp.dto.error;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 404 Not Found
 * 用於找不到指定的資源。
 */
@Schema(description = "404 錯誤回應：找不到資源")
public class NotFoundResponse extends BaseErrorResponse {
    @Schema(description = "HTTP 狀態碼", example = "404")
    private final int status = 404;

    @Schema(description = "錯誤類型", example = "Not Found")
    private final String error = "Not Found";

    @Schema(description = "錯誤訊息", example = "找不到使用者")
    private final String message = "找不到使用者";

    public NotFoundResponse(String message) {
        super(404, "Not Found", message);
    }
}
