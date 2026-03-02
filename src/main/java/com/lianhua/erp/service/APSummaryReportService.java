package com.lianhua.erp.service;

import com.lianhua.erp.dto.report.APSummaryReportDto;

import java.util.List;

/**
 * 📊 應付帳款報表服務介面
 */
public interface APSummaryReportService {

    List<APSummaryReportDto> generateSummary(String period);

    List<APSummaryReportDto> generateSummary(String period, String endDate);

    List<APSummaryReportDto> generateSummary(List<String> periods);
}