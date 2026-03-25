package com.lianhua.erp.security;

import com.lianhua.erp.dto.report.FinancialReportKey;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * 財務報表匯出依 reportKey 要求不同權限（與各報表 GET Controller 一致）。
 */
@Component
public class ReportExportAuthorization {

    public void requirePermission(FinancialReportKey key) {
        String required = switch (key) {
            case AP_SUMMARY -> "ap:view";
            case AR_SUMMARY -> "ar:view";
            default -> "report:view";
        };
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("未認證");
        }
        boolean ok = auth.getAuthorities().stream()
                .anyMatch(a -> required.equals(a.getAuthority()));
        if (!ok) {
            throw new AccessDeniedException("無權限匯出此報表，需要：" + required);
        }
    }
}
