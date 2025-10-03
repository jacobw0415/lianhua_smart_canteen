package com.lianhua.erp.service;

import com.lianhua.erp.dto.OrderCustomerDto;
import com.lianhua.erp.dto.OrderResponseDto;

import java.util.List;

public interface OrderCustomerService {
    List<OrderCustomerDto> getAllCustomers();
    OrderCustomerDto getCustomerById(Long id);
    OrderCustomerDto createCustomer(OrderCustomerDto dto);
    OrderCustomerDto updateCustomer(Long id, OrderCustomerDto dto);
    void deleteCustomer(Long id);

    List<OrderResponseDto> getOrdersByCustomerId(Long customerId);
}
