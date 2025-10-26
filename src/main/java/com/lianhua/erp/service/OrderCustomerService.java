package com.lianhua.erp.service;

import com.lianhua.erp.dto.orderCustomer.*;
import java.util.List;

public interface OrderCustomerService {
    OrderCustomerResponseDto create(OrderCustomerRequestDto dto);
    OrderCustomerResponseDto update(Long id, OrderCustomerRequestDto dto);
    void delete(Long id);
    List<OrderCustomerResponseDto> findAll();
    OrderCustomerResponseDto findById(Long id);
}
