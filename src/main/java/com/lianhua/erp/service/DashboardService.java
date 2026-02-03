    package com.lianhua.erp.service;

    import com.lianhua.erp.dto.dashboard.*;
    import com.lianhua.erp.dto.dashboard.analytics.*; // 引入分析專用 DTO

    import java.time.LocalDate;
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

        /* =========================================================
         * 3. 財務三表與深度決策 API (v3.0 新增)
         * ========================================================= */

        /**
         * [圖表 1] 獲取損益平衡分析數據
         * 用於分析累計營收何時超越固定成本門檻
         * @param period 會計期間
         */
        List<BreakEvenPointDto> getBreakEvenAnalysis(String period);

        /**
         * [圖表 2] 獲取流動性與償債能力指標
         * 包含流動比率、速動資產等財務健康度數據
         */
        LiquidityDto getLiquidityAnalytics();

        /**
         * [圖表 3] 獲取未來 30 天現金流預測
         * 結合應收與應付到期日進行資金水位預估
         */
        List<CashflowForecastDto> getCashflowForecast(LocalDate baseDate, int days);


        /**
         * [圖表 4] 獲取商品獲利貢獻 Pareto 分析
         * 識別貢獻 80% 獲利的關鍵品項
         * @param start 開始日期
         * @param end 結束日期
         */
        List<ProductParetoDto> getProductParetoAnalysis(LocalDate start, LocalDate end);

        /**
         * [圖表 5] 獲取供應商採購集中度分析
         * 評估採購金額在各供應商間的佔比與風險
         * @param start 開始日期
         * @param end 結束日期
         */
        List<SupplierConcentrationDto> getSupplierConcentration(LocalDate start, LocalDate end);

        /**
         * [圖表 6] 獲取客戶回購與沉睡分析
         * 監控客戶下單間隔與潛在流失風險
         */
        List<CustomerRetentionDto> getCustomerRetention();

        /**
         * [圖表 7] 獲取採購結構分析 (依進貨項目)
         * 分析特定期間內各品項的採購金額佔比
         * @param start 開始日期
         * @param end 結束日期
         */
        List<PurchaseStructureDto> getPurchaseStructureByItem(LocalDate start, LocalDate end);

        /**
         * [圖表 8] 獲取客戶採購集中度分析
         * 分析指定期間內各客戶的訂單總額及其對比全體營收的佔比。
         * @param start 開始日期
         * @param end 結束日期
         */
        List<CustomerConcentrationDto> getCustomerConcentration(LocalDate start, LocalDate end);

    }