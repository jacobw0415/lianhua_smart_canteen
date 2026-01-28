package com.lianhua.erp.service;

import com.lianhua.erp.dto.dashboard.DashboardStatsDto;
import com.lianhua.erp.dto.dashboard.TrendPointDto;
import java.util.List;

public interface DashboardService {
    /**
     * 獲取所有核心 KPI 指標摘要
     */
    DashboardStatsDto getDashboardStats();

    /**
     * 獲取營運趨勢圖數據 (預設 30 天)
     */
    List<TrendPointDto> getSalesTrendData(int days);
}