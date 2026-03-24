package com.lianhua.erp.service.impl;

import com.lianhua.erp.domain.Product;
import com.lianhua.erp.domain.Sale;
import com.lianhua.erp.dto.export.ExportPayload;
import com.lianhua.erp.dto.sale.*;
import com.lianhua.erp.export.ExportFilenameUtils;
import com.lianhua.erp.export.ExportFormat;
import com.lianhua.erp.export.ExportScope;
import com.lianhua.erp.export.TabularExporter;
import com.lianhua.erp.mapper.SalesMapper;
import com.lianhua.erp.repository.ProductRepository;
import com.lianhua.erp.repository.SalesRepository;
import com.lianhua.erp.service.SalesService;
import com.lianhua.erp.service.impl.spec.SaleSpecifications;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SalesServiceImpl implements SalesService {

    private static final String[] SALES_EXPORT_HEADERS = new String[]{
            "銷售日期", "商品名稱", "銷售數量", "付款方式", "銷售金額"
    };

    private final SalesRepository repository;
    private final ProductRepository productRepository;
    private final SalesMapper mapper;

    @Value("${app.export.max-rows:50000}")
    private int maxExportRows;

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

    // === 匯出銷售列表 ===
    @Override
    @Transactional(readOnly = true)
    public ExportPayload exportSales(
            SaleSearchRequestDto req,
            Pageable pageable,
            ExportFormat format,
            ExportScope scope
    ) {
        SaleSearchRequestDto request = req == null ? new SaleSearchRequestDto() : req;
        ExportFormat safeFormat = format == null ? ExportFormat.XLSX : format;
        ExportScope safeScope = scope == null ? ExportScope.ALL : scope;
        Sort safeSort = pageable != null && pageable.getSort().isSorted()
                ? pageable.getSort()
                : Sort.by(Sort.Direction.ASC, "id");
        Specification<Sale> spec = SaleSpecifications.build(request);
        List<String[]> rows = new ArrayList<>();

        try {
            if (safeScope == ExportScope.ALL) {
                long total = repository.count(spec);
                if (total > maxExportRows) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "匯出筆數超過上限 (" + maxExportRows + ")，請縮小篩選條件");
                }
                int step = 1000;
                int pages = (int) ((total + step - 1) / step);
                for (int p = 0; p < pages; p++) {
                    Page<Sale> page = repository.findAll(spec, PageRequest.of(p, step, safeSort));
                    for (Sale sale : page.getContent()) {
                        rows.add(toSalesExportRow(mapper.toDto(sale)));
                    }
                }
            } else {
                Pageable p = pageable == null ? PageRequest.of(0, 25, safeSort) : PageRequest.of(
                        Math.max(pageable.getPageNumber(), 0),
                        pageable.getPageSize() <= 0 || pageable.getPageSize() > 200 ? 25 : pageable.getPageSize(),
                        safeSort
                );
                Page<Sale> page = repository.findAll(spec, p);
                for (Sale sale : page.getContent()) {
                    rows.add(toSalesExportRow(mapper.toDto(sale)));
                }
            }
        } catch (PropertyReferenceException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "無效排序欄位：" + ex.getPropertyName());
        }

        byte[] data = switch (safeFormat) {
            case XLSX -> TabularExporter.toXlsx("銷售", SALES_EXPORT_HEADERS, rows);
            case CSV -> TabularExporter.toCsvUtf8Bom(SALES_EXPORT_HEADERS, rows);
        };

        String filename = ExportFilenameUtils.build("sales", safeFormat);
        return new ExportPayload(data, filename, safeFormat.mediaType());
    }

    private static String[] toSalesExportRow(SalesResponseDto s) {
        return new String[]{
                s.getSaleDate() == null ? "" : s.getSaleDate().toString(),
                nz(s.getProductName()),
                s.getQty() == null ? "" : String.valueOf(s.getQty()),
                nz(s.getPayMethod()),
                s.getAmount() == null ? "" : s.getAmount().toPlainString()
        };
    }

    private static String nz(String s) {
        return s == null ? "" : s;
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
