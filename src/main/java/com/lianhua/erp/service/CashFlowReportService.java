package com.lianhua.erp.service;

import com.lianhua.erp.dto.report.CashFlowReportDto;
import java.util.List;

/**
 * 現金流量報表服務介面
 */
public interface CashFlowReportService {

    /**
     * 取得月現金流量報表
     * @return 各會計期間的現金流統計
     */
    List<CashFlowReportDto> getCashFlowReport(String startDate, String endDate, String method, String period);
}

