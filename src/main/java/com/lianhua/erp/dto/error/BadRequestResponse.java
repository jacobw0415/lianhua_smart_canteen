package com.lianhua.erp.dto.error;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "400 錯誤請求")
public class BadRequestResponse extends BaseErrorResponse {

    @Schema(description = "HTTP 狀態碼", example = "400")
    private final int status = 400;

    @Schema(description = "錯誤類型", example = "Bad Request")
    private final String error = "Bad Request";

    @Schema(description = "錯誤訊息", example = "請求參數格式錯誤")
    private final String message = "請求參數格式錯誤";

    public BadRequestResponse(String message) {
        super(400, "Bad Request", message);
    }
}
