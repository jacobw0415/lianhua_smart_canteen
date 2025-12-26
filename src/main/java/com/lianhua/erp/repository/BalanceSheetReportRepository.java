package com.lianhua.erp.repository;

import com.lianhua.erp.dto.report.BalanceSheetReportDto;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * 💼 資產負債表 Repository
 * 預設以月份 (accounting_period) 查詢，亦支援日期區間篩選。
 */
@Repository
@RequiredArgsConstructor
public class BalanceSheetReportRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 📊 查詢資產負債表
     *
     * @param period    指定月份 (YYYY-MM)
     * @param startDate 起始日期 (yyyy-MM-dd)
     * @param endDate   結束日期 (yyyy-MM-dd)
     */
    public List<BalanceSheetReportDto> getBalanceSheet(String period, String startDate, String endDate) {

        StringBuilder sql = new StringBuilder("""
            SELECT
                accounting_period,
                COALESCE(SUM(accounts_receivable), 0) AS accounts_receivable,
                COALESCE(SUM(accounts_payable), 0) AS accounts_payable,
                COALESCE(SUM(cash), 0) AS cash,
                (COALESCE(SUM(accounts_receivable), 0) + COALESCE(SUM(cash), 0)) AS total_assets,
                COALESCE(SUM(accounts_payable), 0) AS total_liabilities,
                ((COALESCE(SUM(accounts_receivable), 0) + COALESCE(SUM(cash), 0)) - COALESCE(SUM(accounts_payable), 0)) AS equity
            FROM (
                -- 🟩 應收帳款（未收客戶款）
                SELECT 
                    DATE_FORMAT(o.order_date, '%Y-%m') AS accounting_period,
                    (SUM(COALESCE(o.total_amount, 0)) - SUM(COALESCE(r.amount, 0))) AS accounts_receivable,
                    0 AS accounts_payable,
                    0 AS cash
                FROM orders o
                LEFT JOIN receipts r ON o.id = r.order_id AND r.status = 'ACTIVE'
                WHERE 1=1
        """);

        // 條件動態組合
        if (period != null && !period.isBlank()) {
            sql.append(" AND o.accounting_period = ? ");
        } else if (startDate != null && endDate != null) {
            sql.append(" AND o.order_date BETWEEN ? AND ? ");
        }

        sql.append("""
                GROUP BY DATE_FORMAT(o.order_date, '%Y-%m')

                UNION ALL

                -- 🟧 應付帳款（未付供應商款）
                SELECT 
                    DATE_FORMAT(p.purchase_date, '%Y-%m') AS accounting_period,
                    0 AS accounts_receivable,
                    (SUM(COALESCE(p.total_amount, 0)) - SUM(COALESCE(pay.amount, 0))) AS accounts_payable,
                    0 AS cash
                FROM purchases p
                LEFT JOIN payments pay ON p.id = pay.purchase_id AND pay.status = 'ACTIVE'
                WHERE p.record_status = 'ACTIVE'
        """);

        if (period != null && !period.isBlank()) {
            sql.append(" AND p.accounting_period = ? ");
        } else if (startDate != null && endDate != null) {
            sql.append(" AND p.purchase_date BETWEEN ? AND ? ");
        }

        sql.append("""
                GROUP BY DATE_FORMAT(p.purchase_date, '%Y-%m')

                UNION ALL

                -- 💵 現金（銷售 + 收款）
                SELECT 
                    DATE_FORMAT(s.sale_date, '%Y-%m') AS accounting_period,
                    0 AS accounts_receivable,
                    0 AS accounts_payable,
                    SUM(COALESCE(s.amount, 0)) AS cash
                FROM sales s
                WHERE s.pay_method IN ('CASH','MOBILE')
        """);

        if (period != null && !period.isBlank()) {
            sql.append(" AND s.accounting_period = ? ");
        } else if (startDate != null && endDate != null) {
            sql.append(" AND s.sale_date BETWEEN ? AND ? ");
        }

        sql.append("""
                GROUP BY DATE_FORMAT(s.sale_date, '%Y-%m')

                UNION ALL

                SELECT 
                    DATE_FORMAT(r.received_date, '%Y-%m') AS accounting_period,
                    0 AS accounts_receivable,
                    0 AS accounts_payable,
                    SUM(COALESCE(r.amount, 0)) AS cash
                FROM receipts r
                WHERE r.status = 'ACTIVE'
        """);

        if (period != null && !period.isBlank()) {
            sql.append(" AND r.accounting_period = ? ");
        } else if (startDate != null && endDate != null) {
            sql.append(" AND r.received_date BETWEEN ? AND ? ");
        }

        sql.append("""
                GROUP BY DATE_FORMAT(r.received_date, '%Y-%m')
            ) AS combined
            GROUP BY accounting_period
            ORDER BY accounting_period
        """);

        // 執行查詢（動態綁參）
        if (period != null && !period.isBlank()) {
            return jdbcTemplate.query(sql.toString(), this::mapRowToDto,
                    period, period, period, period);
        } else if (startDate != null && endDate != null) {
            return jdbcTemplate.query(sql.toString(), this::mapRowToDto,
                    startDate, endDate,
                    startDate, endDate,
                    startDate, endDate,
                    startDate, endDate);
        } else {
            // 沒有條件，查全部
            return jdbcTemplate.query(sql.toString(), this::mapRowToDto);
        }
    }

    private BalanceSheetReportDto mapRowToDto(ResultSet rs, int rowNum) throws SQLException {
        return BalanceSheetReportDto.builder()
                .accountingPeriod(rs.getString("accounting_period"))
                .accountsReceivable(getDecimal(rs, "accounts_receivable"))
                .accountsPayable(getDecimal(rs, "accounts_payable"))
                .cash(getDecimal(rs, "cash"))
                .totalAssets(getDecimal(rs, "total_assets"))
                .totalLiabilities(getDecimal(rs, "total_liabilities"))
                .equity(getDecimal(rs, "equity"))
                .build();
    }

    private BigDecimal getDecimal(ResultSet rs, String column) throws SQLException {
        BigDecimal v = rs.getBigDecimal(column);
        return v != null ? v : BigDecimal.ZERO;
    }
}
