package com.lianhua.erp.service;

import com.lianhua.erp.dto.report.ARSummaryReportDto;

import java.util.List;

public interface ARSummaryReportService {

    List<ARSummaryReportDto> generateSummary(String period);

    List<ARSummaryReportDto> generateSummary(String period, String endDate);

    List<ARSummaryReportDto> generateSummary(List<String> periods);
}
