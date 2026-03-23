package com.lianhua.erp.dto.export;

/**
 * 列表／報表匯出回傳給 Controller 組 HTTP 標頭與 body 用。
 */
public record ExportPayload(byte[] data, String filename, String mediaType) {
}
