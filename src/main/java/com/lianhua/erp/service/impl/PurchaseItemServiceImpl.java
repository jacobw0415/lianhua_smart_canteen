package com.lianhua.erp.service.impl;

import com.lianhua.erp.domain.*;
import com.lianhua.erp.dto.purchase.PurchaseItemDto;
import com.lianhua.erp.dto.purchase.PurchaseItemRequestDto;
import com.lianhua.erp.mapper.PurchaseItemMapper;
import com.lianhua.erp.repository.PurchaseItemRepository;
import com.lianhua.erp.repository.PurchaseRepository;
import com.lianhua.erp.service.PurchaseItemService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class PurchaseItemServiceImpl implements PurchaseItemService {

    private final PurchaseItemRepository itemRepository;
    private final PurchaseRepository purchaseRepository;
    private final PurchaseItemMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<PurchaseItemDto> findByPurchaseId(Long purchaseId) {
        // 驗證採購單存在
        purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new EntityNotFoundException("找不到採購單 ID: " + purchaseId));
        
        return itemRepository.findByPurchaseId(purchaseId)
                .stream()
                .map(mapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PurchaseItemDto> findAllPaged(Pageable pageable) {
        return itemRepository.findAll(pageable).map(mapper::toDto);
    }

    @Override
    @Transactional
    public PurchaseItemDto create(Long purchaseId, PurchaseItemRequestDto dto) {
        // =============================================
        // 1️⃣ 驗證採購單存在
        // =============================================
        Purchase purchase = purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new EntityNotFoundException("找不到採購單 ID: " + purchaseId));

        // =============================================
        // 2️⃣ 業務規則檢查：已付款或已作廢的採購單不可新增明細
        // =============================================
        if (purchase.getStatus() == Purchase.Status.PAID) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "已全額付清的採購單不可新增明細");
        }

        if (purchase.getRecordStatus() == PurchaseStatus.VOIDED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "已作廢的採購單不可新增明細");
        }

        // ✨ 新增：防止重複進貨品項（排除已作廢的進貨單）
        // 邏輯：檢查同一供應商、同一天、同一品項名稱
        boolean hasActiveDuplicate = itemRepository.existsActivePurchaseItem(
                purchase.getSupplier().getId(),
                purchase.getPurchaseDate(),
                dto.getItem().trim()
        );

        if (hasActiveDuplicate) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    String.format("重複品項：供應商於 %s 已有有效的進貨紀錄包含「%s」",
                            purchase.getPurchaseDate(), dto.getItem().trim())
            );
        }

        // =============================================
        // 3️⃣ 驗證明細資料
        // =============================================
        if (dto.getItem() == null || dto.getItem().trim().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "品項名稱不可為空");
        }
        if (dto.getUnit() == null || dto.getUnit().trim().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "單位不可為空");
        }
        if (dto.getQty() == null || dto.getQty() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "數量必須大於 0");
        }
        if (dto.getUnitPrice() == null || dto.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "單價必須大於 0");
        }

        // =============================================
        // 4️⃣ 建立明細並計算金額
        // =============================================
        PurchaseItem item = mapper.toEntity(dto);
        item.setPurchase(purchase);
        
        // 計算明細金額
        computeItemAmounts(item);

        log.info("新增採購明細：purchaseId={}, item={}, qty={}, unitPrice={}, subtotal={}",
                purchaseId, dto.getItem(), dto.getQty(), dto.getUnitPrice(), item.getSubtotal());

        itemRepository.save(item);

        // =============================================
        // 5️⃣ 重新計算採購單總金額
        // =============================================
        updatePurchaseTotalAmount(purchaseId);

        return mapper.toDto(item);
    }

    @Override
    @Transactional
    public PurchaseItemDto update(Long purchaseId, Long itemId, PurchaseItemRequestDto dto) {
        // =============================================
        // 1️⃣ 驗證明細存在
        // =============================================
        PurchaseItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("找不到採購明細 ID: " + itemId));

        // 驗證明細屬於指定的採購單
        if (!item.getPurchase().getId().equals(purchaseId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "明細不屬於指定的採購單");
        }

        Purchase purchase = item.getPurchase();

        // =============================================
        // 2️⃣ 業務規則檢查：已付款或已作廢的採購單不可修改明細
        // =============================================
        if (purchase.getStatus() == Purchase.Status.PAID) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "已全額付清的採購單不可修改明細");
        }

        if (purchase.getRecordStatus() == PurchaseStatus.VOIDED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "已作廢的採購單不可修改明細");
        }

        // =============================================
        // 3️⃣ 驗證明細資料
        // =============================================
        if (dto.getItem() == null || dto.getItem().trim().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "品項名稱不可為空");
        }
        if (dto.getUnit() == null || dto.getUnit().trim().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "單位不可為空");
        }
        if (dto.getQty() == null || dto.getQty() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "數量必須大於 0");
        }
        if (dto.getUnitPrice() == null || dto.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "單價必須大於 0");
        }

        // =============================================
        // 4️⃣ 更新明細資料
        // =============================================
        item.setItem(dto.getItem().trim());
        item.setUnit(dto.getUnit().trim());
        item.setQty(dto.getQty());
        item.setUnitPrice(dto.getUnitPrice());
        item.setNote(dto.getNote());

        // 重新計算明細金額
        computeItemAmounts(item);

        log.info("更新採購明細：purchaseId={}, itemId={}, item={}, qty={}, unitPrice={}, subtotal={}",
                purchaseId, itemId, dto.getItem(), dto.getQty(), dto.getUnitPrice(), item.getSubtotal());

        itemRepository.save(item);

        // =============================================
        // 5️⃣ 重新計算採購單總金額
        // =============================================
        updatePurchaseTotalAmount(purchaseId);

        return mapper.toDto(item);
    }

    @Override
    @Transactional
    public void delete(Long purchaseId, Long itemId) {
        // =============================================
        // 1️⃣ 驗證明細存在
        // =============================================
        PurchaseItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("找不到採購明細 ID: " + itemId));

        // 驗證明細屬於指定的採購單
        if (!item.getPurchase().getId().equals(purchaseId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "明細不屬於指定的採購單");
        }

        Purchase purchase = item.getPurchase();

        // =============================================
        // 2️⃣ 業務規則檢查：已付款或已作廢的採購單不可刪除明細
        // =============================================
        if (purchase.getStatus() == Purchase.Status.PAID) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "已全額付清的採購單不可刪除明細");
        }

        if (purchase.getRecordStatus() == PurchaseStatus.VOIDED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "已作廢的採購單不可刪除明細");
        }

        // =============================================
        // 3️⃣ 檢查是否為最後一筆明細
        // =============================================
        List<PurchaseItem> items = itemRepository.findByPurchaseId(purchaseId);
        if (items.size() <= 1) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "採購單至少需要保留一筆明細，無法刪除");
        }

        log.info("刪除採購明細：purchaseId={}, itemId={}", purchaseId, itemId);

        itemRepository.delete(item);

        // =============================================
        // 4️⃣ 重新計算採購單總金額
        // =============================================
        updatePurchaseTotalAmount(purchaseId);
    }

    /**
     * 計算明細小計（不含稅）
     */
    private void computeItemAmounts(PurchaseItem item) {
        if (item.getQty() == null || item.getUnitPrice() == null) {
            item.setSubtotal(BigDecimal.ZERO);
            return;
        }

        BigDecimal qty = BigDecimal.valueOf(item.getQty());
        BigDecimal unitPrice = item.getUnitPrice();

        // 小計 = 單價 × 數量（不含稅）
        BigDecimal subtotal = unitPrice.multiply(qty).setScale(2, RoundingMode.HALF_UP);

        item.setSubtotal(subtotal);
    }

    /**
     * 重新計算採購單總金額並更新
     */
    private void updatePurchaseTotalAmount(Long purchaseId) {
        Purchase purchase = purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new EntityNotFoundException("找不到採購單 ID: " + purchaseId));

        // 計算所有明細的總金額
        BigDecimal newTotal = itemRepository.sumTotalByPurchaseId(purchaseId);
        purchase.setTotalAmount(newTotal);

        // 重新計算餘額（balance 是計算欄位，會自動更新）
        // 但我們需要確保 paidAmount 不變，只有 totalAmount 變化

        // 更新採購單狀態（如果總金額變化，可能需要重新評估狀態）
        // 但這裡不改變狀態，因為狀態是基於付款情況的

        purchaseRepository.save(purchase);

        log.info("更新採購單總金額：purchaseId={}, newTotal={}", purchaseId, newTotal);
    }
}

