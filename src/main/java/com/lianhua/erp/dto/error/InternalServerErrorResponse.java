package com.lianhua.erp.dto.error;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 500 Internal Server Error
 * 用於伺服器內部錯誤或非預期例外。
 */
@Schema(description = "500 錯誤回應：伺服器內部錯誤")
public class InternalServerErrorResponse extends BaseErrorResponse {
    @Schema(description = "HTTP 狀態碼", example = "500")
    private final int status = 500;

    @Schema(description = "錯誤類型", example = "Internal Server Error")
    private final String error = "Internal Server Error";

    @Schema(description = "錯誤訊息", example = "伺服器錯誤，請洽後台管理員")
    private final String message = "伺服器錯誤，請洽後台管理員";

    public InternalServerErrorResponse(String message) {
        super(500, "Internal Server Error", message);
    }
}
