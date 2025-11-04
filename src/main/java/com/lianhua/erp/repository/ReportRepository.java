package com.lianhua.erp.repository;

import com.lianhua.erp.dto.report.ProfitReportDto;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * 📊 損益表 Repository
 * 支援以月份或日期區間查詢。
 */
@Repository
@RequiredArgsConstructor
public class ReportRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 取得損益報表（依月份或日期區間彙總）
     *
     * @param period    會計期間 (YYYY-MM)
     * @param startDate 起始日期 (yyyy-MM-dd)
     * @param endDate   結束日期 (yyyy-MM-dd)
     */
    public List<ProfitReportDto> getProfitReport(String period, String startDate, String endDate) {

        StringBuilder sql = new StringBuilder("""
            SELECT
                accounting_period,
                COALESCE(SUM(total_sales), 0) AS total_sales,
                COALESCE(SUM(total_orders), 0) AS total_orders,
                (COALESCE(SUM(total_sales), 0) + COALESCE(SUM(total_orders), 0)) AS total_revenue,
                COALESCE(SUM(total_purchase), 0) AS total_purchase,
                COALESCE(SUM(total_expense), 0) AS total_expense,
                ((COALESCE(SUM(total_sales), 0) + COALESCE(SUM(total_orders), 0))
                 - COALESCE(SUM(total_purchase), 0) - COALESCE(SUM(total_expense), 0)) AS net_profit
            FROM (
                -- 🟩 銷售（零售收入）
                SELECT 
                    DATE_FORMAT(s.sale_date, '%Y-%m') AS accounting_period,
                    SUM(s.amount) AS total_sales,
                    0 AS total_orders,
                    0 AS total_purchase,
                    0 AS total_expense
                FROM sales s
                WHERE 1=1
        """);

        // 動態條件 - 銷售
        if (period != null && !period.isBlank()) {
            sql.append(" AND s.accounting_period = ? ");
        } else if (startDate != null && endDate != null) {
            sql.append(" AND s.sale_date BETWEEN ? AND ? ");
        }

        sql.append("""
                GROUP BY DATE_FORMAT(s.sale_date, '%Y-%m')

                UNION ALL

                -- 🟦 訂單（批發收入）
                SELECT 
                    DATE_FORMAT(o.order_date, '%Y-%m') AS accounting_period,
                    0 AS total_sales,
                    SUM(o.total_amount) AS total_orders,
                    0 AS total_purchase,
                    0 AS total_expense
                FROM orders o
                WHERE 1=1
        """);

        // 動態條件 - 訂單
        if (period != null && !period.isBlank()) {
            sql.append(" AND o.accounting_period = ? ");
        } else if (startDate != null && endDate != null) {
            sql.append(" AND o.order_date BETWEEN ? AND ? ");
        }

        sql.append("""
                GROUP BY DATE_FORMAT(o.order_date, '%Y-%m')

                UNION ALL

                -- 🟥 採購（成本支出）
                SELECT 
                    DATE_FORMAT(p.purchase_date, '%Y-%m') AS accounting_period,
                    0 AS total_sales,
                    0 AS total_orders,
                    SUM(p.total_amount) AS total_purchase,
                    0 AS total_expense
                FROM purchases p
                WHERE 1=1
        """);

        // 動態條件 - 採購
        if (period != null && !period.isBlank()) {
            sql.append(" AND p.accounting_period = ? ");
        } else if (startDate != null && endDate != null) {
            sql.append(" AND p.purchase_date BETWEEN ? AND ? ");
        }

        sql.append("""
                GROUP BY DATE_FORMAT(p.purchase_date, '%Y-%m')

                UNION ALL

                -- 🟨 營運費用
                SELECT 
                    DATE_FORMAT(e.expense_date, '%Y-%m') AS accounting_period,
                    0 AS total_sales,
                    0 AS total_orders,
                    0 AS total_purchase,
                    SUM(e.amount) AS total_expense
                FROM expenses e
                WHERE 1=1
        """);

        // 動態條件 - 費用
        if (period != null && !period.isBlank()) {
            sql.append(" AND e.accounting_period = ? ");
        } else if (startDate != null && endDate != null) {
            sql.append(" AND e.expense_date BETWEEN ? AND ? ");
        }

        sql.append("""
                GROUP BY DATE_FORMAT(e.expense_date, '%Y-%m')
            ) AS combined
            GROUP BY accounting_period
            ORDER BY accounting_period;
        """);

        // 綁定參數（對應動態條件）
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
            return jdbcTemplate.query(sql.toString(), this::mapRowToDto);
        }
    }

    private ProfitReportDto mapRowToDto(ResultSet rs, int rowNum) throws SQLException {
        ProfitReportDto dto = new ProfitReportDto();
        dto.setAccountingPeriod(rs.getString("accounting_period"));
        dto.setTotalSales(getDecimal(rs, "total_sales"));
        dto.setTotalOrders(getDecimal(rs, "total_orders"));
        dto.setTotalRevenue(getDecimal(rs, "total_revenue"));
        dto.setTotalPurchase(getDecimal(rs, "total_purchase"));
        dto.setTotalExpense(getDecimal(rs, "total_expense"));
        dto.setNetProfit(getDecimal(rs, "net_profit"));
        return dto;
    }

    private BigDecimal getDecimal(ResultSet rs, String column) throws SQLException {
        BigDecimal v = rs.getBigDecimal(column);
        return v != null ? v : BigDecimal.ZERO;
    }
}
