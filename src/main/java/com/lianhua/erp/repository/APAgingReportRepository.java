package com.lianhua.erp.repository;

import com.lianhua.erp.dto.report.APAgingReportDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 💸 應付帳齡報表 Repository
 * 支援依供應商、最小逾期天數、會計期間查詢。
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class APAgingReportRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 查詢應付帳齡報表
     *
     * @param supplierId  供應商 ID（可為 null）
     * @param minOverdue  最小逾期天數（可為 null）
     * @param period      會計期間（格式：YYYY-MM，可為 null）
     */
    public List<APAgingReportDto> findAgingPayables(Long supplierId, Integer minOverdue, String period) {

        StringBuilder sql = new StringBuilder("""
            SELECT 
                s.name AS supplier_name,
                p.id AS purchase_id,
                DATE_FORMAT(p.purchase_date, '%Y-%m-%d') AS purchase_date,
                p.total_amount AS total_amount,
                COALESCE(SUM(pay.amount), 0) AS paid_amount,
                (p.total_amount - COALESCE(SUM(pay.amount), 0)) AS balance,
                DATEDIFF(CURDATE(), p.purchase_date) AS days_overdue,
                CASE
                    WHEN DATEDIFF(CURDATE(), p.purchase_date) <= 30 THEN '0–30天'
                    WHEN DATEDIFF(CURDATE(), p.purchase_date) <= 60 THEN '31–60天'
                    WHEN DATEDIFF(CURDATE(), p.purchase_date) <= 90 THEN '61–90天'
                    ELSE '90天以上'
                END AS aging_bucket
            FROM purchases p
            JOIN suppliers s ON p.supplier_id = s.id
            LEFT JOIN payments pay ON pay.purchase_id = p.id
            WHERE (p.total_amount - COALESCE(
                    (SELECT SUM(pay2.amount) FROM payments pay2 WHERE pay2.purchase_id = p.id), 0)
                  ) > 0
        """);

        // ✅ 動態條件組合
        List<Object> params = new ArrayList<>();

        if (supplierId != null) {
            sql.append(" AND p.supplier_id = ? ");
            params.add(supplierId);
        }

        if (minOverdue != null) {
            sql.append(" AND DATEDIFF(CURDATE(), p.purchase_date) >= ? ");
            params.add(minOverdue);
        }

        if (period != null && !period.isBlank()) {
            sql.append(" AND p.accounting_period = ? ");
            params.add(period);
        }

        sql.append("""
            GROUP BY p.id, s.name, p.purchase_date, p.total_amount
            ORDER BY days_overdue DESC
        """);

        log.debug("📄 Executing AP Aging SQL:\n{}", sql);
        log.debug("📦 Params: {}", Arrays.toString(params.toArray()));

        return jdbcTemplate.query(sql.toString(), this::mapRowToDto, params.toArray());
    }

    private APAgingReportDto mapRowToDto(ResultSet rs, int rowNum) throws SQLException {
        return APAgingReportDto.builder()
                .supplierName(rs.getString("supplier_name"))
                .purchaseId(rs.getLong("purchase_id"))
                .purchaseDate(rs.getString("purchase_date"))
                .totalAmount(getDecimal(rs, "total_amount"))
                .paidAmount(getDecimal(rs, "paid_amount"))
                .balance(getDecimal(rs, "balance"))
                .daysOverdue(rs.getInt("days_overdue"))
                .agingBucket(rs.getString("aging_bucket"))
                .build();
    }

    private BigDecimal getDecimal(ResultSet rs, String column) throws SQLException {
        BigDecimal value = rs.getBigDecimal(column);
        return value != null ? value : BigDecimal.ZERO;
    }
}
