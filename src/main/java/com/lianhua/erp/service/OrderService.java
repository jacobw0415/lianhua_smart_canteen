package com.lianhua.erp.service;

import com.lianhua.erp.dto.order.*;
import java.util.List;

public interface OrderService {
    OrderResponseDto create(OrderRequestDto dto);
    OrderResponseDto update(Long id, OrderRequestDto dto);
    void delete(Long id);
    OrderResponseDto findById(Long id);
    List<OrderResponseDto> findAll();
}
