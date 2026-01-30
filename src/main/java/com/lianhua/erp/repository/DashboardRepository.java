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
}