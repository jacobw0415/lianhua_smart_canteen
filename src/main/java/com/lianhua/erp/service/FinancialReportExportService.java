package com.lianhua.erp.service;

import com.lianhua.erp.dto.export.ExportPayload;
import com.lianhua.erp.dto.report.FinancialReportKey;
import com.lianhua.erp.dto.report.ReportExportQueryDto;
import com.lianhua.erp.export.ExportFormat;
import com.lianhua.erp.export.ExportScope;
import org.springframework.data.domain.Pageable;

public interface FinancialReportExportService {

    ExportPayload export(
            FinancialReportKey reportKey,
            ReportExportQueryDto query,
            String periodsCommaSeparated,
            ExportFormat format,
            ExportScope scope,
            Pageable pageable,
            String columnsCsv
    );
}
