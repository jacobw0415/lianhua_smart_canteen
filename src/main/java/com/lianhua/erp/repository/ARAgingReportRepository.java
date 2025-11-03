package com.lianhua.erp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.lianhua.erp.domain.Order;

import java.util.List;

/**
 * 應收帳齡報表 Repository
 * 統計每個客戶的應收餘額與帳齡分佈。
 */
@Repository
public interface ARAgingReportRepository extends JpaRepository<Order, Long> {

    @Query(value = """
        SELECT 
            c.name AS customer_name,
            o.id AS order_id,
            DATE_FORMAT(o.order_date, '%Y-%m-%d') AS order_date,
            DATE_FORMAT(o.delivery_date, '%Y-%m-%d') AS delivery_date,
            o.total_amount AS total_amount,
            COALESCE(r.amount, 0) AS received_amount,
            (o.total_amount - COALESCE(r.amount, 0)) AS balance,
            DATEDIFF(CURDATE(), o.delivery_date) AS days_overdue,
            CASE
                WHEN DATEDIFF(CURDATE(), o.delivery_date) <= 30 THEN '0–30天'
                WHEN DATEDIFF(CURDATE(), o.delivery_date) <= 60 THEN '31–60天'
                WHEN DATEDIFF(CURDATE(), o.delivery_date) <= 90 THEN '61–90天'
                ELSE '90天以上'
            END AS aging_bucket
        FROM orders o
        JOIN order_customers c ON o.customer_id = c.id
        LEFT JOIN receipts r ON o.id = r.order_id
        WHERE (o.total_amount - COALESCE(r.amount, 0)) > 0
        ORDER BY days_overdue DESC
        """, nativeQuery = true)
    List<Object[]> findAgingReceivables(Long customerId, Integer minOverdue, String period);
}

