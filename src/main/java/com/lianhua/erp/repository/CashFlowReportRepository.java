package com.lianhua.erp.repository;

import com.lianhua.erp.dto.report.CashFlowReportDto;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class CashFlowReportRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 📊 查詢現金流量統計報表
     */
    public List<CashFlowReportDto> getCashFlow(String period, String startDate, String endDate) {

        StringBuilder sql = new StringBuilder("""
            SELECT
                accounting_period,
                COALESCE(SUM(total_sales), 0) AS total_sales,
                COALESCE(SUM(total_receipts), 0) AS total_receipts,
                COALESCE(SUM(total_payments), 0) AS total_payments,
                COALESCE(SUM(total_expenses), 0) AS total_expenses,
                (COALESCE(SUM(total_sales), 0) + COALESCE(SUM(total_receipts), 0)) AS total_inflow,
                (COALESCE(SUM(total_payments), 0) + COALESCE(SUM(total_expenses), 0)) AS total_outflow,
                ((COALESCE(SUM(total_sales), 0) + COALESCE(SUM(total_receipts), 0))
                 - (COALESCE(SUM(total_payments), 0) + COALESCE(SUM(total_expenses), 0))) AS net_cash_flow
            FROM (
                SELECT accounting_period, SUM(amount) AS total_sales, 0 AS total_receipts, 0 AS total_payments, 0 AS total_expenses
                  FROM sales
                 WHERE (? IS NULL OR sale_date >= ?) AND (? IS NULL OR sale_date <= ?)
                 GROUP BY accounting_period

                UNION ALL

                SELECT accounting_period, 0 AS total_sales, SUM(amount) AS total_receipts, 0 AS total_payments, 0 AS total_expenses
                  FROM receipts
                 WHERE status = 'ACTIVE'
                   AND (? IS NULL OR received_date >= ?) AND (? IS NULL OR received_date <= ?)
                 GROUP BY accounting_period

                UNION ALL

                SELECT accounting_period, 0 AS total_sales, 0 AS total_receipts, SUM(amount) AS total_payments, 0 AS total_expenses
                  FROM payments
                 WHERE (? IS NULL OR pay_date >= ?) AND (? IS NULL OR pay_date <= ?)
                 GROUP BY accounting_period

                UNION ALL

                SELECT accounting_period, 0 AS total_sales, 0 AS total_receipts, 0 AS total_payments, SUM(amount) AS total_expenses
                  FROM expenses
                 WHERE (? IS NULL OR expense_date >= ?) AND (? IS NULL OR expense_date <= ?)
                 GROUP BY accounting_period
            ) AS combined
        """);

        // 🔹 外層條件
        if (period != null && !period.isBlank()) {
            sql.append(" WHERE accounting_period = ? ");
        } else if (startDate != null && endDate != null) {
            sql.append(" WHERE accounting_period BETWEEN DATE_FORMAT(?, '%Y-%m') AND DATE_FORMAT(?, '%Y-%m') ");
        }

        // 🔹 最後再 group by + order
        sql.append(" GROUP BY accounting_period ORDER BY accounting_period; ");

        // 查詢執行
        if (period != null && !period.isBlank()) {
            return jdbcTemplate.query(sql.toString(), this::mapRowToDto,
                    startDate, startDate, endDate, endDate,
                    startDate, startDate, endDate, endDate,
                    startDate, startDate, endDate, endDate,
                    startDate, startDate, endDate, endDate,
                    period);
        } else if (startDate != null && endDate != null) {
            return jdbcTemplate.query(sql.toString(), this::mapRowToDto,
                    startDate, startDate, endDate, endDate,
                    startDate, startDate, endDate, endDate,
                    startDate, startDate, endDate, endDate,
                    startDate, startDate, endDate, endDate,
                    startDate, endDate);
        } else {
            return jdbcTemplate.query(sql.toString(), this::mapRowToDto,
                    startDate, startDate, endDate, endDate,
                    startDate, startDate, endDate, endDate,
                    startDate, startDate, endDate, endDate,
                    startDate, startDate, endDate, endDate);
        }
    }

    /**
     * 🧩 將 SQL 結果映射為 DTO
     */
    private CashFlowReportDto mapRowToDto(ResultSet rs, int rowNum) throws SQLException {
        CashFlowReportDto dto = new CashFlowReportDto();
        dto.setAccountingPeriod(rs.getString("accounting_period"));
        dto.setTotalSales(getDecimal(rs, "total_sales"));
        dto.setTotalReceipts(getDecimal(rs, "total_receipts"));
        dto.setTotalPayments(getDecimal(rs, "total_payments"));
        dto.setTotalExpenses(getDecimal(rs, "total_expenses"));
        dto.setTotalInflow(getDecimal(rs, "total_inflow"));
        dto.setTotalOutflow(getDecimal(rs, "total_outflow"));
        dto.setNetCashFlow(getDecimal(rs, "net_cash_flow"));
        return dto;
    }

    private BigDecimal getDecimal(ResultSet rs, String column) throws SQLException {
        BigDecimal value = rs.getBigDecimal(column);
        return value != null ? value : BigDecimal.ZERO;
    }
}
