package com.lianhua.erp.service;

import com.lianhua.erp.dto.orderCustomer.OrderCustomerRequestDto;
import com.lianhua.erp.dto.orderCustomer.OrderCustomerResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderCustomerService {

    OrderCustomerResponseDto create(OrderCustomerRequestDto dto);

    OrderCustomerResponseDto update(Long id, OrderCustomerRequestDto dto);

    void delete(Long id);

    Page<OrderCustomerResponseDto> page(Pageable pageable);

    Page<OrderCustomerResponseDto> search(OrderCustomerRequestDto request, Pageable pageable);

    OrderCustomerResponseDto findById(Long id);
}
