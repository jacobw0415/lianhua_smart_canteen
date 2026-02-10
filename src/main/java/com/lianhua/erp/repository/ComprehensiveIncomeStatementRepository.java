package com.lianhua.erp.repository;

import com.lianhua.erp.dto.report.ComprehensiveIncomeStatementDto;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 📊 綜合損益表 Repository
 * * 已修正重點：
 * 1. 強制 COLLATE 以解決 UNION 字符集衝突。
 * 2. 移除 sales 表中不存在的 status 欄位過濾。
 */
@Repository
@RequiredArgsConstructor
public class ComprehensiveIncomeStatementRepository {
    
    private final JdbcTemplate jdbcTemplate;
    
    public List<ComprehensiveIncomeStatementDto> getComprehensiveIncomeStatement(
            String period, String startDate, String endDate) {
        
        boolean usePeriod = period != null && !period.isBlank();
        boolean useDateRange = !usePeriod && startDate != null && endDate != null
                && !startDate.isBlank() && !endDate.isBlank();
        
        StringBuilder sql = new StringBuilder("""
            SELECT
                accounting_period,
                COALESCE(SUM(retail_sales), 0) AS retail_sales,
                COALESCE(SUM(order_sales), 0) AS order_sales,
                (COALESCE(SUM(retail_sales), 0) + COALESCE(SUM(order_sales), 0)) AS total_revenue,
                COALESCE(SUM(cost_of_goods_sold), 0) AS cost_of_goods_sold,
                ((COALESCE(SUM(retail_sales), 0) + COALESCE(SUM(order_sales), 0))
                 - COALESCE(SUM(cost_of_goods_sold), 0)) AS gross_profit
            FROM (
                -- 🟩 零售銷售收入 (修正：加上 COLLATE，確保 sales 沒有 status 欄位)
                SELECT
                    accounting_period COLLATE utf8mb4_unicode_ci AS accounting_period,
                    SUM(s.amount) AS retail_sales,
                    0 AS order_sales,
                    0 AS cost_of_goods_sold
                FROM sales s
                WHERE 1=1
        """);
        
        if (usePeriod) {
            sql.append(" AND s.accounting_period = ? ");
        } else if (useDateRange) {
            sql.append(" AND s.sale_date BETWEEN ? AND ? ");
        }
        
        sql.append("""
                GROUP BY accounting_period

                UNION ALL

                -- 🟦 訂單銷售收入 (修正：加上 COLLATE)
                SELECT
                    accounting_period COLLATE utf8mb4_unicode_ci AS accounting_period,
                    0 AS retail_sales,
                    SUM(o.total_amount) AS order_sales,
                    0 AS cost_of_goods_sold
                FROM orders o
                WHERE o.order_status != 'CANCELLED'
        """);
        
        if (usePeriod) {
            sql.append(" AND o.accounting_period = ? ");
        } else if (useDateRange) {
            sql.append(" AND o.order_date BETWEEN ? AND ? ");
        }
        
        sql.append("""
                GROUP BY accounting_period

                UNION ALL

                -- 🟥 採購成本 (修正：加上 COLLATE)
                SELECT
                    accounting_period COLLATE utf8mb4_unicode_ci AS accounting_period,
                    0 AS retail_sales,
                    0 AS order_sales,
                    SUM(p.total_amount) AS cost_of_goods_sold
                FROM purchases p
                WHERE p.record_status = 'ACTIVE'
        """);
        
        if (usePeriod) {
            sql.append(" AND p.accounting_period = ? ");
        } else if (useDateRange) {
            sql.append(" AND p.purchase_date BETWEEN ? AND ? ");
        }
        
        sql.append("""
                GROUP BY accounting_period
            ) AS combined
            GROUP BY accounting_period
            ORDER BY accounting_period;
        """);
        
        List<ComprehensiveIncomeStatementDto> result;
        if (usePeriod) {
            result = jdbcTemplate.query(sql.toString(), this::mapRowToBaseDto, period, period, period);
        } else if (useDateRange) {
            result = jdbcTemplate.query(sql.toString(), this::mapRowToBaseDto,
                    startDate, endDate, startDate, endDate, startDate, endDate);
        } else {
            result = jdbcTemplate.query(sql.toString(), this::mapRowToBaseDto);
        }
        
        // 填充費用與損益計算邏輯
        for (ComprehensiveIncomeStatementDto dto : result) {
            List<ComprehensiveIncomeStatementDto.ExpenseCategoryDetailDto> expenseDetails =
                    getExpenseDetailsByPeriod(dto.getAccountingPeriod(), startDate, endDate);
            dto.setExpenseDetails(expenseDetails);
            
            BigDecimal totalExpenses = expenseDetails.stream()
                    .map(ComprehensiveIncomeStatementDto.ExpenseCategoryDetailDto::getAmount)
                    .filter(amount -> amount != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            dto.setTotalOperatingExpenses(totalExpenses);
            
            BigDecimal grossProfit = dto.getGrossProfit() != null ? dto.getGrossProfit() : BigDecimal.ZERO;
            BigDecimal operatingProfit = grossProfit.subtract(totalExpenses);
            dto.setOperatingProfit(operatingProfit);
            
            dto.setOtherIncome(BigDecimal.ZERO);
            dto.setOtherExpenses(BigDecimal.ZERO);
            BigDecimal netProfit = operatingProfit.add(dto.getOtherIncome()).subtract(dto.getOtherExpenses());
            dto.setNetProfit(netProfit);
            dto.setOtherComprehensiveIncome(BigDecimal.ZERO);
            dto.setComprehensiveIncome(netProfit.add(dto.getOtherComprehensiveIncome()));
        }
        
        return result;
    }
    
    private List<ComprehensiveIncomeStatementDto.ExpenseCategoryDetailDto> getExpenseDetailsByPeriod(
            String period, String startDate, String endDate) {
        
        boolean usePeriod = period != null && !period.isBlank();
        boolean useDateRange = !usePeriod && startDate != null && endDate != null
                && !startDate.isBlank() && !endDate.isBlank();
        
        StringBuilder sql = new StringBuilder("""
            SELECT
                ec.id AS category_id,
                ec.name AS category_name,
                ec.account_code,
                ec.is_salary,
                COALESCE(SUM(e.amount), 0) AS amount
            FROM expense_categories ec
            LEFT JOIN expenses e ON ec.id = e.category_id
                AND e.status = 'ACTIVE'
        """);
        
        if (usePeriod) {
            sql.append(" AND e.accounting_period = ? ");
        } else if (useDateRange) {
            sql.append(" AND e.expense_date BETWEEN ? AND ? ");
        } else {
            sql.append(" AND 1=0 ");
        }
        
        sql.append("""
            WHERE ec.active = true
            GROUP BY ec.id, ec.name, ec.account_code, ec.is_salary
            HAVING amount > 0
            ORDER BY ec.account_code;
        """);
        
        if (usePeriod) {
            return jdbcTemplate.query(sql.toString(), this::mapExpenseDetailRow, period);
        } else if (useDateRange) {
            return jdbcTemplate.query(sql.toString(), this::mapExpenseDetailRow, startDate, endDate);
        } else {
            return new ArrayList<>();
        }
    }
    
    private ComprehensiveIncomeStatementDto mapRowToBaseDto(ResultSet rs, int rowNum) throws SQLException {
        ComprehensiveIncomeStatementDto dto = new ComprehensiveIncomeStatementDto();
        dto.setAccountingPeriod(rs.getString("accounting_period"));
        dto.setRetailSales(getDecimal(rs, "retail_sales"));
        dto.setOrderSales(getDecimal(rs, "order_sales"));
        dto.setTotalRevenue(getDecimal(rs, "total_revenue"));
        dto.setCostOfGoodsSold(getDecimal(rs, "cost_of_goods_sold"));
        dto.setGrossProfit(getDecimal(rs, "gross_profit"));
        return dto;
    }
    
    private ComprehensiveIncomeStatementDto.ExpenseCategoryDetailDto mapExpenseDetailRow(
            ResultSet rs, int rowNum) throws SQLException {
        return ComprehensiveIncomeStatementDto.ExpenseCategoryDetailDto.builder()
                .categoryId(rs.getLong("category_id"))
                .categoryName(rs.getString("category_name"))
                .accountCode(rs.getString("account_code"))
                .isSalary(rs.getBoolean("is_salary"))
                .amount(getDecimal(rs, "amount"))
                .build();
    }
    
    private BigDecimal getDecimal(ResultSet rs, String column) throws SQLException {
        BigDecimal v = rs.getBigDecimal(column);
        return v != null ? v : BigDecimal.ZERO;
    }
    
    public List<ComprehensiveIncomeStatementDto> getComprehensiveIncomeStatement(List<String> periods) {
        List<ComprehensiveIncomeStatementDto> result = new ArrayList<>();
        for (String period : periods) {
            result.addAll(getComprehensiveIncomeStatement(period, null, null));
        }
        return result;
    }
}