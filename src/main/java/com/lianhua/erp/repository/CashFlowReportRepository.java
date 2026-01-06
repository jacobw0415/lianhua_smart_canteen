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
     * 
     * 支援三種查詢模式：
     * 1. 指定月份（period）：使用 accounting_period 精確匹配
     * 2. 日期區間（startDate ~ endDate）：使用日期範圍過濾
     * 3. 全部資料：不添加任何過濾條件
     */
    public List<CashFlowReportDto> getCashFlow(String period, String startDate, String endDate) {

        // 判斷查詢模式
        boolean usePeriod = period != null && !period.isBlank();
        boolean useDateRange = !usePeriod && startDate != null && endDate != null 
                               && !startDate.isBlank() && !endDate.isBlank();

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
                -- 🟩 零售現金收入 (Sales)
                SELECT accounting_period, SUM(amount) AS total_sales, 0 AS total_receipts, 0 AS total_payments, 0 AS total_expenses
                  FROM sales
                 WHERE 1=1
        """);

        // 動態添加 Sales 表的過濾條件
        if (usePeriod) {
            sql.append(" AND accounting_period = ? ");
        } else if (useDateRange) {
            sql.append(" AND sale_date BETWEEN ? AND ? ");
        }

        sql.append("""
                 GROUP BY accounting_period

                UNION ALL

                -- 🟦 訂單收款收入 (Receipts)
                SELECT accounting_period, 0 AS total_sales, SUM(amount) AS total_receipts, 0 AS total_payments, 0 AS total_expenses
                  FROM receipts
                 WHERE status = 'ACTIVE'
        """);

        // 動態添加 Receipts 表的過濾條件
        if (usePeriod) {
            sql.append(" AND accounting_period = ? ");
        } else if (useDateRange) {
            sql.append(" AND received_date BETWEEN ? AND ? ");
        }

        sql.append("""
                 GROUP BY accounting_period

                UNION ALL

                -- 🟧 採購付款支出 (Payments)
                SELECT accounting_period, 0 AS total_sales, 0 AS total_receipts, SUM(amount) AS total_payments, 0 AS total_expenses
                  FROM payments
                 WHERE status = 'ACTIVE'
        """);

        // 動態添加 Payments 表的過濾條件
        if (usePeriod) {
            sql.append(" AND accounting_period = ? ");
        } else if (useDateRange) {
            sql.append(" AND pay_date BETWEEN ? AND ? ");
        }

        sql.append("""
                 GROUP BY accounting_period

                UNION ALL

                -- 🟨 營運費用支出 (Expenses)
                SELECT accounting_period, 0 AS total_sales, 0 AS total_receipts, 0 AS total_payments, SUM(amount) AS total_expenses
                  FROM expenses
                 WHERE status = 'ACTIVE'
        """);

        // 動態添加 Expenses 表的過濾條件
        if (usePeriod) {
            sql.append(" AND accounting_period = ? ");
        } else if (useDateRange) {
            sql.append(" AND expense_date BETWEEN ? AND ? ");
        }

        sql.append("""
                 GROUP BY accounting_period
            ) AS combined
        """);

        // 🔹 外層條件（如果需要進一步過濾 accounting_period）
        if (usePeriod) {
            sql.append(" WHERE accounting_period = ? ");
        } else if (useDateRange) {
            sql.append(" WHERE accounting_period BETWEEN DATE_FORMAT(?, '%Y-%m') AND DATE_FORMAT(?, '%Y-%m') ");
        }

        // 🔹 最後再 group by + order
        sql.append(" GROUP BY accounting_period ORDER BY accounting_period ");

        // 查詢執行 - 根據查詢模式綁定參數
        if (usePeriod) {
            // 使用 period 查詢：每個子查詢都需要 period 參數，外層也需要
            return jdbcTemplate.query(sql.toString(), this::mapRowToDto,
                    period,  // sales
                    period,  // receipts
                    period,  // payments
                    period,  // expenses
                    period); // 外層
        } else if (useDateRange) {
            // 使用日期區間查詢：每個子查詢都需要 startDate 和 endDate，外層也需要
            return jdbcTemplate.query(sql.toString(), this::mapRowToDto,
                    startDate, endDate,  // sales
                    startDate, endDate,  // receipts
                    startDate, endDate,  // payments
                    startDate, endDate,  // expenses
                    startDate, endDate); // 外層
        } else {
            // 查詢全部資料：不需要任何參數
            return jdbcTemplate.query(sql.toString(), this::mapRowToDto);
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
