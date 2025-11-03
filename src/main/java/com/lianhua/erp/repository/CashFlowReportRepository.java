package com.lianhua.erp.repository;


import com.lianhua.erp.domain.Receipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 現金流量報表 Repository
 * 統計 sales + receipts + payments + expenses 四表之現金流。
 */
@Repository
public interface CashFlowReportRepository extends JpaRepository<Receipt, Long> {

    @Query(value = """
        SELECT accounting_period AS period,
               SUM(total_sales) AS total_sales,
               SUM(total_receipts) AS total_receipts,
               SUM(total_payments) AS total_payments,
               SUM(total_expenses) AS total_expenses,
               (SUM(total_sales) + SUM(total_receipts)) AS total_inflow,
               (SUM(total_payments) + SUM(total_expenses)) AS total_outflow,
               ((SUM(total_sales) + SUM(total_receipts)) - (SUM(total_payments) + SUM(total_expenses))) AS net_cash_flow
        FROM (
            -- 💰 現金銷售（零售）
            SELECT s.accounting_period,
                   SUM(s.amount) AS total_sales,
                   0 AS total_receipts,
                   0 AS total_payments,
                   0 AS total_expenses
            FROM sales s
            WHERE s.pay_method = 'CASH' -- 僅統計現金收款部分
            GROUP BY s.accounting_period

            UNION ALL

            -- 💰 收款表（批發訂單收入）
            SELECT r.accounting_period,
                   0 AS total_sales,
                   SUM(r.amount) AS total_receipts,
                   0 AS total_payments,
                   0 AS total_expenses
            FROM receipts r
            GROUP BY r.accounting_period

            UNION ALL

            -- 💸 採購付款（供應商）
            SELECT p.accounting_period,
                   0 AS total_sales,
                   0 AS total_receipts,
                   SUM(p.amount) AS total_payments,
                   0 AS total_expenses
            FROM payments p
            GROUP BY p.accounting_period

            UNION ALL

            -- 💸 營運支出（費用表）
            SELECT e.accounting_period,
                   0 AS total_sales,
                   0 AS total_receipts,
                   0 AS total_payments,
                   SUM(e.amount) AS total_expenses
            FROM expenses e
            GROUP BY e.accounting_period
        ) AS combined
        GROUP BY accounting_period
        ORDER BY accounting_period DESC
        """, nativeQuery = true)
    List<Object[]> findMonthlyCashFlowReport(String startDate, String endDate, String method, String period);
}