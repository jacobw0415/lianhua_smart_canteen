package com.lianhua.erp.service;

import com.lianhua.erp.dto.report.ARAgingReportDto;

import java.util.List;

/**
 * 應收帳齡報表服務介面
 */
public interface ARAgingReportService {

    /**
     * 取得應收帳齡報表資料
     * @return 客戶應收餘額與帳齡資訊
     */
    List<ARAgingReportDto> getAgingReceivables(Long customerId, Integer minOverdue, String period);
}

