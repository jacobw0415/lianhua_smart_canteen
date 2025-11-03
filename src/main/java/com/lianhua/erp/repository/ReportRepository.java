package com.lianhua.erp.repository;

import com.lianhua.erp.domain.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 報表查詢 Repository
 * 提供基於會計期間的損益統計（整合 sales、orders、purchases、expenses）。
 */
@Repository
public interface ReportRepository extends JpaRepository<Sale, Long> {

    @Query(value = """
    SELECT accounting_period AS period,
           SUM(total_sales) AS total_sales,
           SUM(total_orders) AS total_orders,
           (SUM(total_sales) + SUM(total_orders)) AS total_revenue,
           SUM(total_purchase) AS total_purchase,
           SUM(total_expense) AS total_expense,
           ((SUM(total_sales) + SUM(total_orders)) - SUM(total_purchase) - SUM(total_expense)) AS net_profit
    FROM (
        -- 銷售表（零售收入）
        SELECT s.accounting_period,
               SUM(s.amount) AS total_sales,
               0 AS total_orders,
               0 AS total_purchase,
               0 AS total_expense
        FROM sales s
        GROUP BY s.accounting_period

        UNION ALL

        -- 訂單表（批發收入）
        SELECT o.accounting_period,
               0 AS total_sales,
               SUM(o.total_amount) AS total_orders,
               0 AS total_purchase,
               0 AS total_expense
        FROM orders o
        GROUP BY o.accounting_period

        UNION ALL

        -- 採購表（成本）
        SELECT p.accounting_period,
               0 AS total_sales,
               0 AS total_orders,
               SUM(p.total_amount) AS total_purchase,
               0 AS total_expense
        FROM purchases p
        GROUP BY p.accounting_period

        UNION ALL

        -- 費用表（營運支出）
        SELECT e.accounting_period,
               0 AS total_sales,
               0 AS total_orders,
               0 AS total_purchase,
               SUM(e.amount) AS total_expense
        FROM expenses e
        GROUP BY e.accounting_period
    ) AS combined
    GROUP BY accounting_period
    ORDER BY accounting_period DESC
    """, nativeQuery = true)
    List<Object[]> findMonthlyProfitReport(String period, String startDate, String endDate);
}
