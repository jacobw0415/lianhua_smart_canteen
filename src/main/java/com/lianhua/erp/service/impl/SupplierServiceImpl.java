package com.lianhua.erp.service.impl;

import com.lianhua.erp.domain.Supplier;
import com.lianhua.erp.dto.supplier.SupplierResponseDto;
import com.lianhua.erp.dto.supplier.SupplierRequestDto;
import com.lianhua.erp.dto.supplier.SupplierSearchRequest;
import com.lianhua.erp.mapper.SupplierMapper;
import com.lianhua.erp.repository.SupplierRepository;
import com.lianhua.erp.repository.PurchaseRepository;
import com.lianhua.erp.service.SupplierService;
import com.lianhua.erp.service.impl.spec.SupplierSpecifications;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class SupplierServiceImpl implements SupplierService {

    private final SupplierRepository supplierRepository;
    private final SupplierMapper supplierMapper;
    private final PurchaseRepository purchaseRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<SupplierResponseDto> getAllSuppliers(Pageable pageable) {
        Pageable safePageable = normalizePageable(pageable);
        try {
            return supplierRepository.findAll(safePageable).map(supplierMapper::toDto);
        } catch (PropertyReferenceException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "無效排序欄位：" + ex.getPropertyName());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierResponseDto getSupplierById(Long id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("找不到供應商 ID：" + id));
        return supplierMapper.toDto(supplier);
    }

    @Override
    public SupplierResponseDto createSupplier(SupplierRequestDto dto) {
        dto.trimAll();
        if (supplierRepository.existsByName(dto.getName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "供應商名稱已存在：" + dto.getName());
        }
        Supplier supplier = supplierMapper.toEntity(dto);
        supplier.setActive(true);
        return supplierMapper.toDto(supplierRepository.save(supplier));
    }

    @Override
    public SupplierResponseDto updateSupplier(Long id, SupplierRequestDto dto) {
        dto.trimAll();
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("找不到供應商 ID：" + id));

        if (!supplier.getName().equals(dto.getName()) && supplierRepository.existsByName(dto.getName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "供應商名稱已存在：" + dto.getName());
        }

        supplierMapper.updateEntityFromDto(dto, supplier);

        if (dto.getNote() != null) {
            supplier.setNote(dto.getNote().trim());
        } else {
            supplier.setNote(null);
        }

        try {
            return supplierMapper.toDto(supplierRepository.save(supplier));
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "更新供應商失敗，名稱可能已存在", ex);
        }
    }

    @Override
    public SupplierResponseDto deactivateSupplier(Long id) {
        Supplier supplier = findSupplier(id);
        supplier.setActive(false);
        return supplierMapper.toDto(supplierRepository.save(supplier));
    }

    @Override
    public SupplierResponseDto activateSupplier(Long id) {
        Supplier supplier = findSupplier(id);
        supplier.setActive(true);
        return supplierMapper.toDto(supplierRepository.save(supplier));
    }

    @Override
    public void deleteSupplier(Long id) {
        Supplier supplier = findSupplier(id);
        if (purchaseRepository.existsBySupplierId(id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("無法刪除供應商：「%s」，因已存在進貨紀錄。請改為停用。", supplier.getName()));
        }
        supplierRepository.delete(supplier);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SupplierResponseDto> searchSuppliers(SupplierSearchRequest req, Pageable pageable) {
        if (isEmptySearch(req)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "請至少提供一項搜尋條件");
        }

        Pageable safePageable = normalizePageable(pageable);
        // ⭐ 使用抽離後的 Spec 類別
        Specification<Supplier> spec = SupplierSpecifications.bySearchRequest(req);

        try {
            Page<Supplier> page = supplierRepository.findAll(spec, safePageable);
            if (page.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "查無匹配供應商");
            }
            return page.map(supplierMapper::toDto);
        } catch (PropertyReferenceException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "無效排序欄位：" + ex.getPropertyName());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupplierResponseDto> getActiveSuppliers() {
        List<Supplier> suppliers = supplierRepository.findByActiveTrueOrderByNameAsc();
        if (suppliers.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "目前沒有啟用的供應商");
        }
        return suppliers.stream().map(supplierMapper::toDto).toList();
    }

    // ================= Private Helpers =================

    private Supplier findSupplier(Long id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("找不到供應商 ID：" + id));
    }

    private Pageable normalizePageable(Pageable pageable) {
        if (pageable.getPageNumber() < 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page 無效");
        int pageSize = (pageable.getPageSize() <= 0 || pageable.getPageSize() > 200) ? 25 : pageable.getPageSize();
        Sort sort = pageable.getSort().isSorted() ? pageable.getSort() : Sort.by(Sort.Direction.ASC, "id");
        return PageRequest.of(pageable.getPageNumber(), pageSize, sort);
    }

    private boolean isEmptySearch(SupplierSearchRequest req) {
        return !StringUtils.hasText(req.getSupplierName()) &&
                !StringUtils.hasText(req.getContact()) &&
                !StringUtils.hasText(req.getPhone()) &&
                !StringUtils.hasText(req.getBillingCycle()) &&
                !StringUtils.hasText(req.getNote());
    }
}