package com.lianhua.erp.repository;

import com.lianhua.erp.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface DashboardRepository extends JpaRepository<Order, Long> {

    /* =========================================================
     * 1. 營運概況 (營收與支出)
     * ========================================================= */

    @Query(value = "SELECT COALESCE(SUM(amount), 0) FROM sales WHERE sale_date = :today", nativeQuery = true)
    BigDecimal getTodaySalesTotal(@Param("today") LocalDate today);

    @Query(value = "SELECT COALESCE(SUM(total_amount), 0) FROM purchases " +
            "WHERE accounting_period = :period AND record_status = 'ACTIVE'", nativeQuery = true)
    BigDecimal getMonthPurchaseTotal(@Param("period") String period);

    @Query(value = "SELECT COALESCE(SUM(amount), 0) FROM expenses " +
            "WHERE accounting_period = :period AND status = 'ACTIVE'", nativeQuery = true)
    BigDecimal getMonthExpenseTotal(@Param("period") String period);

    /* =========================================================
     * 2. 財務指標 (應收應付)
     * ========================================================= */

    @Query(value = "SELECT COALESCE(SUM(amount), 0) FROM sales WHERE accounting_period = :period", nativeQuery = true)
    BigDecimal getMonthSalesTotal(@Param("period") String period);

    /** 應收帳款：訂單總額 - 已收款 (使用 JOIN 優化效能) */
    @Query(value = """
        SELECT COALESCE(SUM(o.total_amount - IFNULL(r_agg.paid, 0)), 0)
        FROM orders o
        LEFT JOIN (
            SELECT order_id, SUM(amount) as paid 
            FROM receipts WHERE status = 'ACTIVE' 
            GROUP BY order_id
        ) r_agg ON o.id = r_agg.order_id
        WHERE o.record_status = 'ACTIVE' AND o.payment_status != 'PAID'
        """, nativeQuery = true)
    BigDecimal getAccountsReceivableTotal();

    @Query(value = "SELECT COALESCE(SUM(balance), 0) FROM purchases WHERE record_status = 'ACTIVE' AND status != 'PAID'", nativeQuery = true)
    BigDecimal getAccountsPayableTotal();

    /* =========================================================
     * 3. 業務概況
     * ========================================================= */

    @Query(value = "SELECT COUNT(*) FROM suppliers WHERE active = 1", nativeQuery = true)
    long countActiveSuppliers();

    @Query(value = "SELECT COUNT(*) FROM order_customers", nativeQuery = true)
    long countTotalCustomers();

    @Query(value = "SELECT COUNT(*) FROM products WHERE active = 1", nativeQuery = true)
    long countActiveProducts();

    @Query(value = "SELECT COUNT(*) FROM orders " +
            "WHERE order_status NOT IN ('DELIVERED', 'CANCELLED') AND record_status = 'ACTIVE'", nativeQuery = true)
    int getPendingOrderCount();

    /* =========================================================
     * 4. 現金流量
     * ========================================================= */

    @Query(value = "SELECT COALESCE(SUM(amount), 0) FROM receipts " +
            "WHERE received_date = :today AND status = 'ACTIVE'", nativeQuery = true)
    BigDecimal getTodayReceiptsTotal(@Param("today") LocalDate today);

    /** * 今日總入金：此處建議僅統計 receipts (實際錢進來的數量)
     * 若要保留 sales + receipts，請注意業務上是否會雙倍計算現銷。
     */
    @Query(value = "SELECT " +
            "(SELECT COALESCE(SUM(amount), 0) FROM sales WHERE sale_date = :today) + " +
            "(SELECT COALESCE(SUM(amount), 0) FROM receipts WHERE received_date = :today AND status = 'ACTIVE')",
            nativeQuery = true)
    BigDecimal getTodayTotalInflow(@Param("today") LocalDate today);

    @Query(value = "SELECT " +
            "(SELECT COALESCE(SUM(amount), 0) FROM sales WHERE accounting_period = :period) + " +
            "(SELECT COALESCE(SUM(amount), 0) FROM receipts WHERE accounting_period = :period AND status = 'ACTIVE')",
            nativeQuery = true)
    BigDecimal getMonthTotalReceived(@Param("period") String period);

    /** 七日内即將到期應收帳款 (優化為 JOIN 模式) */
    @Query(value = """
        SELECT COALESCE(SUM(o.total_amount - IFNULL(r_agg.paid, 0)), 0)
        FROM orders o
        LEFT JOIN (
            SELECT order_id, SUM(amount) as paid FROM receipts 
            WHERE status = 'ACTIVE' GROUP BY order_id
        ) r_agg ON o.id = r_agg.order_id
        WHERE o.record_status = 'ACTIVE' 
          AND o.payment_status != 'PAID' 
          AND o.delivery_date <= DATE_ADD(CURDATE(), INTERVAL 7 DAY)
        """, nativeQuery = true)
    BigDecimal getUpcomingAR();

    /* =========================================================
     * 5. 趨勢分析
     * ========================================================= */

    @Query(value = """
        SELECT 
            t.d AS date,
            SUM(t.s_amt) AS saleAmount,
            SUM(t.r_amt) AS receiptAmount
        FROM (
            SELECT DATE(sale_date) AS d, amount AS s_amt, 0 AS r_amt 
            FROM sales 
            WHERE sale_date >= :startDate
            UNION ALL
            SELECT DATE(received_date) AS d, 0 AS s_amt, amount AS r_amt 
            FROM receipts 
            WHERE received_date >= :startDate AND status = 'ACTIVE'
        ) AS t
        GROUP BY t.d
        ORDER BY t.d ASC
        """, nativeQuery = true)
    List<Object[]> getCombinedTrend(@Param("startDate") LocalDate startDate);

    /* =========================================================
     * 6. 其他圖表與列表
     * ========================================================= */

    @Query(value = """
        SELECT '進貨採購' as category, COALESCE(SUM(total_amount), 0) as amount 
        FROM purchases WHERE accounting_period = :period AND record_status = 'ACTIVE'
        UNION ALL
        SELECT ec.name as category, COALESCE(SUM(e.amount), 0) as amount 
        FROM expenses e 
        JOIN expense_categories ec ON e.category_id = ec.id 
        WHERE e.accounting_period = :period AND e.status = 'ACTIVE'
        GROUP BY ec.name
        """, nativeQuery = true)
    List<Object[]> getMonthlyExpenseComposition(@Param("period") String period);

    @Query(value = """
        SELECT oc.name, o.order_no, o.delivery_date, 
               (o.total_amount - IFNULL(r_agg.paid, 0)) as balance
        FROM orders o
        JOIN order_customers oc ON o.customer_id = oc.id
        LEFT JOIN (
            SELECT order_id, SUM(amount) as paid FROM receipts 
            WHERE status = 'ACTIVE' GROUP BY order_id
        ) r_agg ON o.id = r_agg.order_id
        WHERE o.record_status = 'ACTIVE' 
          AND o.payment_status != 'PAID'
          AND o.delivery_date <= DATE_ADD(CURDATE(), INTERVAL 7 DAY)
        ORDER BY o.delivery_date ASC LIMIT 5
        """, nativeQuery = true)
    List<Object[]> getUpcomingARList();

    /* =========================================================
     * 7. 進階分析圖表
     * ========================================================= */

    /** AR / AP 帳齡風險分析 (完整補齊邏輯並優化效能) */
    @Query(value = """
        SELECT 
            t.bucketLabel,
            SUM(t.arAmount) AS arAmount,
            SUM(t.apAmount) AS apAmount
        FROM (
            -- 應收部分
            SELECT 
                CASE 
                    WHEN DATEDIFF(CURDATE(), o.delivery_date) <= 30 THEN '0-30天'
                    WHEN DATEDIFF(CURDATE(), o.delivery_date) <= 60 THEN '31-60天'
                    WHEN DATEDIFF(CURDATE(), o.delivery_date) <= 90 THEN '61-90天'
                    ELSE '>90天' 
                END AS bucketLabel,
                (o.total_amount - IFNULL(r_agg.paid, 0)) AS arAmount,
                0 AS apAmount
            FROM orders o
            LEFT JOIN (
                SELECT order_id, SUM(amount) as paid FROM receipts 
                WHERE status = 'ACTIVE' GROUP BY order_id
            ) r_agg ON o.id = r_agg.order_id
            WHERE o.record_status = 'ACTIVE' AND o.payment_status != 'PAID'
            
            UNION ALL
            
            -- 應付部分
            SELECT 
                CASE 
                    WHEN DATEDIFF(CURDATE(), p.purchase_date) <= 30 THEN '0-30天'
                    WHEN DATEDIFF(CURDATE(), p.purchase_date) <= 60 THEN '31-60天'
                    WHEN DATEDIFF(CURDATE(), p.purchase_date) <= 90 THEN '61-90天'
                    ELSE '>90天' 
                END AS bucketLabel,
                0 AS arAmount,
                p.balance AS apAmount
            FROM purchases p
            WHERE p.record_status = 'ACTIVE' AND p.status != 'PAID'
        ) t
        GROUP BY t.bucketLabel
        ORDER BY FIELD(t.bucketLabel, '0-30天', '31-60天', '61-90天', '>90天')
        """, nativeQuery = true)
    List<Object[]> getAccountAging();

    /** 損益趨勢 (修正排序，由舊到新回傳最近 6 個月) */
    @Query(value = """
        SELECT * FROM (
            SELECT 
                t.period,
                SUM(t.rev) AS revenue,
                SUM(t.rev - t.cost) AS grossProfit,
                SUM(t.exp) AS expense,
                SUM(t.rev - t.cost - t.exp) AS netProfit
            FROM (
                SELECT accounting_period AS period, amount AS rev, 0 AS cost, 0 AS exp FROM sales
                UNION ALL
                SELECT accounting_period AS period, 0 AS rev, total_amount AS cost, 0 AS exp 
                FROM purchases WHERE record_status = 'ACTIVE'
                UNION ALL
                SELECT accounting_period AS period, 0 AS rev, 0 AS cost, amount AS exp 
                FROM expenses WHERE status = 'ACTIVE'
            ) t
            GROUP BY t.period 
            ORDER BY t.period DESC 
            LIMIT 6
        ) final_res ORDER BY period ASC
        """, nativeQuery = true)
    List<Object[]> getProfitLossTrend();

    @Query(value = """
        SELECT 
            order_status AS stage,
            COUNT(*) AS orderCount,
            SUM(total_amount) AS totalAmount
        FROM orders
        WHERE record_status = 'ACTIVE'
        GROUP BY order_status
        """, nativeQuery = true)
    List<Object[]> getOrderFunnel();

    /* =========================================================
     * 3. 核心決策圖表 (v3.0 財務三表與深度分析)
     * ========================================================= */

    /** * [圖表 1] 損益平衡分析 (Break-Even Analysis)
     * 回傳：日期、當日累計營收、當日累計支出、損益平衡門檻
     */
    @Query(value = """
        SELECT 
            t_agg.d AS date,
            SUM(t_agg.daily_rev) OVER (ORDER BY t_agg.d) AS runningRevenue,
            SUM(t_agg.daily_exp) OVER (ORDER BY t_agg.d) AS runningExpense,
            (SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE accounting_period = :period AND status = 'ACTIVE') AS breakEvenThreshold
        FROM (
            SELECT d, SUM(rev) as daily_rev, SUM(exp) as daily_exp
            FROM (
                SELECT sale_date AS d, amount AS rev, 0 AS exp FROM sales WHERE accounting_period = :period
                UNION ALL
                SELECT expense_date AS d, 0 AS rev, amount AS exp FROM expenses WHERE accounting_period = :period AND status = 'ACTIVE'
            ) union_t
            GROUP BY d
        ) t_agg
        ORDER BY t_agg.d ASC
        """, nativeQuery = true)
    List<Object[]> getBreakEvenData(@Param("period") String period);

    /** * [圖表 2] 流動性與償債能力 (Current Ratio)
     * 回傳：流動資產、流動負債、速動資產
     */
    @Query(value = """
        SELECT 
            (SELECT COALESCE(SUM(amount), 0) FROM sales) + (SELECT COALESCE(SUM(amount), 0) FROM receipts WHERE status = 'ACTIVE') AS liquidAssets,
            (SELECT COALESCE(SUM(balance), 0) FROM purchases WHERE record_status = 'ACTIVE') AS liquidLiabilities,
            (SELECT COALESCE(SUM(amount), 0) FROM receipts WHERE status = 'ACTIVE') AS quickAssets
        """, nativeQuery = true)
    List<Object[]> getLiquidityMetrics();

    /** * [圖表 3] 未來 30 天現金流預測 (Cash Flow Forecast)
     * 回傳：預測日期、預計流入、預計流出
     */
    /** 未來 30 天現金流預測 (修正逾期邏輯) */
    /** * [圖表 3] 未來現金流預測
     * 邏輯：將基準日之前的「未結帳款」全部歸類到基準日當天（顯示即時資金壓力）
     * 並過濾出基準日後 X 天內的預計流入與流出。
     */
    @Query(value = """
    SELECT 
        -- 核心邏輯：基準日以前的欠款擠壓到基準日，其餘按原日期顯示
        CASE WHEN t.pred_date < :baseDate THEN :baseDate ELSE t.pred_date END AS date,
        SUM(expected_in) AS inflow,
        SUM(expected_out) AS outflow
    FROM (
        -- 1. 應收帳款 (AR)
        SELECT o.delivery_date AS pred_date, 
               (o.total_amount - IFNULL(r_agg.paid, 0)) AS expected_in, 
               0 AS expected_out
        FROM orders o
        LEFT JOIN (
            SELECT order_id, SUM(amount) as paid 
            FROM receipts WHERE status = 'ACTIVE' 
            GROUP BY order_id
        ) r_agg ON o.id = r_agg.order_id
        WHERE o.record_status = 'ACTIVE' 
          AND o.payment_status != 'PAID'
          -- 🔍 修正過濾：只抓基準日之後 N 天內的，或基準日之前的逾期款
          AND DATEDIFF(o.delivery_date, :baseDate) <= :days
        
        UNION ALL
        
        -- 2. 應付帳款 (AP)
        SELECT p.purchase_date AS pred_date, 
               0 AS expected_in, 
               p.balance AS expected_out
        FROM purchases p 
        WHERE p.record_status = 'ACTIVE' 
          AND p.status != 'PAID'
          -- 🔍 修正過濾：只抓基準日之後 N 天內的，或基準日之前的逾期款
          AND DATEDIFF(p.purchase_date, :baseDate) <= :days
    ) t
    -- 🔍 終極保險：過濾掉太遠以後的資料（例如 days 設 7，就不該出現 30 天後的點）
    -- 且排除掉已經「太老」的數據（視需求而定，這裡設定抓取範圍內的所有點）
    WHERE DATEDIFF(CASE WHEN t.pred_date < :baseDate THEN :baseDate ELSE t.pred_date END, :baseDate) BETWEEN 0 AND :days
    GROUP BY date 
    ORDER BY date ASC
    """, nativeQuery = true)
    List<Object[]> getCashflowForecast(@Param("baseDate") LocalDate baseDate, @Param("days") int days);

    /** * [圖表 4] 商品獲利貢獻 Pareto 分析 (Pareto Driver)
     * 回傳：商品名稱、總銷售額、累計百分比
     */
    @Query(value = """
    SELECT 
        name, totalAmount,
        (SUM(totalAmount) OVER (ORDER BY totalAmount DESC) / SUM(totalAmount) OVER ()) * 100 AS cumulativePct
    FROM (
        SELECT p.name, SUM(s.amount) AS totalAmount
        FROM sales s
        JOIN products p ON s.product_id = p.id
        WHERE s.sale_date BETWEEN :startDate AND :endDate
        GROUP BY p.name
    ) t
    ORDER BY totalAmount DESC
    """, nativeQuery = true)
    List<Object[]> getProductPareto(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /** * [圖表 5] 供應商採購集中度 (Concentration)
     * 回傳：供應商名稱、採購佔比、採購總額
     */
    @Query(value = """
    SELECT 
        s.name,
        -- 分子：該供應商採購總額 / 分母：該區間所有 active 採購總額
        (SUM(p.total_amount) / 
            NULLIF((SELECT SUM(total_amount) 
                    FROM purchases 
                    WHERE record_status = 'ACTIVE' 
                    AND purchase_date BETWEEN :startDate AND :endDate), 0)
        ) * 100 AS ratio,
        SUM(p.total_amount) AS totalAmount
    FROM purchases p
    JOIN suppliers s ON p.supplier_id = s.id
    WHERE p.record_status = 'ACTIVE' 
      AND p.purchase_date BETWEEN :startDate AND :endDate
    GROUP BY s.name
    ORDER BY totalAmount DESC
    """, nativeQuery = true)
    List<Object[]> getSupplierConcentration(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /** * [圖表 6] 客戶回購與沉睡分析 (Customer Retention)
     * 回傳：客戶名稱、最後交易日、距今天數、狀態
     */
    @Query(value = """
        SELECT 
            c.name,
            MAX(o.order_date) AS lastOrderDate,
            DATEDIFF(CURDATE(), MAX(o.order_date)) AS daysSinceLastOrder,
            CASE 
                WHEN DATEDIFF(CURDATE(), MAX(o.order_date)) <= 30 THEN '活躍'
                WHEN DATEDIFF(CURDATE(), MAX(o.order_date)) <= 60 THEN '沉睡風險'
                ELSE '流失'
            END AS status
        FROM order_customers c
        LEFT JOIN orders o ON c.id = o.customer_id AND o.record_status = 'ACTIVE'
        GROUP BY c.name
        ORDER BY daysSinceLastOrder DESC
        """, nativeQuery = true)
    List<Object[]> getCustomerRetention();
}