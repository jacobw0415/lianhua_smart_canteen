package com.lianhua.erp.audit;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 供業務 Service 在單次 HTTP 請求內補充稽核「details」欄位（ThreadLocal）。
 * <p>
 * 僅應寫入非敏感摘要（不得含密碼、Token、完整個資）。攔截器會在請求結束時併入 JSON 的 {@code ctx.*} 鍵。
 */
public final class ActivityAuditContext {

    private static final int MAX_ENTRIES = 32;
    private static final int MAX_KEY_LEN = 64;
    private static final int MAX_STRING_VALUE_LEN = 2000;

    private static final ThreadLocal<Map<String, Object>> HOLDER = ThreadLocal.withInitial(LinkedHashMap::new);

    private ActivityAuditContext() {
    }

    /**
     * 寫入一筆補充欄位；超過筆數或長度則略過。
     */
    public static void put(String key, Object value) {
        if (key == null || key.isBlank() || value == null) {
            return;
        }
        String k = key.length() > MAX_KEY_LEN ? key.substring(0, MAX_KEY_LEN) : key;
        Map<String, Object> m = HOLDER.get();
        if (m.size() >= MAX_ENTRIES) {
            return;
        }
        Object v = value;
        if (v instanceof String s) {
            v = s.length() > MAX_STRING_VALUE_LEN ? s.substring(0, MAX_STRING_VALUE_LEN) : s;
        }
        m.put(k, v);
    }

    /**
     * 取出目前累積的補充欄位並清除 ThreadLocal（供攔截器呼叫）。
     */
    public static Map<String, Object> drain() {
        Map<String, Object> m = HOLDER.get();
        Map<String, Object> copy = new LinkedHashMap<>(m);
        HOLDER.remove();
        return copy.isEmpty() ? Map.of() : copy;
    }

    /**
     * 強制清除，避免執行緒回池後汙染後續請求。
     */
    public static void remove() {
        HOLDER.remove();
    }
}
