package com.lianhua.erp.dto.error;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 🔹 通用錯誤回應基底類別
 * 所有錯誤與成功回應（如 400、401、403、404、409、500、204）皆繼承此類。
 *
 * 提供統一的錯誤結構：
 * {
 *   "status": 409,
 *   "error": "Conflict",
 *   "message": "使用者帳號已存在"
 * }
 */
@Getter
@AllArgsConstructor
@Schema(description = "通用錯誤回應基底類別，提供狀態碼、錯誤描述與訊息內容")
public class BaseErrorResponse {
    
    @Schema(description = "HTTP 狀態碼", example = "400")
    private final int status;
    
    @Schema(description = "錯誤類型或狀態描述", example = "Bad Request")
    private final String error;
    
    @Schema(description = "詳細錯誤訊息", example = "請求參數驗證失敗")
    private final String message;
    
    @Override
    public String toString() {
        return "BaseErrorResponse{" +
                "status=" + status +
                ", error='" + error + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}
