package com.lianhua.erp.repository;

import com.lianhua.erp.dto.report.APSummaryReportDto;
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
 * 📊 應付帳款財務總表 Repository (AP Summary - Financial View)
 *
 * <p>適用架構：Lianhua ERP Schema v2.5 / v2.6
 * <p>資料來源：purchases (採購進貨主表)
 *
 * <p>計算原則：
 * - 總應付 (Total Payable)    = 有效進貨單的總金額 (record_status = 'ACTIVE')
 * - 已付總額 (Total Paid)     = 有效進貨單的已付金額 (paid_amount)
 * - 未付餘額 (Total Outstanding) = 有效進貨單的剩餘未付金額 (balance)
 *
 * 注意：
 * 1. 排除 record_status = 'VOIDED' (已作廢) 的單據。
 * 2. status 欄位 (PENDING/PARTIAL/PAID) 可用於輔助分析，但金額計算直接取數值欄位較準確。
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class APSummaryReportRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 取得指定期間/截止日的應付帳款匯總
     *
     * @param period  會計期間 (格式: YYYY-MM)，例如 "2026-01"
     * @param endDate (選填) 截止日期 (格式: YYYY-MM-DD)。若有值，則以該日為準；若無值，則取該月最後一天。
     */
    public APSummaryReportDto getSummary(String period, String endDate) {
        if (period == null || period.isBlank()) {
            throw new IllegalArgumentException("period (YYYY-MM) 不可為空，請指定會計期間");
        }

        String displayPeriod = (endDate != null && !endDate.isBlank()) ? endDate : period;

        // 🟢 修正後的 SQL：完全對應 purchases 表結構
        String sql = """
            SELECT
                :displayPeriod AS accounting_period,
                
                /* 1. 總應付 (Total Payable) - 所有有效進貨單總額 */
                COALESCE(SUM(p.total_amount), 0) AS total_payable,
                
                /* 2. 已付總額 (Total Paid) - 所有有效進貨單的已付金額 */
                COALESCE(SUM(p.paid_amount), 0) AS total_paid,
                
                /* 3. 未付餘額 (Total Outstanding) - 所有有效進貨單的剩餘欠款 (balance) */
                /* 註：balance 是生成欄位 (total - paid)，直接加總即可 */
                COALESCE(SUM(p.balance), 0) AS total_outstanding
            
            FROM purchases p
            WHERE 
              /* 排除已作廢的採購單 */
              p.record_status = 'ACTIVE'
              
              /* 日期過濾條件 */
              %s
            """;

        // 動態組裝日期條件
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
            log.error("查詢應付帳款總表發生錯誤 period={}, endDate={}", period, endDate, e);
            // 回傳空物件避免前端報錯
            return new APSummaryReportDto(displayPeriod, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }
    }

    /**
     * 取得單一區間的列表封裝 (方便前端一致性處理)
     */
    public List<APSummaryReportDto> getSummaryList(String period, String endDate) {
        List<APSummaryReportDto> result = new ArrayList<>();
        result.add(getSummary(period, endDate));
        return result;
    }

    /**
     * 批次取得多個月份的數據 (用於繪製趨勢圖)
     */
    public List<APSummaryReportDto> getSummaryList(List<String> periods) {
        List<APSummaryReportDto> result = new ArrayList<>();
        if (periods == null || periods.isEmpty()) {
            return result;
        }
        for (String period : periods) {
            if (period != null && !period.isBlank()) {
                try {
                    // endDate 傳入 null，代表查詢該月整月 (直到月底)
                    APSummaryReportDto dto = getSummary(period, null);
                    result.add(dto);
                } catch (Exception ex) {
                    log.error("查詢期間 {} 失敗: {}", period, ex.getMessage());
                }
            }
        }
        return result;
    }

    /* =============================================================
     * Private Helpers
     * ============================================================= */

    private APSummaryReportDto mapRowToDto(ResultSet rs, int rowNum) throws SQLException {
        return APSummaryReportDto.builder()
                .accountingPeriod(rs.getString("accounting_period"))
                .totalPayable(getDecimal(rs, "total_payable"))
                .totalPaid(getDecimal(rs, "total_paid"))
                .totalOutstanding(getDecimal(rs, "total_outstanding"))
                .build();
    }

    private BigDecimal getDecimal(ResultSet rs, String column) throws SQLException {
        BigDecimal value = rs.getBigDecimal(column);
        return value != null ? value : BigDecimal.ZERO;
    }

    private String buildDateFilter(String endDate, String period) {
        // 使用 purchase_date (進貨日期) 作為基準
        if (endDate != null && !endDate.isBlank()) {
            return "AND p.purchase_date <= :endDate";
        }
        // 使用 MySQL LAST_DAY 函數確保包含該月最後一天
        return "AND p.purchase_date <= LAST_DAY(STR_TO_DATE(CONCAT(:period, '-01'), '%Y-%m-%d'))";
    }
}