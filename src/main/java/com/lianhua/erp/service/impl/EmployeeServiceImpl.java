package com.lianhua.erp.service.impl;

import com.lianhua.erp.domin.Employee;
import com.lianhua.erp.dto.employee.*;
import com.lianhua.erp.mapper.EmployeeMapper;
import com.lianhua.erp.repository.EmployeeRepository;
import com.lianhua.erp.service.EmployeeService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class EmployeeServiceImpl implements EmployeeService {
    
    private final EmployeeRepository repository;
    private final EmployeeMapper mapper;
    
    @Override
    @Transactional(readOnly = true)
    public List<EmployeeResponseDto> findAll() {
        return repository.findAll().stream()
                .map(mapper::toDto)
                .toList();
    }
    
    @Override
    @Transactional(readOnly = true)
    public EmployeeResponseDto findById(Long id) {
        Employee employee = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("找不到員工 ID：" + id));
        return mapper.toDto(employee);
    }
    
    @Override
    public EmployeeResponseDto create(EmployeeRequestDto dto) {
        if (repository.existsByFullName(dto.getFullName())) {
            throw new DataIntegrityViolationException("員工名稱已存在：" + dto.getFullName());
        }
        
        Employee employee = mapper.toEntity(dto);
        return mapper.toDto(repository.save(employee));
    }
    
    @Override
    public EmployeeResponseDto update(Long id, EmployeeRequestDto dto) {
        Employee existing = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("找不到員工 ID：" + id));
        mapper.updateFromDto(dto, existing);
        return mapper.toDto(repository.save(existing));
    }
    
    @Override
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new EntityNotFoundException("員工不存在：" + id);
        }
        repository.deleteById(id);
    }
}
