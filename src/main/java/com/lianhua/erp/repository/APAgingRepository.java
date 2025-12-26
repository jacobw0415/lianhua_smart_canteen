package com.lianhua.erp.repository;

import com.lianhua.erp.dto.ap.APAgingFilterDto;
import com.lianhua.erp.dto.ap.APAgingSummaryDto;
import com.lianhua.erp.dto.ap.APAgingPurchaseDetailDto;

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
public class APAgingRepository {

    private final JdbcTemplate jdbcTemplate;

    /* =============================================================
     * ① Summary（不分頁）
     * ============================================================= */
    public List<APAgingSummaryDto> findAgingSummary() {
        String sql = baseSummarySql()
                + " HAVING SUM(p.total_amount - COALESCE(paid_summary.paid_amount, 0)) > 0 "
                + " ORDER BY balance DESC ";
        return jdbcTemplate.query(sql, this::mapSummaryRow);
    }

    /* =============================================================
     * ② Summary（分頁 + 搜尋）
     * ============================================================= */
    public Page<APAgingSummaryDto> findAgingSummaryPaged(
            APAgingFilterDto filter,
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
            if (filter != null && StringUtils.hasText(filter.getSupplierName())) {
                sql.append(" AND s.name LIKE ? ");
                params.add("%" + filter.getSupplierName().trim() + "%");
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
                            SUM(CASE WHEN DATEDIFF(CURDATE(), p.purchase_date) <= 30
                            THEN (p.total_amount - COALESCE(paid_summary.paid_amount, 0)) ELSE 0 END) > 0
                        """);
                case "DAYS_31_60" -> havingConditions.add("""
                            SUM(CASE WHEN DATEDIFF(CURDATE(), p.purchase_date) BETWEEN 31 AND 60
                            THEN (p.total_amount - COALESCE(paid_summary.paid_amount, 0)) ELSE 0 END) > 0
                        """);
                case "DAYS_60_PLUS" -> havingConditions.add("""
                            SUM(CASE WHEN DATEDIFF(CURDATE(), p.purchase_date) > 60
                            THEN (p.total_amount - COALESCE(paid_summary.paid_amount, 0)) ELSE 0 END) > 0
                        """);
                default -> {
                }
            }
        }

        if (filter == null || filter.getOnlyUnpaid() == null || Boolean.TRUE.equals(filter.getOnlyUnpaid())) {
            havingConditions.add(" SUM(p.total_amount - COALESCE(paid_summary.paid_amount, 0)) > 0 ");
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

        List<APAgingSummaryDto> content =
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
                    s.id AS supplier_id,
                    s.name AS supplier_name,
                
                    /* ---- Aging bucket 計算 ---- */
                    SUM(
                        CASE 
                            WHEN DATEDIFF(CURDATE(), p.purchase_date) <= 30 
                            THEN (p.total_amount - COALESCE(paid_summary.paid_amount, 0))
                            ELSE 0 
                        END
                    ) AS aging_0_30,
                
                    SUM(
                        CASE 
                            WHEN DATEDIFF(CURDATE(), p.purchase_date) BETWEEN 31 AND 60
                            THEN (p.total_amount - COALESCE(paid_summary.paid_amount, 0))
                            ELSE 0 
                        END
                    ) AS aging_31_60,
                
                    SUM(
                        CASE 
                            WHEN DATEDIFF(CURDATE(), p.purchase_date) > 60
                            THEN (p.total_amount - COALESCE(paid_summary.paid_amount, 0))
                            ELSE 0 
                        END
                    ) AS aging_60_plus,
                
                    /* ---- 應付總額、已付、未付 ---- */
                    SUM(p.total_amount) AS total_amount,
                    SUM(COALESCE(paid_summary.paid_amount, 0)) AS paid_amount,
                    SUM(p.total_amount - COALESCE(paid_summary.paid_amount, 0)) AS balance
                
                FROM purchases p
                JOIN suppliers s ON s.id = p.supplier_id
                LEFT JOIN (
                    SELECT 
                        purchase_id,
                        SUM(amount) AS paid_amount
                    FROM payments
                    WHERE status = 'ACTIVE'
                    GROUP BY purchase_id
                ) paid_summary ON p.id = paid_summary.purchase_id
                
                WHERE p.record_status = 'ACTIVE'
                
                GROUP BY s.id, s.name
                """;
    }

    private APAgingSummaryDto mapSummaryRow(ResultSet rs, int rowNum) throws SQLException {
        return APAgingSummaryDto.builder()
                .id(rs.getLong("supplier_id"))
                .supplierId(rs.getLong("supplier_id"))
                .supplierName(rs.getString("supplier_name"))
                .aging0to30(getDecimal(rs, "aging_0_30"))
                .aging31to60(getDecimal(rs, "aging_31_60"))
                .aging60plus(getDecimal(rs, "aging_60_plus"))
                .totalAmount(getDecimal(rs, "total_amount"))
                .paidAmount(getDecimal(rs, "paid_amount"))
                .balance(getDecimal(rs, "balance"))
                .build();
    }

    /* =============================================================
     * ③ Detail — 指定供應商未付進貨明細（未動）
     * ============================================================= */
    public List<APAgingPurchaseDetailDto> findPurchasesBySupplierId(Long supplierId) {

        String sql = """
                SELECT
                    p.id AS purchase_id,
                    p.purchase_no,
                    p.purchase_date,
                    p.total_amount,
                    COALESCE(paid_summary.paid_amount, 0) AS paid_amount,
                    (p.total_amount - COALESCE(paid_summary.paid_amount, 0)) AS balance,
                    p.status,
                
                    CASE
                        WHEN DATEDIFF(CURDATE(), p.purchase_date) <= 30 THEN '0–30'
                        WHEN DATEDIFF(CURDATE(), p.purchase_date) BETWEEN 31 AND 60 THEN '31–60'
                        ELSE '>60'
                    END AS aging_bucket
                
                FROM purchases p
                LEFT JOIN (
                    SELECT 
                        purchase_id,
                        SUM(amount) AS paid_amount
                    FROM payments
                    WHERE status = 'ACTIVE'
                    GROUP BY purchase_id
                ) paid_summary ON p.id = paid_summary.purchase_id
                WHERE p.supplier_id = ?
                  AND p.record_status = 'ACTIVE'
                  AND (p.total_amount - COALESCE(paid_summary.paid_amount, 0)) > 0
                ORDER BY p.purchase_date DESC
                """;

        return jdbcTemplate.query(sql, this::mapDetailRow, supplierId);
    }

    private APAgingPurchaseDetailDto mapDetailRow(ResultSet rs, int rowNum) throws SQLException {
        return APAgingPurchaseDetailDto.builder()
                .id(rs.getLong("purchase_id"))
                .purchaseId(rs.getLong("purchase_id"))
                .purchaseNo(rs.getString("purchase_no"))
                .purchaseDate(rs.getDate("purchase_date").toLocalDate())
                .totalAmount(getDecimal(rs, "total_amount"))
                .paidAmount(getDecimal(rs, "paid_amount"))
                .balance(getDecimal(rs, "balance"))
                .agingBucket(rs.getString("aging_bucket"))
                .status(rs.getString("status"))
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