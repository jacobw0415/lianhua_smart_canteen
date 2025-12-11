package com.lianhua.erp.repository;

import com.lianhua.erp.dto.ap.APAgingSummaryDto;
import com.lianhua.erp.dto.ap.APAgingPurchaseDetailDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
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
        String sql = baseSummarySql() + " ORDER BY balance DESC ";
        return jdbcTemplate.query(sql, this::mapSummaryRow);
    }

    /* =============================================================
     * ② Summary（分頁）
     * ============================================================= */
    public Page<APAgingSummaryDto> findAgingSummaryPaged(int page, int size) {

        int offset = page * size;

        String pagedSql = baseSummarySql()
                + " ORDER BY balance DESC "
                + " LIMIT ? OFFSET ? ";

        List<APAgingSummaryDto> content =
                jdbcTemplate.query(pagedSql, this::mapSummaryRow, size, offset);

        String countSql = """
            SELECT COUNT(*) FROM (
                """ + baseSummarySql() + """
            ) AS t
            """;

        Integer total = jdbcTemplate.queryForObject(countSql, Integer.class);

        return new PageImpl<>(content, PageRequest.of(page, size), total);
    }

    /* =============================================================
     * 共用 Summary SQL
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
                        THEN p.balance
                        ELSE 0 
                    END
                ) AS aging_0_30,

                SUM(
                    CASE 
                        WHEN DATEDIFF(CURDATE(), p.purchase_date) BETWEEN 31 AND 60
                        THEN p.balance
                        ELSE 0 
                    END
                ) AS aging_31_60,

                SUM(
                    CASE 
                        WHEN DATEDIFF(CURDATE(), p.purchase_date) > 60
                        THEN p.balance
                        ELSE 0 
                    END
                ) AS aging_60_plus,

                /* ---- 應付總額、已付、未付 ---- */
                SUM(p.total_amount) AS total_amount,
                SUM(p.paid_amount) AS paid_amount,
                SUM(p.balance) AS balance

            FROM purchases p
            JOIN suppliers s ON s.id = p.supplier_id

            WHERE p.balance > 0

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
     * ③ Detail — 指定供應商未付進貨明細
     * ============================================================= */
    public List<APAgingPurchaseDetailDto> findPurchasesBySupplierId(Long supplierId) {

        String sql = """
            SELECT
                p.id AS purchase_id,
                p.purchase_date,
                p.total_amount,
                p.paid_amount,
                p.balance,
                p.status
            FROM purchases p
            WHERE p.supplier_id = ?
              AND p.balance > 0
            ORDER BY p.purchase_date DESC
            """;

        return jdbcTemplate.query(sql, this::mapDetailRow, supplierId);
    }

    private APAgingPurchaseDetailDto mapDetailRow(ResultSet rs, int rowNum) throws SQLException {
        return APAgingPurchaseDetailDto.builder()
                .id(rs.getLong("purchase_id"))
                .purchaseId(rs.getLong("purchase_id"))
                .purchaseDate(rs.getDate("purchase_date").toLocalDate())
                .amount(getDecimal(rs, "total_amount"))
                .paidAmount(getDecimal(rs, "paid_amount"))
                .balance(getDecimal(rs, "balance"))
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