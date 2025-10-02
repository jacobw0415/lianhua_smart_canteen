package com.lianhua.erp.repository;

import com.lianhua.erp.domin.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {}
