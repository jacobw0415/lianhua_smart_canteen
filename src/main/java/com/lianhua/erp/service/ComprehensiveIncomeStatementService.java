package com.lianhua.erp.service;

import com.lianhua.erp.dto.report.ComprehensiveIncomeStatementDto;

import java.util.List;

/**
 * 💼 綜合損益表服務介面
 * 
 * 提供綜合損益表的查詢與生成功能。
 */
public interface ComprehensiveIncomeStatementService {

    /**
     * 取得綜合損益表（單一期間）
     * 
     * @param period 會計期間 (YYYY-MM)
     * @return 綜合損益表列表
     */
    List<ComprehensiveIncomeStatementDto> generateComprehensiveIncomeStatement(String period);

    /**
     * 取得綜合損益表（單一期間 + 日期區間）
     * 
     * @param period    會計期間 (YYYY-MM)
     * @param startDate 起始日期 (yyyy-MM-dd)
     * @param endDate   結束日期 (yyyy-MM-dd)
     * @return 綜合損益表列表
     */
    List<ComprehensiveIncomeStatementDto> generateComprehensiveIncomeStatement(
            String period, String startDate, String endDate);

    /**
     * 取得綜合損益表（多個期間比較）
     * 
     * @param periods 會計期間列表 (YYYY-MM)
     * @return 綜合損益表列表（包含各期間明細及合計）
     */
    List<ComprehensiveIncomeStatementDto> generateComprehensiveIncomeStatement(List<String> periods);
}

