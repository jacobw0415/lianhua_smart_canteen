package com.lianhua.erp.service;

import com.lianhua.erp.dto.order.*;
import com.lianhua.erp.dto.orderItem.OrderItemRequestDto;
import com.lianhua.erp.dto.orderItem.OrderItemResponseDto;

import java.util.List;

public interface OrderItemService {

    List<OrderItemResponseDto> findByOrderId(Long orderId);

    OrderItemResponseDto create(Long orderId, OrderItemRequestDto dto);

    OrderItemResponseDto update(Long orderId, Long itemId, OrderItemRequestDto dto);

    void delete(Long orderId, Long itemId);
}
