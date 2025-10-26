package com.lianhua.erp.service.impl;

import com.lianhua.erp.domin.*;
import com.lianhua.erp.dto.order.*;
import com.lianhua.erp.mapper.OrderMapper;
import com.lianhua.erp.repository.*;
import com.lianhua.erp.service.OrderService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OrderServiceImpl implements OrderService {
    
    private final OrderRepository orderRepository;
    private final OrderCustomerRepository customerRepository;
    private final OrderMapper mapper;
    
    @Override
    public OrderResponseDto create(OrderRequestDto dto) {
        // 驗證客戶存在
        OrderCustomer customer = customerRepository.findById(dto.getCustomerId())
                .orElseThrow(() -> new EntityNotFoundException("找不到客戶 ID: " + dto.getCustomerId()));
        
        // 唯一約束檢查
        if (orderRepository.existsByCustomer_IdAndOrderDate(dto.getCustomerId(), dto.getOrderDate())) {
            throw new IllegalArgumentException("該客戶於 " + dto.getOrderDate() + " 已有訂單紀錄。");
        }
        
        Order order = mapper.toEntity(dto);
        order.setCustomer(customer);
        
        if (dto.getStatus() != null) {
            order.setStatus(Order.Status.valueOf(dto.getStatus()));
        }
        
        if (dto.getAccountingPeriod() == null) {
            order.setAccountingPeriod(dto.getOrderDate().toString().substring(0, 7));
        }
        
        orderRepository.save(order);
        return mapper.toResponseDto(order);
    }
    
    @Override
    public OrderResponseDto update(Long id, OrderRequestDto dto) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("找不到訂單 ID: " + id));
        
        mapper.updateEntityFromDto(dto, order);
        
        if (dto.getStatus() != null) {
            order.setStatus(Order.Status.valueOf(dto.getStatus()));
        }
        
        if (dto.getCustomerId() != null && !dto.getCustomerId().equals(order.getCustomer().getId())) {
            OrderCustomer newCustomer = customerRepository.findById(dto.getCustomerId())
                    .orElseThrow(() -> new EntityNotFoundException("找不到客戶 ID: " + dto.getCustomerId()));
            order.setCustomer(newCustomer);
        }
        
        return mapper.toResponseDto(orderRepository.save(order));
    }
    
    @Override
    public void delete(Long id) {
        if (!orderRepository.existsById(id)) {
            throw new EntityNotFoundException("找不到要刪除的訂單 ID: " + id);
        }
        orderRepository.deleteById(id);
    }
    
    @Override
    public OrderResponseDto findById(Long id) {
        return orderRepository.findById(id)
                .map(mapper::toResponseDto)
                .orElseThrow(() -> new EntityNotFoundException("找不到訂單 ID: " + id));
    }
    
    @Override
    public List<OrderResponseDto> findAll() {
        return orderRepository.findAll().stream().map(mapper::toResponseDto).toList();
    }
}
