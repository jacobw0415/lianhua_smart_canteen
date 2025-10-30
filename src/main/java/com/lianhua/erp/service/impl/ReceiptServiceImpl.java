package com.lianhua.erp.service.impl;

import com.lianhua.erp.domin.*;
import com.lianhua.erp.dto.receipt.*;
import com.lianhua.erp.mapper.ReceiptMapper;
import com.lianhua.erp.repository.*;
import com.lianhua.erp.service.ReceiptService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ReceiptServiceImpl implements ReceiptService {
    
    private final ReceiptRepository repository;
    private final OrderRepository orderRepository;
    private final ReceiptMapper mapper;
    
    /**
     * 建立收款記錄（自動帶入金額、會計期與唯一檢查）
     */
    @Override
    public ReceiptResponseDto create(ReceiptRequestDto dto) {
        // ✅ 檢查訂單是否存在
        Order order = orderRepository.findById(dto.getOrderId())
                .orElseThrow(() -> new EntityNotFoundException("找不到訂單 ID: " + dto.getOrderId()));
        
        // ✅ 防止重複輸入收款
        if (repository.existsByOrderId(order.getId())) {
            throw new DataIntegrityViolationException("該訂單已存在收款記錄，請勿重複輸入。");
        }
        
        // ✅ 轉換並自動設定必要欄位
        Receipt receipt = mapper.toEntity(dto);
        receipt.setOrder(order);
        
        // 金額由訂單自動帶入
        receipt.setAmount(order.getTotalAmount());
        
        // 若未指定收款日期則自動設定為今日
        if (receipt.getReceivedDate() == null) {
            receipt.setReceivedDate(LocalDate.now());
        }
        
        // 自動設定會計期間 (yyyy-MM)
        receipt.setAccountingPeriod(receipt.getReceivedDate()
                .format(DateTimeFormatter.ofPattern("yyyy-MM")));
        
        // 付款方式（若傳入）
        if (dto.getMethod() != null) {
            try {
                receipt.setMethod(Receipt.PaymentMethod.valueOf(dto.getMethod()));
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("收款方式不合法：" + dto.getMethod());
            }
        }
        
        repository.save(receipt);
        log.info("✅ 已建立收款記錄：orderId={}, amount={}, period={}",
                order.getId(), receipt.getAmount(), receipt.getAccountingPeriod());
        
        return mapper.toDto(receipt);
    }
    
    /**
     * 更新收款記錄（僅允許修改付款方式、備註等）
     */
    @Override
    public ReceiptResponseDto update(Long id, ReceiptRequestDto dto) {
        Receipt receipt = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("找不到收款 ID: " + id));
        
        // 禁止修改金額與訂單
        mapper.updateEntityFromDto(dto, receipt);
        
        // 更新付款方式
        if (dto.getMethod() != null) {
            try {
                receipt.setMethod(Receipt.PaymentMethod.valueOf(dto.getMethod()));
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("收款方式不合法：" + dto.getMethod());
            }
        }
        
        // 再次鎖定金額與訂單（避免被 mapper 覆蓋）
        receipt.setAmount(receipt.getOrder().getTotalAmount());
        receipt.setOrder(receipt.getOrder());
        
        repository.save(receipt);
        log.info("✏️ 已更新收款記錄：receiptId={}, method={}, note={}",
                id, receipt.getMethod(), receipt.getNote());
        
        return mapper.toDto(receipt);
    }
    
    /**
     * 刪除收款記錄
     */
    @Override
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new EntityNotFoundException("找不到收款記錄 ID: " + id);
        }
        repository.deleteById(id);
        log.info("🗑️ 已刪除收款記錄 ID={}", id);
    }
    
    /**
     * 查詢全部收款記錄
     */
    @Override
    @Transactional(readOnly = true)
    public List<ReceiptResponseDto> findAll() {
        return repository.findAll()
                .stream()
                .map(mapper::toDto)
                .toList();
    }
    
    /**
     * 依訂單 ID 查詢收款記錄
     */
    @Override
    @Transactional(readOnly = true)
    public List<ReceiptResponseDto> findByOrderId(Long orderId) {
        return repository.findByOrderId(orderId)
                .stream()
                .map(mapper::toDto)
                .toList();
    }
    
    /**
     * 依收款 ID 查詢詳細資料
     */
    @Override
    @Transactional(readOnly = true)
    public ReceiptResponseDto findById(Long id) {
        return repository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new EntityNotFoundException("找不到收款記錄 ID: " + id));
    }
}
