package com.lianhua.erp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.lianhua.erp.domain.Purchase;

import java.util.List;

/**
 * 應付帳齡報表 Repository
 * 統計每個供應商的未付款金額與帳齡分佈。
 */
@Repository
public interface APAgingReportRepository extends JpaRepository<Purchase, Long> {
    @Query(value = """
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
    GROUP BY p.id, s.name
    HAVING balance > 0
    ORDER BY days_overdue DESC
""", nativeQuery = true)
    List<Object[]> findAgingPayables(Long supplierId, Integer minOverdue, String period);
}
