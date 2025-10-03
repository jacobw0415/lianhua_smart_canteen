package com.lianhua.erp.service;

import com.lianhua.erp.dto.OrderDto;
import com.lianhua.erp.dto.OrderResponseDto;

import java.util.List;

public interface OrderService {
    List<OrderDto> getAllOrders();
    OrderDto getOrderById(Long id);
    OrderDto createOrder(OrderDto dto);
    OrderDto updateOrder(Long id, OrderDto dto);
    void deleteOrder(Long id);
    OrderResponseDto getOrderWithDetails(Long id);
    List<OrderResponseDto> getOrdersByCustomerId(Long customerId);
}
