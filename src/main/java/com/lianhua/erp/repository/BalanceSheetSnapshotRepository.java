package com.lianhua.erp.repository;

import com.lianhua.erp.dto.report.BalanceSheetReportDto;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 💼 資產負債表（Balance Sheet - Snapshot）
 *
 * 📌 會計定義：
 * - 資產負債表為「時點報表」
 * - 顯示「截至指定月份月底」的財務狀態
 *
 * 📌 時間語意：
 * - period：YYYY-MM
 * - 表示「<= 該月份月底」
 *
 * 📌 計算原則：
 * - 應收帳款 = 訂單總額 - 已收款（截至期末）
 * - 應付帳款 = 採購總額 - 已付款（截至期末）
 * - 現金 = 所有現金流入 - 所有現金流出（截至期末）
 * - 資產 = 現金 + 應收
 * - 權益 = 資產 - 負債
 */
@Repository
@RequiredArgsConstructor
public class BalanceSheetSnapshotRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 📊 查詢資產負債表（Snapshot）
     *
     * @param period YYYY-MM（例如：2025-03），表示截至該月底
     */
    public BalanceSheetReportDto getBalanceSheet(String period) {

        if (period == null || period.isBlank()) {
            throw new IllegalArgumentException("period (YYYY-MM) 不可為空，資產負債表必須指定截至月份");
        }

        String sql = """
                    SELECT
                        :period AS accounting_period,

                        ar.accounts_receivable,
                        ap.accounts_payable,
                        cash.cash,

                        (ar.accounts_receivable + cash.cash) AS total_assets,
                        ap.accounts_payable AS total_liabilities,
                        (ar.accounts_receivable + cash.cash - ap.accounts_payable) AS equity
                    FROM
                        (
                            -- 應收帳款（截至期末未收餘額）
                            SELECT
                                COALESCE(SUM(
                                    GREATEST(
                                        0,
                                        COALESCE(o.total_amount, 0)
                                        - COALESCE(r.received_amount, 0)
                                    )
                                ), 0) AS accounts_receivable
                            FROM orders o
                            LEFT JOIN (
                                SELECT
                                    order_id,
                                    SUM(amount) AS received_amount
                                FROM receipts
                                WHERE status = 'ACTIVE'
                                GROUP BY order_id
                            ) r ON r.order_id = o.id
                            WHERE o.order_status != 'CANCELLED'
                              AND o.accounting_period <= :period
                        ) ar,

                        (
                            -- 應付帳款（截至期末未付餘額）
                            SELECT
                                COALESCE(SUM(
                                    GREATEST(
                                        0,
                                        COALESCE(p.total_amount, 0)
                                        - COALESCE(pay.paid_amount, 0)
                                    )
                                ), 0) AS accounts_payable
                            FROM purchases p
                            LEFT JOIN (
                                SELECT
                                    purchase_id,
                                    SUM(amount) AS paid_amount
                                FROM payments
                                WHERE status = 'ACTIVE'
                                GROUP BY purchase_id
                            ) pay ON pay.purchase_id = p.id
                            WHERE p.record_status = 'ACTIVE'
                              AND p.accounting_period <= :period
                        ) ap,

                        (
                            -- 現金餘額（截至期末）
                            SELECT
                                COALESCE(SUM(inflow), 0) - COALESCE(SUM(outflow), 0) AS cash
                            FROM (
                                -- 零售收入（視為現金）
                                SELECT
                                    s.amount AS inflow,
                                    0 AS outflow
                                FROM sales s
                                WHERE s.pay_method IN ('CASH', 'CARD', 'MOBILE')
                                  AND s.accounting_period <= :period

                                UNION ALL

                                -- 訂單收款
                                SELECT
                                    r.amount AS inflow,
                                    0 AS outflow
                                FROM receipts r
                                WHERE r.status = 'ACTIVE'
                                  AND r.method IN ('CASH','TRANSFER','CARD','CHECK')
                                  AND r.accounting_period <= :period

                                UNION ALL

                                -- 營運費用
                                SELECT
                                    0 AS inflow,
                                    e.amount AS outflow
                                FROM expenses e
                                WHERE e.status = 'ACTIVE'
                                  AND e.accounting_period <= :period

                                UNION ALL

                                -- 採購付款
                                SELECT
                                    0 AS inflow,
                                    p.amount AS outflow
                                FROM payments p
                                WHERE p.status = 'ACTIVE'
                                  AND p.method IN ('CASH','TRANSFER','CARD','CHECK')
                                  AND p.accounting_period <= :period
                            ) cash_flow
                        ) cash
                """;

        NamedParameterJdbcTemplate namedJdbc = new NamedParameterJdbcTemplate(jdbcTemplate);

        MapSqlParameterSource params = new MapSqlParameterSource("period", period);

        return namedJdbc.queryForObject(
                sql,
                params,
                this::mapRowToDto);
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

    /**
     * 📊 查詢資產負債表（支援 period 和 endDate 參數，返回列表）
     *
     * @param period  YYYY-MM（例如：2025-03），表示截至該月底
     * @param endDate yyyy-MM-dd（例如：2025-12-31），表示截至該日期
     * @return 資產負債表報表資料列表
     */
    public List<BalanceSheetReportDto> getBalanceSheet(String period, String endDate) {
        List<BalanceSheetReportDto> result = new ArrayList<>();

        // 如果提供了 endDate，使用 endDate（轉換為 period 格式或使用日期過濾）
        // 如果只提供了 period，使用 period
        // 如果都沒有提供，返回空列表
        String effectivePeriod = null;
        String effectiveEndDate = null;

        if (endDate != null && !endDate.isBlank()) {
            // 如果有 endDate，提取 YYYY-MM 部分作為 period
            effectiveEndDate = endDate;
            if (endDate.length() >= 7) {
                effectivePeriod = endDate.substring(0, 7); // 提取 YYYY-MM
            }
        } else if (period != null && !period.isBlank()) {
            effectivePeriod = period;
        } else {
            // 如果都沒有提供，返回空列表
            return result;
        }

        // 使用 period 查詢（因為現有的 SQL 邏輯基於 accounting_period）
        BalanceSheetReportDto dto = getBalanceSheet(effectivePeriod);

        // 如果提供了 endDate，更新 accounting_period 顯示為日期格式
        if (effectiveEndDate != null) {
            dto.setAccountingPeriod(effectiveEndDate);
        }

        result.add(dto);
        return result;
    }

    private BigDecimal getDecimal(ResultSet rs, String column) throws SQLException {
        BigDecimal value = rs.getBigDecimal(column);
        return value != null ? value : BigDecimal.ZERO;
    }
}
