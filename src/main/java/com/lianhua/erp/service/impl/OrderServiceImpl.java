package com.lianhua.erp.service.impl;

import com.lianhua.erp.dto.order.OrderDto;
import com.lianhua.erp.domin.Order;
import com.lianhua.erp.dto.order.OrderResponseDto;
import com.lianhua.erp.mapper.OrderMapper;
import com.lianhua.erp.repository.OrderRepository;
import com.lianhua.erp.service.OrderService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    public OrderServiceImpl(OrderRepository orderRepository, OrderMapper orderMapper) {
        this.orderRepository = orderRepository;
        this.orderMapper = orderMapper;
    }

    @Override
    public List<OrderDto> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(orderMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public OrderDto getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Order not found: " + id));
        return orderMapper.toDto(order);
    }

    @Override
    public OrderDto createOrder(OrderDto dto) {
        Order order = orderMapper.toEntity(dto);
        return orderMapper.toDto(orderRepository.save(order));
    }

    @Override
    public OrderDto updateOrder(Long id, OrderDto dto) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Order not found: " + id));

        Order updated = orderMapper.toEntity(dto);
        updated.setId(order.getId());

        return orderMapper.toDto(orderRepository.save(updated));
    }

    @Override
    public void deleteOrder(Long id) {
        orderRepository.deleteById(id);
    }

    @Override
    public OrderResponseDto getOrderWithDetails(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Order not found: " + id));
        return orderMapper.toResponseDto(order);
    }

    @Override
    public List<OrderResponseDto> getOrdersByCustomerId(Long customerId) {
        return List.of();
    }

}
