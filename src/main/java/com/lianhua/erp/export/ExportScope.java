package com.lianhua.erp.export;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * all：依篩選條件匯出全部符合列（不分頁）。<br>
 * page：依篩選條件＋與列表相同的 page / size / sort 只匯出當前頁。
 */
public enum ExportScope {
    ALL,
    PAGE;

    public static ExportScope fromQueryParam(String raw) {
        if (raw == null || raw.isBlank()) {
            return PAGE;
        }
        return switch (raw.trim().toLowerCase()) {
            case "all" -> ALL;
            case "page" -> PAGE;
            default -> throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "scope 必須為 all 或 page");
        };
    }
}
