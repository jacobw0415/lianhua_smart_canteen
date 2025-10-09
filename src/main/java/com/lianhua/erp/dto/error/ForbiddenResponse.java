package com.lianhua.erp.dto.error;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 403 Forbidden
 * 用於權限不足或禁止存取。
 */
@Schema(description = "403 錯誤回應：禁止存取資源")
public class ForbiddenResponse extends BaseErrorResponse {

    @Schema(description = "HTTP 狀態碼", example = "403")
    private final int status = 403;

    @Schema(description = "錯誤類型", example = "Forbidden")
    private final String error = "Forbidden";

    @Schema(description = "錯誤訊息", example = "無權限存取此資源")
    private final String message = "無權限存取此資源";

    public ForbiddenResponse(String message) {
        super(403, "Forbidden", message);
    }
}

