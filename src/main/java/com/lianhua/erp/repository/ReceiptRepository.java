package com.lianhua.erp.repository;

import com.lianhua.erp.domain.Receipt;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReceiptRepository extends JpaRepository<Receipt, Long>, JpaSpecificationExecutor<Receipt> {
    boolean existsByOrderId(Long orderId);

    @EntityGraph(attributePaths = { "order", "order.customer" })
    @Query("SELECT r FROM Receipt r WHERE r.order.id = :orderId AND r.status = 'ACTIVE'")
    List<Receipt> findByOrderId(@Param("orderId") Long orderId);

    @EntityGraph(attributePaths = { "order", "order.customer" })
    @Override
    Optional<Receipt> findById(Long id);

    // ⭐ 核心方法：計算訂單已收金額（排除已作廢的收款）
    @Query("""
                SELECT COALESCE(SUM(r.amount), 0)
                FROM Receipt r
                WHERE r.order.id = :orderId
                  AND r.status = 'ACTIVE'
            """)
    BigDecimal sumAmountByOrderId(@Param("orderId") Long orderId);

    // ⭐ 檢查訂單是否有任何收款記錄（包括已作廢的）
    @Query("""
                SELECT COUNT(r) > 0
                FROM Receipt r
                WHERE r.order.id = :orderId
            """)
    boolean hasAnyReceiptByOrderId(@Param("orderId") Long orderId);
}
