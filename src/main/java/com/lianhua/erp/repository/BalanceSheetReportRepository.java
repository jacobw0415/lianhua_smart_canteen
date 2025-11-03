package com.lianhua.erp.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
@RequiredArgsConstructor
public class BalanceSheetReportRepository {

    private final JdbcTemplate jdbcTemplate;

    // 應收帳款
    public BigDecimal getAccountsReceivable(String period) {
        String sql = """
            SELECT COALESCE(SUM(o.total_amount) - SUM(COALESCE(r.amount, 0)), 0)
            FROM orders o
            LEFT JOIN receipts r ON o.id = r.order_id
            WHERE (? IS NULL OR o.accounting_period = ?)
        """;
        return jdbcTemplate.queryForObject(sql, BigDecimal.class, period, period);
    }

    // 應付帳款
    public BigDecimal getAccountsPayable(String period) {
        String sql = """
            SELECT COALESCE(SUM(p.total_amount) - SUM(COALESCE(pay.amount, 0)), 0)
            FROM purchases p
            LEFT JOIN payments pay ON p.id = pay.purchase_id
            WHERE (? IS NULL OR p.accounting_period = ?)
        """;
        return jdbcTemplate.queryForObject(sql, BigDecimal.class, period, period);
    }

    // 現金收款（Receipts）
    public BigDecimal getCashReceipts(String period) {
        String sql = """
            SELECT COALESCE(SUM(r.amount), 0)
            FROM receipts r
            WHERE (? IS NULL OR r.accounting_period = ?)
        """;
        return jdbcTemplate.queryForObject(sql, BigDecimal.class, period, period);
    }

    // 零售現金銷售（Sales）
    public BigDecimal getCashSales(String period) {
        String sql = """
            SELECT COALESCE(SUM(s.amount), 0)
            FROM sales s
            WHERE (? IS NULL OR s.accounting_period = ?)
              AND s.pay_method IN ('CASH','MOBILE')
        """;
        return jdbcTemplate.queryForObject(sql, BigDecimal.class, period, period);
    }
}
