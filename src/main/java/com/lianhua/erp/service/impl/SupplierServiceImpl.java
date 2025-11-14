package com.lianhua.erp.service.impl;

import com.lianhua.erp.domain.Supplier;
import com.lianhua.erp.dto.supplier.SupplierDto;
import com.lianhua.erp.dto.supplier.SupplierRequestDto;
import com.lianhua.erp.dto.supplier.SupplierSearchRequest;
import com.lianhua.erp.mapper.SupplierMapper;
import com.lianhua.erp.repository.SupplierRepository;
import com.lianhua.erp.service.SupplierService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class SupplierServiceImpl implements SupplierService {

    private final SupplierRepository supplierRepository;
    private final SupplierMapper supplierMapper;

    @Override
    @Transactional(readOnly = true)
    public List<SupplierDto> getAllSuppliers() {
        return supplierRepository.findAll()
                .stream()
                .map(supplierMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierDto getSupplierById(Long id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("找不到供應商 ID：" + id));
        return supplierMapper.toDto(supplier);
    }

    @Override
    public SupplierDto createSupplier(SupplierRequestDto dto) {
        if (supplierRepository.existsByName(dto.getName())) {
            throw new IllegalArgumentException("供應商名稱已存在：" + dto.getName());
        }
        Supplier supplier = supplierMapper.toEntity(dto);
        return supplierMapper.toDto(supplierRepository.save(supplier));
    }

    @Override
    public SupplierDto updateSupplier(Long id, SupplierRequestDto dto) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("找不到供應商 ID：" + id));

        if (!supplier.getName().equals(dto.getName())
                && supplierRepository.existsByName(dto.getName())) {
            throw new IllegalArgumentException("供應商名稱已存在：" + dto.getName());
        }

        supplierMapper.updateEntityFromDto(dto, supplier);

        try {
            supplier = supplierRepository.save(supplier);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException("更新供應商失敗，名稱可能已存在：" + dto.getName(), ex);
        }

        return supplierMapper.toDto(supplier);
    }

    @Override
    public void deleteSupplier(Long id) {
        if (!supplierRepository.existsById(id)) {
            throw new EntityNotFoundException("找不到供應商 ID：" + id);
        }
        supplierRepository.deleteById(id);
    }

    // ================================================================
    // 供應商搜尋邏輯（含欄位客製化訊息）
    // ================================================================
    @Override
    @Transactional(readOnly = true)
    public List<SupplierDto> searchSuppliers(SupplierSearchRequest req) {

        // 🔍 至少一個搜尋條件必須提供
        if (isEmptySearch(req)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "搜尋條件不可全為空，至少需提供一項搜尋欄位"
            );
        }

        Specification<Supplier> spec = Specification.unrestricted();
        StringBuilder searchInfo = new StringBuilder("查無匹配資料：");

        // 1️⃣ 供應商名稱（模糊搜尋）
        if (hasText(req.getSupplierName())) {
            String keyword = req.getSupplierName().trim().toLowerCase();
            searchInfo.append(STR."供應商名稱「\{req.getSupplierName()}」 ");

            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("name")), STR."%\{keyword}%"));
        }

        // 2️⃣ 聯絡人（模糊搜尋）
        if (hasText(req.getContact())) {
            String keyword = req.getContact().trim().toLowerCase();
            searchInfo.append(STR."聯絡人「\{req.getContact()}」 ");

            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("contact")), STR."%\{keyword}%"));
        }

        // 3️⃣ 電話（模糊搜尋）
        if (hasText(req.getPhone())) {
            String keyword = req.getPhone().trim();
            searchInfo.append(STR."電話「\{req.getPhone()}」 ");

            spec = spec.and((root, query, cb) ->
                    cb.like(root.get("phone"), STR."%\{keyword}%"));
        }

        // 4️⃣ 結帳週期（ENUM 精確搜尋）
        if (hasText(req.getBillingCycle())) {
            searchInfo.append(STR."結帳週期「\{req.getBillingCycle()}」 ");

            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("billingCycle"), req.getBillingCycle()));
        }

        // 5️⃣ 備註（模糊搜尋）
        if (hasText(req.getNote())) {
            String keyword = req.getNote().trim().toLowerCase();
            searchInfo.append(STR."備註「\{req.getNote()}」 ");

            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("note")), STR."%\{keyword}%"));
        }

        List<Supplier> results = supplierRepository.findAll(spec);

        // ❌ 沒結果 → 客製化錯誤
        if (results.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    searchInfo.append("未找到符合的供應商資料").toString()
            );
        }

        // ✔ 有結果
        return results.stream()
                .map(supplierMapper::toDto)
                .toList();
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
}
