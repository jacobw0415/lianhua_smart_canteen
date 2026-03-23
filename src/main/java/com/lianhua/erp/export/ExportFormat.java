package com.lianhua.erp.export;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public enum ExportFormat {
    XLSX,
    CSV;

    public static ExportFormat fromQueryParam(String raw) {
        if (raw == null || raw.isBlank()) {
            return XLSX;
        }
        return switch (raw.trim().toLowerCase()) {
            case "xlsx" -> XLSX;
            case "csv" -> CSV;
            default -> throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "format 必須為 xlsx 或 csv");
        };
    }

    public String fileExtension() {
        return switch (this) {
            case XLSX -> "xlsx";
            case CSV -> "csv";
        };
    }

    public String mediaType() {
        return switch (this) {
            case XLSX -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case CSV -> "text/csv; charset=UTF-8";
        };
    }
}
