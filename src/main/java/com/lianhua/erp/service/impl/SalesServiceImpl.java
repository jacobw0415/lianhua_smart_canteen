package com.lianhua.erp.service.impl;

import com.lianhua.erp.domain.Product;
import com.lianhua.erp.domain.Sale;
import com.lianhua.erp.dto.sale.*;
import com.lianhua.erp.mapper.SalesMapper;
import com.lianhua.erp.repository.ProductRepository;
import com.lianhua.erp.repository.SalesRepository;
import com.lianhua.erp.service.SalesService;
import com.lianhua.erp.service.impl.spec.SaleSpecifications;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SalesServiceImpl implements SalesService {

    private final SalesRepository repository;
    private final ProductRepository productRepository;
    private final SalesMapper mapper;

    private static final DateTimeFormatter PERIOD_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    // === 建立銷售紀錄 ===
    @Override
    public SalesResponseDto create(SalesRequestDto dto) {
        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new EntityNotFoundException("找不到商品 ID: " + dto.getProductId()));

        // 檢查唯一約束 (同日期 + 同商品)
        if (repository.existsBySaleDateAndProductId(dto.getSaleDate(), dto.getProductId())) {
            throw new DataIntegrityViolationException("該商品於該日期已有銷售紀錄，請勿重複建立。");
        }

        Sale sale = mapper.toEntity(dto);
        sale.setProduct(product);

        // ✅ 自動設定會計期間（依銷售日期）
        if (sale.getSaleDate() != null) {
            sale.setAccountingPeriod(sale.getSaleDate().format(PERIOD_FORMAT));
        } else {
            sale.setSaleDate(LocalDate.now());
            sale.setAccountingPeriod(LocalDate.now().format(PERIOD_FORMAT));
        }

        // ✅ 自動計算金額（商品單價 × 數量）
        BigDecimal unitPrice = product.getUnitPrice();
        BigDecimal total = unitPrice.multiply(BigDecimal.valueOf(dto.getQty()));
        sale.setAmount(total);

        return mapper.toDto(repository.save(sale));
    }

    // === 更新銷售紀錄 ===
    @Override
    public SalesResponseDto update(Long id, SalesRequestDto dto) {
        Sale existing = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("找不到銷售紀錄 ID: " + id));

        // 檢查是否違反唯一約束（若修改日期或商品）
        if (dto.getSaleDate() != null && dto.getProductId() != null &&
                repository.existsBySaleDateAndProductId(dto.getSaleDate(), dto.getProductId()) &&
                !dto.getProductId().equals(existing.getProduct().getId())) {
            throw new DataIntegrityViolationException("該商品於該日期已有銷售紀錄，請勿重複建立。");
        }

        // 更新基本屬性
        mapper.updateEntityFromDto(dto, existing);

        // 若日期變更 → 同步更新會計期間
        if (dto.getSaleDate() != null) {
            existing.setAccountingPeriod(dto.getSaleDate().format(PERIOD_FORMAT));
        }

        // 若商品 ID 被更新，需重新查詢商品資料
        if (dto.getProductId() != null && !dto.getProductId().equals(existing.getProduct().getId())) {
            Product newProduct = productRepository.findById(dto.getProductId())
                    .orElseThrow(() -> new EntityNotFoundException("找不到商品 ID: " + dto.getProductId()));
            existing.setProduct(newProduct);
        }

        // ✅ 自動重新計算金額
        BigDecimal unitPrice = existing.getProduct().getUnitPrice();
        BigDecimal total = unitPrice.multiply(BigDecimal.valueOf(existing.getQty()));
        existing.setAmount(total);

        return mapper.toDto(repository.save(existing));
    }

    // === 支援分頁 PAGE ===
    @Override
    @Transactional(readOnly = true)
    public Page<SalesResponseDto> getAllSales(Pageable pageable) {

        return repository.findAll(pageable)
                .map(mapper::toDto);
    }

    // === 模糊搜尋 ===
    @Override
    @Transactional(readOnly = true)
    public Page<SalesResponseDto> search(
            SaleSearchRequestDto req,
            Pageable pageable
    ) {

        Specification<Sale> spec = SaleSpecifications.build(req);

        return repository.findAll(spec, pageable)
                .map(mapper::toDto);
    }

    // === 刪除銷售紀錄 ===
    @Override
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new EntityNotFoundException("找不到銷售紀錄 ID: " + id);
        }
        repository.deleteById(id);
    }

    // === 查詢單筆 ===
    @Override
    @Transactional(readOnly = true)
    public SalesResponseDto findById(Long id) {
        return repository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new EntityNotFoundException("找不到銷售紀錄 ID: " + id));
    }


    // === 查詢指定商品的銷售紀錄 ===
    @Override
    @Transactional(readOnly = true)
    public List<SalesResponseDto> findByProduct(Long productId) {
        return repository.findByProductId(productId).stream().map(mapper::toDto).collect(Collectors.toList());
    }
}
