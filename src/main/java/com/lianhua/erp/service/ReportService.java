package com.lianhua.erp.service;

import com.lianhua.erp.dto.report.ProfitReportDto;
import java.util.List;

/**
 * 報表服務介面
 * 定義報表查詢的主要方法。
 */
public interface ReportService {

    /**
     * 取得月損益報表
     * @return 損益報表列表（依月份）
     */
    List<ProfitReportDto> getMonthlyProfitReport(String period, String startDate, String endDate);
}
