package com.lianhua.erp.dto.error;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * ✅ 204 No Content
 * 表示操作成功但沒有回傳內容，例如刪除成功。
 * （此類回應不應被視為錯誤，而是成功狀態）
 */
@Schema(description = "204 成功但無內容：刪除成功或不需回傳資料")
public class NoContentResponse extends BaseErrorResponse {

    @Schema(description = "HTTP 狀態碼", example = "204")
    private final int status = 204;

    @Schema(description = "狀態描述", example = "No Content")
    private final String error = "No Content";

    @Schema(description = "錯誤訊息", example = "刪除成功，沒有內容可回傳")
    private final String message = "刪除成功，沒有內容可回傳";

    public NoContentResponse() {
        super(204, "No Content", "刪除成功，沒有內容可回傳");
    }
}
