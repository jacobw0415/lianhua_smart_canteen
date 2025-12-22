package com.lianhua.erp.repository;

import com.lianhua.erp.domain.Receipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ReceiptRepository extends JpaRepository<Receipt, Long>, JpaSpecificationExecutor<Receipt> {
    boolean existsByOrderId(Long orderId);
    List<Receipt> findByOrderId(Long orderId);

    // ⭐ 核心方法：計算訂單已收金額
    @Query("""
        SELECT COALESCE(SUM(r.amount), 0)
        FROM Receipt r
        WHERE r.order.id = :orderId
    """)
    BigDecimal sumAmountByOrderId(@Param("orderId") Long orderId);
}
