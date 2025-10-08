package com.lianhua.erp.service.impl;

import com.lianhua.erp.domin.Order;
import com.lianhua.erp.dto.order.OrderCustomerDto;
import com.lianhua.erp.domin.OrderCustomer;
import com.lianhua.erp.dto.order.OrderResponseDto;
import com.lianhua.erp.mapper.OrderCustomerMapper;
import com.lianhua.erp.mapper.OrderMapper;
import com.lianhua.erp.repository.OrderCustomerRepository;
import com.lianhua.erp.repository.OrderRepository;
import com.lianhua.erp.service.OrderCustomerService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrderCustomerServiceImpl implements OrderCustomerService {


    private final OrderCustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final OrderCustomerMapper customerMapper;
    private final OrderMapper orderMapper;

    public OrderCustomerServiceImpl(OrderCustomerRepository customerRepository,
                                    OrderRepository orderRepository,
                                    OrderMapper orderMapper,
                                    OrderCustomerMapper customerMapper) {
        this.customerRepository = customerRepository;
        this.orderRepository = orderRepository;
        this.orderMapper = orderMapper;
        this.customerMapper = customerMapper;
    }

    @Override
    public List<OrderCustomerDto> getAllCustomers() {
        return customerRepository.findAll().stream()
                .map(customerMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public OrderCustomerDto getCustomerById(Long id) {
        OrderCustomer customer = customerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found: " + id));
        return customerMapper.toDto(customer);
    }

    @Override
    public OrderCustomerDto createCustomer(OrderCustomerDto dto) {
        OrderCustomer customer = customerMapper.toEntity(dto);
        return customerMapper.toDto(customerRepository.save(customer));
    }

    @Override
    public OrderCustomerDto updateCustomer(Long id, OrderCustomerDto dto) {
        OrderCustomer customer = customerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found: " + id));
        customerMapper.updateEntityFromDto(dto, customer);
        return customerMapper.toDto(customerRepository.save(customer));
    }

    @Override
    public void deleteCustomer(Long id) {
        customerRepository.deleteById(id);
    }

    @Override
    public List<OrderResponseDto> getOrdersByCustomerId(Long customerId) {
        OrderCustomer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found: " + customerId));

        List<Order> orders = orderRepository.findByCustomer(customer);

        return orders.stream()
                .map(orderMapper::toResponseDto) // ✅ 使用含明細與產品資訊的 DTO
                .collect(Collectors.toList());
    }
}
