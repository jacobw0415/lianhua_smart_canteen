package com.lianhua.erp.service;

import com.lianhua.erp.dto.order.*;
import com.lianhua.erp.dto.orderItem.OrderItemRequestDto;
import com.lianhua.erp.dto.orderItem.OrderItemResponseDto;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface OrderItemService {

    List<OrderItemResponseDto> findAll();
    
    Page<OrderItemResponseDto> findAllPaged(int page, int size, String keyword);
    
    List<OrderItemResponseDto> findByOrderId(Long orderId);

    OrderItemResponseDto create(Long orderId, OrderItemRequestDto dto);

    OrderItemResponseDto update(Long orderId, Long itemId, OrderItemRequestDto dto);

    void delete(Long orderId, Long itemId);
    
    Page<OrderItemResponseDto> findAllPaged(Pageable pageable);
}
