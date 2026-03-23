package com.lianhua.erp.service;

import com.lianhua.erp.dto.export.ExportPayload;
import com.lianhua.erp.dto.orderCustomer.OrderCustomerRequestDto;
import com.lianhua.erp.dto.orderCustomer.OrderCustomerResponseDto;
import com.lianhua.erp.export.ExportFormat;
import com.lianhua.erp.export.ExportScope;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderCustomerService {

    OrderCustomerResponseDto create(OrderCustomerRequestDto dto);

    OrderCustomerResponseDto update(Long id, OrderCustomerRequestDto dto);

    void delete(Long id);

    Page<OrderCustomerResponseDto> page(Pageable pageable);

    Page<OrderCustomerResponseDto> search(OrderCustomerRequestDto request, Pageable pageable);

    /**
     * 匯出客戶列表（篩選條件與 {@link #search} 相同；scope=all 時不分頁）。
     */
    ExportPayload exportCustomers(
            OrderCustomerRequestDto request,
            Pageable pageable,
            ExportFormat format,
            ExportScope scope
    );

    OrderCustomerResponseDto findById(Long id);
}
