package com.lianhua.erp.repository;

import com.lianhua.erp.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {
    boolean existsByCustomer_IdAndOrderDate(Long customerId, LocalDate orderDate);
    boolean existsByCustomerId(Long customerId);
    
    Optional<Order> findByOrderNo(String orderNo);
    
    boolean existsByOrderNo(String orderNo);
    
    @Query("""
            SELECT COALESCE(
                MAX(
                    CAST(
                        SUBSTRING(o.orderNo, LENGTH(:prefix) + 2)
                        AS integer
                    )
                ),
                0
            ) + 1
            FROM Order o
            WHERE o.orderNo LIKE CONCAT(:prefix, '-%')
            """)
    int findNextOrderSequence(@Param("prefix") String prefix);
}
