package com.lianhua.erp.service;

import com.lianhua.erp.dto.report.APAgingReportDto;

import java.util.List;

/**
 * 應付帳齡報表服務介面
 */
public interface APAgingReportService {

    /**
     * 取得應付帳齡報表資料
     * @return 供應商未付款與帳齡資訊
     */
    List<APAgingReportDto> getAgingPayables(Long supplierId, Integer minOverdue, String period);
}
