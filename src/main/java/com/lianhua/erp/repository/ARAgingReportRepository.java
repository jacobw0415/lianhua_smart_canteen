package com.lianhua.erp.repository;

import com.lianhua.erp.dto.report.ARAgingReportDto;
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
 * 💰 應收帳齡報表 Repository
 * 支援依客戶、最小逾期天數、會計期間查詢。
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class ARAgingReportRepository {

    private final JdbcTemplate jdbcTemplate;

    public List<ARAgingReportDto> findAgingReceivables(Long customerId, Integer minOverdue, String period) {

        StringBuilder sql = new StringBuilder("""
            SELECT 
                c.name AS customer_name,
                o.id AS order_id,
                DATE_FORMAT(o.order_date, '%Y-%m-%d') AS order_date,
                DATE_FORMAT(o.delivery_date, '%Y-%m-%d') AS delivery_date,
                o.total_amount AS total_amount,
                COALESCE(SUM(r.amount), 0) AS received_amount,
                (o.total_amount - COALESCE(SUM(r.amount), 0)) AS balance,
                DATEDIFF(CURDATE(), o.delivery_date) AS days_overdue,
                CASE
                    WHEN DATEDIFF(CURDATE(), o.delivery_date) <= 30 THEN '0–30天'
                    WHEN DATEDIFF(CURDATE(), o.delivery_date) <= 60 THEN '31–60天'
                    WHEN DATEDIFF(CURDATE(), o.delivery_date) <= 90 THEN '61–90天'
                    ELSE '90天以上'
                END AS aging_bucket
            FROM orders o
            JOIN order_customers c ON o.customer_id = c.id
            LEFT JOIN receipts r ON o.id = r.order_id AND r.status = 'ACTIVE'
            WHERE (o.total_amount - COALESCE((SELECT SUM(r2.amount) 
                                             FROM receipts r2 
                                             WHERE r2.order_id = o.id AND r2.status = 'ACTIVE'), 0)) > 0
        """);

        // ✅ 動態條件組合
        List<Object> params = new ArrayList<>();

        if (customerId != null) {
            sql.append(" AND o.customer_id = ? ");
            params.add(customerId);
        }

        if (minOverdue != null) {
            sql.append(" AND DATEDIFF(CURDATE(), o.delivery_date) >= ? ");
            params.add(minOverdue);
        }

        if (period != null && !period.isBlank()) {
            sql.append(" AND o.accounting_period = ? ");
            params.add(period);
        }

        sql.append("""
            GROUP BY o.id, c.name, o.order_date, o.delivery_date, o.total_amount
            ORDER BY days_overdue DESC
        """);

        // 🧠 Debug log for troubleshooting
        log.debug("🧾 Executing SQL:\n{}", sql);
        log.debug("📦 Params: {}", Arrays.toString(params.toArray()));

        // ✅ 執行查詢
        return jdbcTemplate.query(sql.toString(), this::mapRowToDto, params.toArray());
    }

    private ARAgingReportDto mapRowToDto(ResultSet rs, int rowNum) throws SQLException {
        return ARAgingReportDto.builder()
                .customerName(rs.getString("customer_name"))
                .orderId(rs.getLong("order_id"))
                .orderDate(rs.getString("order_date"))
                .deliveryDate(rs.getString("delivery_date"))
                .totalAmount(getDecimal(rs, "total_amount"))
                .receivedAmount(getDecimal(rs, "received_amount"))
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
