package com.lianhua.erp.service;

import com.lianhua.erp.dto.report.CashFlowReportDto;
import java.util.List;

public interface CashFlowReportService {
    List<CashFlowReportDto> generateCashFlow(String period, String startDate, String endDate);
}


