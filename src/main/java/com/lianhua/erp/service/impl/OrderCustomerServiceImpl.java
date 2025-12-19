package com.lianhua.erp.service.impl;

import com.lianhua.erp.domain.OrderCustomer;
import com.lianhua.erp.dto.orderCustomer.*;
import com.lianhua.erp.mapper.OrderCustomerMapper;
import com.lianhua.erp.repository.OrderCustomerRepository;
import com.lianhua.erp.repository.OrderRepository;
import com.lianhua.erp.service.OrderCustomerService;
import com.lianhua.erp.service.impl.spec.OrderCustomerSpecifications;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OrderCustomerServiceImpl implements OrderCustomerService {

    private final OrderCustomerRepository repository;
    private final OrderCustomerMapper mapper;
    private final OrderRepository orderRepository;

    /**
     * 建立新客戶
     * - 檢查名稱是否重複
     * - 轉換 DTO -> Entity
     * - 設定 BillingCycle（如有）
     * - 儲存並回傳 Response DTO
     */
    @Override
    public OrderCustomerResponseDto create(OrderCustomerRequestDto dto) {

        if (repository.existsByName(dto.getName())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "建立失敗，客戶名稱已存在：" + dto.getName()
            );
        }

        OrderCustomer entity = mapper.toEntity(dto);

        if (dto.getBillingCycle() != null) {
            entity.setBillingCycle(OrderCustomer.BillingCycle.valueOf(dto.getBillingCycle()));
        }

        return mapper.toResponseDto(repository.save(entity));
    }

    /**
     * 更新既有客戶
     * - 根據 ID 找 Entity（找不到則 Exception）
     * - 更新欄位
     * - 覆寫 BillingCycle（如有）
     * - 儲存並回傳 Response DTO
     */
    @Override
    public OrderCustomerResponseDto update(Long id, OrderCustomerRequestDto dto) {
        OrderCustomer entity = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("找不到客戶 ID: " + id));
        
        if (dto.getName() != null && repository.existsByNameAndIdNot(dto.getName(), id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "更新失敗，客戶名稱已存在：" + dto.getName()
            );
        }

        mapper.updateEntityFromDto(dto, entity);

        if (dto.getBillingCycle() != null) {
            entity.setBillingCycle(OrderCustomer.BillingCycle.valueOf(dto.getBillingCycle()));
        }

        return mapper.toResponseDto(repository.save(entity));
    }

    /**
     * 刪除客戶
     * - 根據 ID 找是否存在
     * - 不存在則拋出 404 Exception
     * - 存在則刪除
     */
    @Override
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new EntityNotFoundException("找不到要刪除的客戶 ID: " + id);
        }

        boolean hasOrders = orderRepository.existsByCustomerId(id);
        if (hasOrders) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "刪除失敗，該客戶已有訂單紀錄，無法刪除"
            );
        }
        repository.deleteById(id);
    }

    /**
     * 純分頁查詢（不包含搜尋條件）
     * - 使用 Spring Data JPA 內建 findAll(Pageable)
     * - 回傳 DTO Page
     * Page<T> findAll(Pageable) 是 Spring Data JPA Pagination 功能的一部分，
     * 會自動把前端傳入的 page/size/sort 轉成 SQL limit/offset 排序。 :contentReference[oaicite:0]{index=0}
     */
    @Override
    public Page<OrderCustomerResponseDto> page(Pageable pageable) {
        return repository
                .findAll(pageable)
                .map(mapper::toResponseDto);
    }

    /**
     * 分頁 + 模糊搜尋
     * - 透過 Specification 動態組合查詢條件
     * - 適用於 List Filter & React-Admin 搜尋介面
     * - 結果以 Page 回傳
     * findAll(Specification, Pageable) 會同時處理條件查詢與分頁排序。 :contentReference[oaicite:1]{index=1}
     */
    @Override
    @Transactional(readOnly = true)
    public Page<OrderCustomerResponseDto> search(
            OrderCustomerRequestDto request,
            Pageable pageable
    ) {
        Specification<OrderCustomer> spec =
                OrderCustomerSpecifications.bySearchRequest(request);

        return repository.findAll(spec, pageable)
                .map(mapper::toResponseDto);
    }

    /**
     * 單一客戶查詢
     * - 根據 ID 查詢
     * - 找不到則拋出 NotFound Exception
     * - 找到則回傳對應 DTO
     */
    @Override
    @Transactional(readOnly = true)
    public OrderCustomerResponseDto findById(Long id) {
        return repository.findById(id)
                .map(mapper::toResponseDto)
                .orElseThrow(() -> new EntityNotFoundException("找不到客戶 ID: " + id));
    }
}
