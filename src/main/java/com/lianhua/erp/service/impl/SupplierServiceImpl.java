package com.lianhua.erp.service.impl;

import com.lianhua.erp.domain.Supplier;
import com.lianhua.erp.dto.supplier.SupplierDto;
import com.lianhua.erp.dto.supplier.SupplierRequestDto;
import com.lianhua.erp.dto.supplier.SupplierSearchRequest;
import com.lianhua.erp.mapper.SupplierMapper;
import com.lianhua.erp.repository.SupplierRepository;
import com.lianhua.erp.repository.PurchaseRepository;
import com.lianhua.erp.service.SupplierService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class SupplierServiceImpl implements SupplierService {

    private final SupplierRepository supplierRepository;
    private final SupplierMapper supplierMapper;
    private final PurchaseRepository purchaseRepository;

    // ================================================================
    // 分頁取得全部供應商
    // ================================================================
    @Override
    @Transactional(readOnly = true)
    public Page<SupplierDto> getAllSuppliers(Pageable pageable) {

        Pageable safePageable = normalizePageable(pageable);

        try {
            return supplierRepository.findAll(safePageable)
                    .map(supplierMapper::toDto);
        } catch (PropertyReferenceException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "無效排序欄位：" + ex.getPropertyName()
            );
        }
    }

    // ================================================================
    // 單筆查詢
    // ================================================================
    @Override
    @Transactional(readOnly = true)
    public SupplierDto getSupplierById(Long id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("找不到供應商 ID：" + id));

        return supplierMapper.toDto(supplier);
    }

    // ================================================================
    // 建立供應商
    // ================================================================
    @Override
    public SupplierDto createSupplier(SupplierRequestDto dto) {

        if (supplierRepository.existsByName(dto.getName())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "供應商名稱已存在：" + dto.getName()
            );
        }

        Supplier supplier = supplierMapper.toEntity(dto);
        supplier.setActive(true);
        supplier = supplierRepository.save(supplier);

        return supplierMapper.toDto(supplier);
    }

    // ================================================================
    // 更新供應商
    // ================================================================
    @Override
    public SupplierDto updateSupplier(Long id, SupplierRequestDto dto) {

        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("找不到供應商 ID：" + id));

        // 名稱變更才檢查是否重複
        if (!supplier.getName().equals(dto.getName())
                && supplierRepository.existsByName(dto.getName())) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "供應商名稱已存在：" + dto.getName()
            );
        }

        supplierMapper.updateEntityFromDto(dto, supplier);

        try {
            supplier = supplierRepository.save(supplier);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    STR."更新供應商失敗，名稱可能已存在：\{dto.getName()}", ex
            );
        }

        return supplierMapper.toDto(supplier);
    }

    // ================================================================
    // 停用供應商（active = false）
    // ================================================================
    @Override
    public SupplierDto deactivateSupplier(Long id) {

        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("找不到供應商 ID：" + id));

        supplier.setActive(false);
        supplierRepository.save(supplier);

        return supplierMapper.toDto(supplier);
    }

    // ================================================================
    // 啟用供應商（active = true）
    // ================================================================
    @Override
    public SupplierDto activateSupplier(Long id) {

        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("找不到供應商 ID：" + id));

        supplier.setActive(true);
        supplierRepository.save(supplier);

        return supplierMapper.toDto(supplier);
    }

    // ================================================================
    // 刪除供應商
    // ================================================================
    @Override
    public void deleteSupplier(Long id) {

        // 1️⃣ 確認供應商存在
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("找不到供應商 ID：" + id));

        // 2️⃣ 檢查供應商是否仍有進貨紀錄
        boolean hasPurchase = purchaseRepository.existsBySupplierId(id);
        if (hasPurchase) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    STR."無法刪除供應商：「\{supplier.getName()}」，因已存在相關進貨單紀錄。請改為停用供應商。"
            );
        }

        // 3️⃣ 通過檢查才可刪除
        supplierRepository.delete(supplier);
    }

    // ================================================================
    // 分頁搜尋供應商
    // ================================================================
    @Override
    @Transactional(readOnly = true)
    public Page<SupplierDto> searchSuppliers(SupplierSearchRequest req, Pageable pageable) {

        if (isEmptySearch(req)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "搜尋條件不可全為空，至少需提供一項搜尋欄位"
            );
        }

        Pageable safePageable = normalizePageable(pageable);
        Specification<Supplier> spec = buildSupplierSpec(req);

        Page<Supplier> page;

        try {
            page = supplierRepository.findAll(spec, safePageable);
        } catch (PropertyReferenceException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    STR."無效排序欄位：\{ex.getPropertyName()}"
            );
        }

        if (page.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "查無匹配的供應商資料，請調整搜尋條件"
            );
        }

        return page.map(supplierMapper::toDto);
    }

    // ================================================================
    // 建立 Specification（搜尋邏輯最佳化）
    // ================================================================
    private Specification<Supplier> buildSupplierSpec(SupplierSearchRequest req) {

        return (root, query, cb) -> {

            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            if (hasText(req.getSupplierName())) {
                predicates.add(cb.like(cb.lower(root.get("name")),
                        STR."%\{req.getSupplierName().toLowerCase()}%"));
            }

            if (hasText(req.getContact())) {
                predicates.add(cb.like(cb.lower(root.get("contact")),
                        STR."%\{req.getContact().toLowerCase()}%"));
            }

            if (hasText(req.getPhone())) {
                predicates.add(cb.like(cb.lower(root.get("phone")),
                        STR."%\{req.getPhone().toLowerCase()}%"));
            }

            if (hasText(req.getBillingCycle())) {
                predicates.add(cb.equal(
                        cb.lower(root.get("billingCycle")),
                        req.getBillingCycle().toLowerCase()
                ));
            }

            if (hasText(req.getNote())) {
                predicates.add(cb.like(cb.lower(root.get("note")),
                        STR."%\{req.getNote().toLowerCase()}%"));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    // ================================================================
    // 分頁防呆 + 預設排序
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

    private boolean isEmptySearch(SupplierSearchRequest req) {
        return !hasText(req.getSupplierName()) &&
                !hasText(req.getContact()) &&
                !hasText(req.getPhone()) &&
                !hasText(req.getBillingCycle()) &&
                !hasText(req.getNote());
    }

    // ================================================================
    // 取得所有啟用中的供應商（for 新增進貨單 Dropdown）
    // ================================================================
    @Override
    @Transactional(readOnly = true)
    public List<SupplierDto> getActiveSuppliers() {

        List<Supplier> suppliers = supplierRepository.findByActiveTrueOrderByNameAsc();

        if (suppliers.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "目前沒有可用（啟用中）的供應商"
            );
        }

        return suppliers.stream()
                .map(supplierMapper::toDto)
                .toList();
    }

}
