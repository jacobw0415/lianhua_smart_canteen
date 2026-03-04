package com.lianhua.erp.security;

import java.util.regex.Pattern;

/**
 * 簡易敏感資料遮罩工具。
 * 目前僅用於在記錄 log 前，遮罩密碼與長 Token 類字串，可視需要再擴充。
 */
public final class SensitiveDataMasker {

    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("(\"password\"\\s*:\\s*\")[^\"]*(\")", Pattern.CASE_INSENSITIVE);

    private static final int TOKEN_VISIBLE_PREFIX = 8;

    private SensitiveDataMasker() {
    }

    /**
     * 遮罩 JSON 字串中出現的 password 欄位內容。
     */
    public static String maskPasswords(String json) {
        if (json == null || json.isBlank()) {
            return json;
        }
        return PASSWORD_PATTERN.matcher(json)
                .replaceAll("$1***$2");
    }

    /**
     * 將 Token 只保留前 N 碼，其餘以 * 取代。
     */
    public static String maskToken(String token) {
        if (token == null || token.isBlank()) {
            return token;
        }
        if (token.length() <= TOKEN_VISIBLE_PREFIX) {
            return "***";
        }
        String prefix = token.substring(0, TOKEN_VISIBLE_PREFIX);
        return prefix + "***";
    }
}

