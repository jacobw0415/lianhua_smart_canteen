package com.lianhua.erp.repository;

import com.lianhua.erp.dto.report.ARSummaryReportDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 * 📊 應收帳款財務總表 Repository (修正版)
 *
 * <p>適用架構：Lianhua ERP v2.5 (Simple Payment Status)
 * <p>計算原則：
 * - 總應收 (Total Receivable) = 該期間所有有效訂單總額
 * - 未收餘額 (Outstanding)    = 該期間 payment_status = 'UNPAID' 的訂單
 * - 已收總額 (Received)       = 總應收 - 未收餘額 (或是 payment_status = 'PAID')
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class ARSummaryReportRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 取得指定期間的應收帳款匯總
     * @param period  會計期間 (格式: YYYY-MM)
     * @param endDate (選填) 截止日期
     */
    public ARSummaryReportDto getSummary(String period, String endDate) {
        // 參數檢查
        if (period == null || period.isBlank()) {
            throw new IllegalArgumentException("period (YYYY-MM) 不可為空");
        }
        String displayPeriod = (endDate != null && !endDate.isBlank()) ? endDate : period;

        // 🟢 修正後的 SQL：不 Join receipt 表，直接統計 orders
        // 使用 CASE WHEN 來區分「應收」與「已收」
        String sql = """
            SELECT
                :displayPeriod AS accounting_period,
                
                /* 1. 總應收 (所有訂單) */
                COALESCE(SUM(total_amount), 0) AS total_receivable,
                
                /* 2. 已收總額 (狀態為 PAID 的總額) */
                COALESCE(SUM(CASE WHEN payment_status = 'PAID' THEN total_amount ELSE 0 END), 0) AS total_received,
                
                /* 3. 未收餘額 (狀態為 UNPAID 的總額) */
                COALESCE(SUM(CASE WHEN payment_status = 'UNPAID' THEN total_amount ELSE 0 END), 0) AS total_outstanding
            
            FROM orders o
            WHERE 1=1
              /* 排除軟刪除 (若未來有 deleted_at 則啟用下一行) */
              -- AND o.deleted_at IS NULL 
              
              /* 日期過濾條件 */
              %s
            """;

        // 動態組裝日期條件 (取代原本複雜的 helper)
        String dateFilter = buildDateFilter(endDate, period);
        String finalSql = String.format(sql, dateFilter);

        NamedParameterJdbcTemplate namedJdbc = new NamedParameterJdbcTemplate(jdbcTemplate);
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("period", period);
        params.addValue("displayPeriod", displayPeriod);
        if (endDate != null && !endDate.isBlank()) {
            params.addValue("endDate", endDate);
        }

        try {
            return namedJdbc.queryForObject(finalSql, params, this::mapRowToDto);
        } catch (Exception e) {
            log.error("查詢應收帳款失敗 period={}", period, e);
            return new ARSummaryReportDto(displayPeriod, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }
    }

    // --- 輔助方法 ---

    private ARSummaryReportDto mapRowToDto(ResultSet rs, int rowNum) throws SQLException {
        return ARSummaryReportDto.builder()
                .accountingPeriod(rs.getString("accounting_period"))
                .totalReceivable(rs.getBigDecimal("total_receivable"))
                .totalReceived(rs.getBigDecimal("total_received"))
                .totalOutstanding(rs.getBigDecimal("total_outstanding"))
                .build();
    }

    // 簡化的日期過濾器
    private String buildDateFilter(String endDate, String period) {
        // 如果有指定特定截止日 (例如: 2026-01-08)
        if (endDate != null && !endDate.isBlank()) {
            return "AND o.created_at <= :endDate"; // 假設訂單時間欄位是 created_at 或 order_date
        }
        // 否則預設為該月月底 (使用 MySQL LAST_DAY)
        // 注意：這裡假設您的 DB 有 order_date 或是 created_at，請依實際欄位調整
        return "AND o.created_at <= LAST_DAY(STR_TO_DATE(CONCAT(:period, '-01'), '%Y-%m-%d'))";
    }

    // 保留原本的 List 包裝方法，方便前端呼叫
    public List<ARSummaryReportDto> getSummaryList(String period, String endDate) {
        List<ARSummaryReportDto> list = new ArrayList<>();
        list.add(getSummary(period, endDate));
        return list;
    }
}