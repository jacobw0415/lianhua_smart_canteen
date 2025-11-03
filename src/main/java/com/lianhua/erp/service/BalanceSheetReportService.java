package com.lianhua.erp.service;

import com.lianhua.erp.dto.report.BalanceSheetReportDto;
import java.util.List;

public interface BalanceSheetReportService {
    List<BalanceSheetReportDto> generateBalanceSheet(String period);

    List<BalanceSheetReportDto> generateBalanceSheet(String period, String startDate, String endDate);
}
