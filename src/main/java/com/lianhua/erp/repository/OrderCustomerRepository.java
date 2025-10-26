package com.lianhua.erp.repository;

import com.lianhua.erp.domin.OrderCustomer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderCustomerRepository extends JpaRepository<OrderCustomer, Long> {
    Optional<OrderCustomer> findByName(String name);
    boolean existsByName(String name);
}
