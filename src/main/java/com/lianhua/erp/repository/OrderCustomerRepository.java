package com.lianhua.erp.repository;

import com.lianhua.erp.domain.OrderCustomer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface OrderCustomerRepository extends JpaRepository<OrderCustomer, Long> , JpaSpecificationExecutor<OrderCustomer> {
    Optional<OrderCustomer> findByName(String name);
    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name, Long id);
}
