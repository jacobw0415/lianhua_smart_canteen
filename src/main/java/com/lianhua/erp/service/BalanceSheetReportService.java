package com.lianhua.erp.service;

import com.lianhua.erp.dto.report.BalanceSheetReportDto;
import java.util.List;

public interface BalanceSheetReportService {
    List<BalanceSheetReportDto> generateBalanceSheet(String period);

    List<BalanceSheetReportDto> generateBalanceSheet(String period, String endDate);

    /**
     * 生成多個月份的資產負債表（支援並列比較）
     * @param periods 多個會計期間列表（YYYY-MM）
     * @return 資產負債表報表資料列表（每個月份一筆，加上合計行）
     */
    List<BalanceSheetReportDto> generateBalanceSheet(List<String> periods);
}
