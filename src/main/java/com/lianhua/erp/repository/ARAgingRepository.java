package com.lianhua.erp.repository;

import com.lianhua.erp.dto.ar.ARAgingFilterDto;
import com.lianhua.erp.dto.ar.ARAgingOrderDetailDto;
import com.lianhua.erp.dto.ar.ARAgingSummaryDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
@Slf4j
public class ARAgingRepository {

    private final JdbcTemplate jdbcTemplate;

    /* =============================================================
     * ① Summary（不分頁）
     * ============================================================= */
    public List<ARAgingSummaryDto> findAgingSummary() {
        String sql = baseSummarySql()
                + " HAVING SUM(o.total_amount - COALESCE(received_summary.received_amount, 0)) > 0 "
                + " ORDER BY balance DESC ";
        return jdbcTemplate.query(sql, this::mapSummaryRow);
    }

    /* =============================================================
     * ② Summary（分頁 + 搜尋）
     * ============================================================= */
    public Page<ARAgingSummaryDto> findAgingSummaryPaged(
            ARAgingFilterDto filter,
            int page,
            int size
    ) {

        int offset = page * size;

        String baseSql = baseSummarySql();

        // ⭐ 關鍵修正：在 GROUP BY 前插入 WHERE 條件
        int groupByIndex = baseSql.lastIndexOf("GROUP BY");

        StringBuilder sql = new StringBuilder();
        List<Object> params = new ArrayList<>();

        if (groupByIndex > -1) {
            sql.append(baseSql, 0, groupByIndex);

            /* ===============================
             * WHERE（非聚合條件）
             * =============================== */
            if (filter != null && StringUtils.hasText(filter.getCustomerName())) {
                sql.append(" AND c.name LIKE ? ");
                params.add("%" + filter.getCustomerName().trim() + "%");
            }

            // 接回 GROUP BY
            sql.append(baseSql.substring(groupByIndex));
        } else {
            // fallback（理論上不會進）
            sql.append(baseSql);
        }

        /* ===============================
         * HAVING（聚合條件）
         * =============================== */
        List<String> havingConditions = new ArrayList<>();

        if (filter != null && filter.getAgingBucket() != null) {
            switch (filter.getAgingBucket()) {
                case "DAYS_0_30" -> havingConditions.add("""
                            SUM(CASE WHEN DATEDIFF(CURDATE(), o.delivery_date) <= 30
                            THEN (o.total_amount - COALESCE(received_summary.received_amount, 0)) ELSE 0 END) > 0
                        """);
                case "DAYS_31_60" -> havingConditions.add("""
                            SUM(CASE WHEN DATEDIFF(CURDATE(), o.delivery_date) BETWEEN 31 AND 60
                            THEN (o.total_amount - COALESCE(received_summary.received_amount, 0)) ELSE 0 END) > 0
                        """);
                case "DAYS_60_PLUS" -> havingConditions.add("""
                            SUM(CASE WHEN DATEDIFF(CURDATE(), o.delivery_date) > 60
                            THEN (o.total_amount - COALESCE(received_summary.received_amount, 0)) ELSE 0 END) > 0
                        """);
                default -> {
                }
            }
        }

        if (filter == null || filter.getOnlyUnpaid() == null || Boolean.TRUE.equals(filter.getOnlyUnpaid())) {
            havingConditions.add(" SUM(o.total_amount - COALESCE(received_summary.received_amount, 0)) > 0 ");
        }

        if (!havingConditions.isEmpty()) {
            sql.append(" HAVING ");
            sql.append(String.join(" AND ", havingConditions));
        }

        /* ===============================
         * 排序 + 分頁
         * =============================== */
        sql.append(" ORDER BY balance DESC ");
        String countBaseSql = sql.toString();

        sql.append(" LIMIT ? OFFSET ? ");
        params.add(size);
        params.add(offset);

        List<ARAgingSummaryDto> content =
                jdbcTemplate.query(sql.toString(), this::mapSummaryRow, params.toArray());

        /* ===============================
         * Count SQL
         * =============================== */
        String countSql = """
                SELECT COUNT(*) FROM (
                """ + countBaseSql + """
                ) t
                """;

        Integer total = jdbcTemplate.queryForObject(
                countSql,
                Integer.class,
                params.subList(0, params.size() - 2).toArray()
        );

        return new PageImpl<>(content, PageRequest.of(page, size), total);
    }

