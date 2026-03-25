package com.lianhua.erp.dto.report;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * 財務報表匯出路徑 {@code /api/reports/{reportKey}/export} 使用的識別值。
 */
public enum FinancialReportKey {
    BALANCE_SHEET("balance_sheet"),
    COMPREHENSIVE_INCOME_STATEMENT("comprehensive_income_statement"),
    CASH_FLOW_REPORTS("cash_flow_reports"),
    AR_SUMMARY("ar_summary"),
    AP_SUMMARY("ap_summary");

    private final String pathSegment;

    FinancialReportKey(String pathSegment) {
        this.pathSegment = pathSegment;
    }

    public String pathSegment() {
        return pathSegment;
    }

    public static FinancialReportKey fromPath(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "reportKey 不可為空");
        }
        String t = raw.trim();
        for (FinancialReportKey k : values()) {
            if (k.pathSegment.equals(t)) {
                return k;
            }
        }
        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "不支援的 reportKey: " + t);
    }
}
