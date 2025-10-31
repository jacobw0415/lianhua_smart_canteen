package com.lianhua.erp.repository;

import com.lianhua.erp.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;

public interface OrderRepository extends JpaRepository<Order, Long> {
    boolean existsByCustomer_IdAndOrderDate(Long customerId, LocalDate orderDate);
}
