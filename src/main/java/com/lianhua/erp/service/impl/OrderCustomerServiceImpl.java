package com.lianhua.erp.service.impl;

import com.lianhua.erp.domin.OrderCustomer;
import com.lianhua.erp.dto.orderCustomer.*;
import com.lianhua.erp.mapper.OrderCustomerMapper;
import com.lianhua.erp.repository.OrderCustomerRepository;
import com.lianhua.erp.service.OrderCustomerService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OrderCustomerServiceImpl implements OrderCustomerService {
    
    private final OrderCustomerRepository repository;
    private final OrderCustomerMapper mapper;
    
    @Override
    public OrderCustomerResponseDto create(OrderCustomerRequestDto dto) {
        if (repository.existsByName(dto.getName())) {
            throw new DataIntegrityViolationException("客戶名稱已存在：" + dto.getName());
        }
        
        OrderCustomer entity = mapper.toEntity(dto);
        if (dto.getBillingCycle() != null) {
            entity.setBillingCycle(OrderCustomer.BillingCycle.valueOf(dto.getBillingCycle()));
        }
        
        repository.save(entity);
        return mapper.toResponseDto(entity);
    }
    
    @Override
    public OrderCustomerResponseDto update(Long id, OrderCustomerRequestDto dto) {
        OrderCustomer entity = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("找不到客戶 ID: " + id));
        
        mapper.updateEntityFromDto(dto, entity);
        
        if (dto.getBillingCycle() != null) {
            entity.setBillingCycle(OrderCustomer.BillingCycle.valueOf(dto.getBillingCycle()));
        }
        
        return mapper.toResponseDto(repository.save(entity));
    }
    
    @Override
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new EntityNotFoundException("找不到要刪除的客戶 ID: " + id);
        }
        repository.deleteById(id);
    }
    
    @Override
    public List<OrderCustomerResponseDto> findAll() {
        return repository.findAll().stream().map(mapper::toResponseDto).toList();
    }
    
    @Override
    public OrderCustomerResponseDto findById(Long id) {
        return repository.findById(id)
                .map(mapper::toResponseDto)
                .orElseThrow(() -> new EntityNotFoundException("找不到客戶 ID: " + id));
    }
}
