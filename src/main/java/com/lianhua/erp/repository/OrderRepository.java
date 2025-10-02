package com.lianhua.erp.repository;

import com.lianhua.erp.domin.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {}
