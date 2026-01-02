package com.lianhua.erp.service.impl;

import com.lianhua.erp.domain.Employee;
import com.lianhua.erp.dto.employee.*;
import com.lianhua.erp.mapper.EmployeeMapper;
import com.lianhua.erp.repository.EmployeeRepository;
import com.lianhua.erp.repository.ExpenseRepository;
import com.lianhua.erp.service.EmployeeService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class EmployeeServiceImpl implements EmployeeService {
    
    private final EmployeeRepository repository;
    private final EmployeeMapper mapper;
    private final ExpenseRepository expenseRepository;
    
    @Override
    @Transactional(readOnly = true)
    public Page<EmployeeResponseDto> findAll(Pageable pageable) {
        Pageable safePageable = normalizePageable(pageable);
        
        try {
            return repository.findAll(safePageable)
                    .map(mapper::toDto);
        } catch (PropertyReferenceException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "無效排序欄位：" + ex.getPropertyName()
            );
        }
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
        // =============================================
        // 1️⃣ 基本欄位完整性檢查
        // =============================================
        if (!StringUtils.hasText(dto.getFullName())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "員工全名為必填欄位，不可為空");
        }
        
        // 去除前後空白
        String fullName = dto.getFullName().trim();
        if (fullName.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "員工全名不可為空白");
        }
        
        // 檢查名稱長度
        if (fullName.length() > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "員工全名長度不可超過100個字元");
        }
        
        // =============================================
        // 2️⃣ 檢查員工名稱是否已存在（在保存前檢查）
        // =============================================
        if (repository.existsByFullName(fullName)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "員工名稱已存在：「" + fullName + "」，請使用其他名稱");
        }
        
        // =============================================
        // 3️⃣ 驗證其他欄位
        // =============================================
        // 職位長度檢查
        if (dto.getPosition() != null && dto.getPosition().trim().length() > 50) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "職位長度不可超過50個字元");
        }
        
        // 薪資檢查（如果提供）
        if (dto.getSalary() != null && dto.getSalary().compareTo(java.math.BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "薪資不得為負數");
        }
        
        // 狀態驗證（如果提供）
        if (dto.getStatus() != null && !dto.getStatus().trim().isEmpty()) {
            try {
                Employee.Status.valueOf(dto.getStatus().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "狀態必須為 ACTIVE 或 INACTIVE");
            }
        }
        
        // =============================================
        // 4️⃣ 建立員工實體並保存
        // =============================================
        Employee employee = mapper.toEntity(dto);
        // 確保使用trim後的名稱
        employee.setFullName(fullName);
        // 處理職位（如果有）
        if (dto.getPosition() != null) {
            employee.setPosition(dto.getPosition().trim());
        }
        
        try {
            Employee saved = repository.save(employee);
            return mapper.toDto(saved);
        } catch (DataIntegrityViolationException ex) {
            // 如果仍然發生資料完整性違規，提供更友好的錯誤訊息
            String errorMessage = ex.getMessage();
            if (errorMessage != null && errorMessage.contains("full_name")) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "員工名稱「" + fullName + "」已存在，請使用其他名稱");
            }
            // 其他資料完整性錯誤
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "資料驗證失敗，請檢查輸入資料是否正確");
        }
    }
    
    @Override
    public EmployeeResponseDto update(Long id, EmployeeRequestDto dto) {
        Employee existing = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("找不到員工 ID：" + id));
        
        // 檢查員工名稱是否與其他員工重複（排除自己）
        if (hasText(dto.getFullName()) && 
            !dto.getFullName().trim().equalsIgnoreCase(existing.getFullName()) &&
            repository.existsByFullName(dto.getFullName())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "員工名稱已存在：" + dto.getFullName());
        }
        
        mapper.updateFromDto(dto, existing);
        return mapper.toDto(repository.save(existing));
    }
    
    @Override
    public void delete(Long id) {
        Employee employee = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("找不到員工 ID：" + id));
        
        // 檢查員工是否被支出記錄引用
        if (expenseRepository.existsByEmployeeId(id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "無法刪除員工「" + employee.getFullName() + "」，該員工已被支出記錄引用。請先處理相關的支出記錄。");
        }
        
        repository.deleteById(id);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<EmployeeResponseDto> getActive() {
        return repository.findByStatusOrderByFullNameAsc(Employee.Status.ACTIVE)
                .stream()
                .map(mapper::toDto)
                .toList();
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<EmployeeResponseDto> searchEmployees(EmployeeSearchRequest request, Pageable pageable) {
        if (isEmptySearch(request)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "搜尋條件不可全為空，至少需提供一項搜尋欄位"
            );
        }
        
        Pageable safePageable = normalizePageable(pageable);
        Specification<Employee> spec = buildEmployeeSpec(request);
        
        Page<Employee> page;
        
        try {
            page = repository.findAll(spec, safePageable);
        } catch (PropertyReferenceException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    String.format("無效排序欄位：%s", ex.getPropertyName())
            );
        }
        
        if (page.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "查無匹配的員工資料，請調整搜尋條件"
            );
        }
        
        return page.map(mapper::toDto);
    }
    
    @Override
    public EmployeeResponseDto activate(Long id) {
        Employee employee = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("找不到員工 ID：" + id));
        
        employee.setStatus(Employee.Status.ACTIVE);
        Employee saved = repository.save(employee);
        return mapper.toDto(saved);
    }
    
    @Override
    public EmployeeResponseDto deactivate(Long id) {
        Employee employee = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("找不到員工 ID：" + id));
        
        employee.setStatus(Employee.Status.INACTIVE);
        Employee saved = repository.save(employee);
        return mapper.toDto(saved);
    }
    
    // ================================================================
    // Specification 建置
    // ================================================================
    private Specification<Employee> buildEmployeeSpec(EmployeeSearchRequest req) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // 員工姓名（模糊搜尋）
            if (hasText(req.getFullName())) {
                predicates.add(cb.like(
                        cb.lower(root.get("fullName")),
                        String.format("%%%s%%", req.getFullName().toLowerCase())
                ));
            }
            
            // 職位（模糊搜尋）
            if (hasText(req.getPosition())) {
                predicates.add(cb.like(
                        cb.lower(root.get("position")),
                        String.format("%%%s%%", req.getPosition().toLowerCase())
                ));
            }
            
            // 狀態（精確搜尋）
            if (hasText(req.getStatus())) {
                try {
                    Employee.Status status = Employee.Status.valueOf(req.getStatus().toUpperCase());
                    predicates.add(cb.equal(root.get("status"), status));
                } catch (IllegalArgumentException e) {
                    // 無效的狀態值，忽略此條件
                }
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
    
    // ================================================================
    // pageable 安全檢查
    // ================================================================
    private Pageable normalizePageable(Pageable pageable) {
        if (pageable.getPageNumber() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page 不可小於 0");
        }
        
        if (pageable.getPageSize() <= 0 || pageable.getPageSize() > 200) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "size 需介於 1 - 200 之間");
        }
        
        Sort sort = pageable.getSort().isSorted()
                ? pageable.getSort()
                : Sort.by(Sort.Direction.ASC, "id");
        
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }
    
    // ================================================================
    // 工具方法
    // ================================================================
    private boolean hasText(String s) {
        return s != null && !s.trim().isEmpty();
    }
    
    private boolean isEmptySearch(EmployeeSearchRequest req) {
        return !hasText(req.getFullName()) &&
               !hasText(req.getPosition()) &&
               !hasText(req.getStatus());
    }
}
