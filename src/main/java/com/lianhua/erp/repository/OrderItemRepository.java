package com.lianhua.erp.repository;

import com.lianhua.erp.domin.OrderItem;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrder_Id(Long orderId);

    @Query("SELECT COALESCE(SUM(i.subtotal - i.discount + i.tax), 0) FROM OrderItem i WHERE i.order.id = :orderId")
    BigDecimal sumTotalByOrderId(@Param("orderId") Long orderId);
}
