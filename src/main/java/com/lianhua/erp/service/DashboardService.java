    package com.lianhua.erp.service;

    import com.lianhua.erp.dto.dashboard.*;
    import com.lianhua.erp.dto.dashboard.analytics.*; // 引入分析專用 DTO

    import java.util.List;

    /**
     * 📊 蓮華 ERP 儀表板服務介面
     * 負責處理營運指標計算、雙軸趨勢數據整合及深度決策分析
     */
    public interface DashboardService {

        /* =========================================================
         * 1. 基礎監控 API (基礎字卡與趨勢)
         * ========================================================= */

        /**
         * 獲取所有核心 KPI 指標摘要 (第一排至第四排字卡)
         */
        DashboardStatsDto getDashboardStats();

        /**
         * 獲取營運趨勢圖數據 (雙 Y 軸對比：零售營收 vs 訂單收款)
         */
        List<TrendPointDto> getSalesTrendData(int days);

        /**
         * 獲取本月支出結構數據 (用於圓餅圖)
         */
        List<ExpenseCompositionDto> getExpenseComposition();

        /**
         * 獲取待辦任務與即期預警明細 (用於資訊牆清單)
         */
        List<DashboardTaskDto> getPendingTasks();

        /* =========================================================
         * 2. 進階分析 API (v2.0 決策支援)
         * ========================================================= */


        /**
         * 獲取帳款帳齡風險分析 (AR/AP Aging)
         * @return 不同天數區段的帳款分佈
         */
        List<AccountAgingDto> getAgingAnalytics();

        /**
         * 獲取損益四線走勢 (營收、毛利、費用、淨利)
         * @return 跨期間的損益數據點
         */
        List<ProfitLossPointDto> getProfitLossTrend(int months);

        /**
         * 獲取訂單履約轉化漏斗分析
         * @return 各階段訂單狀態的筆數與涉及金額
         */
        List<OrderFunnelDto> getOrderFunnel(String period);
    }