    /* =============================================================
     * 共用 Summary SQL（骨架）
     * ============================================================= */
    private String baseSummarySql() {

        return """
                SELECT 
                    c.id AS customer_id,
                    c.name AS customer_name,
                
                    /* ---- Aging bucket 計算 ---- */
                    SUM(
                        CASE 
                            WHEN DATEDIFF(CURDATE(), o.delivery_date) <= 30 
                            THEN (o.total_amount - COALESCE(received_summary.received_amount, 0))
                            ELSE 0 
                        END
                    ) AS aging_0_30,
                
                    SUM(
                        CASE 
                            WHEN DATEDIFF(CURDATE(), o.delivery_date) BETWEEN 31 AND 60
                            THEN (o.total_amount - COALESCE(received_summary.received_amount, 0))
                            ELSE 0 
                        END
                    ) AS aging_31_60,
                
                    SUM(
                        CASE 
                            WHEN DATEDIFF(CURDATE(), o.delivery_date) > 60
                            THEN (o.total_amount - COALESCE(received_summary.received_amount, 0))
                            ELSE 0 
                        END
                    ) AS aging_60_plus,
                
                    /* ---- 應收總額、已收、未收 ---- */
                    SUM(o.total_amount) AS total_amount,
                    SUM(COALESCE(received_summary.received_amount, 0)) AS received_amount,
                    SUM(o.total_amount - COALESCE(received_summary.received_amount, 0)) AS balance
                
                FROM orders o
                JOIN order_customers c ON c.id = o.customer_id
                LEFT JOIN (
                    SELECT 
                        order_id,
                        SUM(amount) AS received_amount
                    FROM receipts
                    WHERE status = 'ACTIVE'
                    GROUP BY order_id
                ) received_summary ON o.id = received_summary.order_id
                
                WHERE o.order_status != 'CANCELLED'
                  AND NOT (o.payment_status = 'PAID' AND COALESCE(received_summary.received_amount, 0) = 0)
                
                GROUP BY c.id, c.name
                """;
    }

    private ARAgingSummaryDto mapSummaryRow(ResultSet rs, int rowNum) throws SQLException {
        return ARAgingSummaryDto.builder()
                .id(rs.getLong("customer_id"))
                .customerId(rs.getLong("customer_id"))
                .customerName(rs.getString("customer_name"))
                .aging0to30(getDecimal(rs, "aging_0_30"))
                .aging31to60(getDecimal(rs, "aging_31_60"))
                .aging60plus(getDecimal(rs, "aging_60_plus"))
                .totalAmount(getDecimal(rs, "total_amount"))
                .receivedAmount(getDecimal(rs, "received_amount"))
                .balance(getDecimal(rs, "balance"))
                .build();
    }

    /* =============================================================
     * ③ Detail — 指定客戶未收訂單明細
     * ============================================================= */
    public List<ARAgingOrderDetailDto> findOrdersByCustomerId(Long customerId) {

        String sql = """
                SELECT
                    o.id AS order_id,
                    o.order_no,
                    o.order_date,
                    o.delivery_date,
                    o.total_amount,
                    COALESCE(received_summary.received_amount, 0) AS received_amount,
                    (o.total_amount - COALESCE(received_summary.received_amount, 0)) AS balance,
                
                    CASE
                        WHEN DATEDIFF(CURDATE(), o.delivery_date) <= 30 THEN '0–30'
                        WHEN DATEDIFF(CURDATE(), o.delivery_date) BETWEEN 31 AND 60 THEN '31–60'
                        ELSE '>60'
                    END AS aging_bucket,
                    
                    DATEDIFF(CURDATE(), o.delivery_date) AS days_overdue
                
                FROM orders o
                LEFT JOIN (
                    SELECT 
                        order_id,
                        SUM(amount) AS received_amount
                    FROM receipts
                    WHERE status = 'ACTIVE'
                    GROUP BY order_id
                ) received_summary ON o.id = received_summary.order_id
                WHERE o.customer_id = ?
                  AND o.order_status != 'CANCELLED'
                  AND NOT (o.payment_status = 'PAID' AND COALESCE(received_summary.received_amount, 0) = 0)
                  AND (o.total_amount - COALESCE(received_summary.received_amount, 0)) > 0
                ORDER BY o.delivery_date DESC
                """;

        return jdbcTemplate.query(sql, this::mapDetailRow, customerId);
    }

    private ARAgingOrderDetailDto mapDetailRow(ResultSet rs, int rowNum) throws SQLException {
        return ARAgingOrderDetailDto.builder()
                .id(rs.getLong("order_id"))
                .orderId(rs.getLong("order_id"))
                .orderNo(rs.getString("order_no"))
                .orderDate(rs.getDate("order_date") != null ? rs.getDate("order_date").toLocalDate() : null)
                .deliveryDate(rs.getDate("delivery_date") != null ? rs.getDate("delivery_date").toLocalDate() : null)
                .totalAmount(getDecimal(rs, "total_amount"))
                .receivedAmount(getDecimal(rs, "received_amount"))
                .balance(getDecimal(rs, "balance"))
                .agingBucket(rs.getString("aging_bucket"))
                .daysOverdue(rs.getInt("days_overdue"))
                .build();
    }

    /* =============================================================
     * 共用工具方法
     * ============================================================= */
    private BigDecimal getDecimal(ResultSet rs, String column) throws SQLException {
        BigDecimal val = rs.getBigDecimal(column);
        return val != null ? val : BigDecimal.ZERO;
    }
}

