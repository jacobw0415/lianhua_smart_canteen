package com.lianhua.erp.repository;

import com.lianhua.erp.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;

public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {
    boolean existsByCustomer_IdAndOrderDate(Long customerId, LocalDate orderDate);
    boolean existsByCustomerId(Long customerId);
}
