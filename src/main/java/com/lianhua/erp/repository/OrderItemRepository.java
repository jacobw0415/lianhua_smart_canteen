package com.lianhua.erp.repository;

import com.lianhua.erp.domain.OrderItem;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrder_Id(Long orderId);

    @Query("SELECT COALESCE(SUM(i.subtotal), 0) FROM OrderItem i WHERE i.order.id = :orderId")
    BigDecimal sumTotalByOrderId(@Param("orderId") Long orderId);

    /**
     * 檢查同一客戶在同一天是否已有包含指定商品的訂單
     * 用於防止重複下訂相同商品
     */
    @Query("""
            SELECT COUNT(i) > 0
            FROM OrderItem i
            WHERE i.order.customer.id = :customerId
              AND i.order.orderDate = :orderDate
              AND i.product.id = :productId
              AND i.order.orderStatus != 'CANCELLED'
            """)
    boolean existsByOrder_Customer_IdAndOrder_OrderDateAndProduct_Id(
            @Param("customerId") Long customerId,
            @Param("orderDate") java.time.LocalDate orderDate,
            @Param("productId") Long productId);

    // ✅ 支援關鍵字搜尋（搜尋商品名稱或備註）
    @Query("""
                SELECT i FROM OrderItem i
                JOIN i.product p
                WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(i.note) LIKE LOWER(CONCAT('%', :keyword, '%'))
            """)
    Page<OrderItem> search(@Param("keyword") String keyword, Pageable pageable);
}
