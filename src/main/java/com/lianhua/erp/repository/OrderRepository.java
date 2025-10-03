package com.lianhua.erp.repository;

import com.lianhua.erp.domin.Order;
import com.lianhua.erp.domin.OrderCustomer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByCustomer(OrderCustomer customer);
}

