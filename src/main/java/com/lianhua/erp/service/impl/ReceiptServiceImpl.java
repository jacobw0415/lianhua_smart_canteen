package com.lianhua.erp.service.impl;

import com.lianhua.erp.domain.*;
import com.lianhua.erp.dto.receipt.*;
import com.lianhua.erp.event.ReceiptEvent;
import com.lianhua.erp.mapper.ReceiptMapper;
import com.lianhua.erp.repository.OrderRepository;
import com.lianhua.erp.repository.ReceiptRepository;
import com.lianhua.erp.service.OrderService; // 🚀 補齊匯入
import com.lianhua.erp.service.ReceiptService;
import com.lianhua.erp.service.impl.spec.ReceiptSpecifications;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ReceiptServiceImpl implements ReceiptService {

    private final ReceiptRepository receiptRepository;
    private final OrderRepository orderRepository;
    private final OrderService orderService; // 🚀 注入 OrderService 以便同步 Order Table
    private final ReceiptMapper mapper;
    private final ApplicationEventPublisher eventPublisher;

    // =====================================================
    // 建立收款（金額自動計算，不可超收）
    // =====================================================
    @Override
    public ReceiptResponseDto create(ReceiptRequestDto dto) {
        Order order = orderRepository.findById(dto.getOrderId())
                .orElseThrow(() -> new EntityNotFoundException("找不到訂單 ID：" + dto.getOrderId()));

        BigDecimal paidAmount = receiptRepository.sumAmountByOrderId(order.getId());
        if (paidAmount == null) {
            paidAmount = BigDecimal.ZERO;
        }

        BigDecimal receivable = order.getTotalAmount().subtract(paidAmount);
        if (receivable.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "此訂單已完成收款，無法再新增收款紀錄");
        }

        Receipt receipt = mapper.toEntity(dto);
        receipt.setOrder(order);
        receipt.setAmount(receivable);

        if (receipt.getReceivedDate() == null) {
            receipt.setReceivedDate(LocalDate.now());
        }

        receipt.setAccountingPeriod(receipt.getReceivedDate().format(DateTimeFormatter.ofPattern("yyyy-MM")));
        Receipt saved = receiptRepository.save(receipt);

        // ⭐ 發送新增收款事件通知
        Map<String, Object> payload = new HashMap<>();
        payload.put("no", order.getOrderNo());
        payload.put("amount", saved.getAmount());
        log.info("🚀 發送新增收款事件：訂單編號 {}", order.getOrderNo());
        eventPublisher.publishEvent(new ReceiptEvent(this, saved, "RECEIPT_CREATED", payload));

        // ⭐ 重算狀態
        recalcPaymentStatus(order);
        advanceOrderStatusIfNeeded(order);

        log.info("✅ 新增收款成功：orderId={}, amount={}", order.getId(), saved.getAmount());
        return mapper.toDto(saved);
    }

    // =====================================================
    // 更新收款（禁止修改金額）
    // =====================================================
    @Override
    public ReceiptResponseDto update(Long id, ReceiptRequestDto dto) {
        Receipt receipt = receiptRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("找不到收款 ID：" + id));

        if (receipt.getStatus() == ReceiptStatus.VOIDED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "已作廢的收款單不可修改");
        }

        // 1. 保存原始金額（因為金額由系統計算，不應被 Mapper 覆蓋）
        BigDecimal originalAmount = receipt.getAmount();

        // 2. 使用 Mapper 自動更新其他欄位 (receivedDate, method 等)
        // 這裡會自動處理日期變動，但備註會因為 IGNORE 策略被跳過
        mapper.updateEntityFromDto(dto, receipt);

        // 3. 回填原始金額，確保安全性
        receipt.setAmount(originalAmount);

        // 4. 手動處理備註：解決 IGNORE 導致無法清空的問題
        if (dto.getNote() != null) {
            String note = dto.getNote().trim();
            if (note.length() > 500) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "備註長度不可超過500個字元。");
            }
            receipt.setNote(note);
        } else {
            receipt.setNote(null); // 強制清空
        }

        // 5. 連動更新會計期間 (只要日期有變動就重算)
        if (receipt.getReceivedDate() != null) {
            receipt.setAccountingPeriod(receipt.getReceivedDate().format(DateTimeFormatter.ofPattern("yyyy-MM")));
        }

        receiptRepository.save(receipt);
        recalcPaymentStatus(receipt.getOrder());
        advanceOrderStatusIfNeeded(receipt.getOrder());

        return mapper.toDto(receipt);
    }

    // =====================================================
    // 刪除收款
    // =====================================================
    @Override
    public void delete(Long id) {
        Receipt receipt = receiptRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("找不到收款 ID：" + id));

        if (receipt.getStatus() == ReceiptStatus.VOIDED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "已作廢的收款單不可刪除");
        }

        Order order = receipt.getOrder();
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "訂單已完成收款，不可刪除收款紀錄");
        }

        receiptRepository.delete(receipt);
        recalcPaymentStatus(order);
        advanceOrderStatusIfNeeded(order);
    }

    // =====================================================
    // ✅ 作廢收款單（同步更新 Order Table 解決閃跳與資料一致性）
    // =====================================================
    @Override
    public ReceiptResponseDto voidReceipt(Long id, String reason) {
        Receipt receipt = receiptRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("找不到收款 ID：" + id));

        if (receipt.getStatus() == ReceiptStatus.VOIDED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "此收款單已經作廢");
        }

        // 1. 更新收款單狀態
        receipt.setStatus(ReceiptStatus.VOIDED);
        receipt.setVoidedAt(LocalDateTime.now());
        receipt.setVoidReason(reason);
        receiptRepository.save(receipt);

        // 2. 重新計算關聯訂單狀態
        Order order = receipt.getOrder();
        recalcPaymentStatus(order);
        advanceOrderStatusIfNeeded(order);

        // 3. 🚀 關鍵修正：同步更新 orders 資料表中的作廢欄位
        if (order != null && order.getOrderNo() != null) {
            orderService.voidOrder(order.getOrderNo(), reason);
            log.info("✅ 已同步作廢資訊至訂單表：orderNo={}", order.getOrderNo());
        }

        // 4. 發送作廢事件通知
        Map<String, Object> payload = new HashMap<>();
        payload.put("no", order != null ? order.getOrderNo() : "N/A");
        payload.put("amount", receipt.getAmount());
        payload.put("reason", reason);

        log.info("🚀 發送收款單作廢事件：訂單 {}", order != null ? order.getOrderNo() : "N/A");
        eventPublisher.publishEvent(new ReceiptEvent(this, receipt, "RECEIPT_VOIDED", payload));

        return mapper.toDto(receipt);
    }

    // =====================================================
    // 查詢相關方法
    // =====================================================
    @Override
    @Transactional(readOnly = true)
    public Page<ReceiptResponseDto> findAll(Pageable pageable) {
        Specification<Receipt> fetchSpec = (root, query, cb) -> {
            if (!query.getResultType().equals(Long.class) && !query.getResultType().equals(long.class)) {
                root.fetch("order", jakarta.persistence.criteria.JoinType.LEFT);
            }
            return null;
        };
        return receiptRepository.findAll(fetchSpec, pageable).map(mapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public ReceiptResponseDto findById(Long id) {
        return receiptRepository.findById(id).map(mapper::toDto)
                .orElseThrow(() -> new EntityNotFoundException("找不到收款 ID：" + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReceiptResponseDto> findByOrderId(Long orderId) {
        return receiptRepository.findByOrderId(orderId).stream().map(mapper::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReceiptResponseDto> searchReceipts(ReceiptSearchRequest req, Pageable pageable) {
        if (isSearchRequestEmpty(req)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "搜尋條件不可全為空");
        }
        Specification<Receipt> spec = ReceiptSpecifications.build(req);
        return receiptRepository.findAll(spec, pageable).map(mapper::toDto);
    }

    // =====================================================
    // ⭐ 狀態重算邏輯
    // =====================================================
    private void recalcPaymentStatus(Order order) {
        BigDecimal paidAmount = receiptRepository.sumAmountByOrderId(order.getId());
        if (paidAmount == null) paidAmount = BigDecimal.ZERO;

        BigDecimal totalAmount = order.getTotalAmount();
        boolean hasAnyReceipt = receiptRepository.hasAnyReceiptByOrderId(order.getId());

        if (paidAmount.compareTo(BigDecimal.ZERO) == 0) {
            // 如果所有收款都被作廢，但曾經有過紀錄，維持 PAID 狀態防止誤刪 (依據您的業務邏輯註解)
            order.setPaymentStatus(hasAnyReceipt ? PaymentStatus.PAID : PaymentStatus.UNPAID);
        } else {
            order.setPaymentStatus(PaymentStatus.PAID);
        }
        orderRepository.save(order);
    }

    private void advanceOrderStatusIfNeeded(Order order) {
        if (order.getPaymentStatus() == PaymentStatus.PAID &&
                (order.getOrderStatus() == OrderStatus.PENDING || order.getOrderStatus() == OrderStatus.CONFIRMED)) {
            order.setOrderStatus(OrderStatus.DELIVERED);
            orderRepository.save(order);
        }
    }

    private boolean isSearchRequestEmpty(ReceiptSearchRequest req) {
        return req.getId() == null && isEmpty(req.getCustomerName()) && isEmpty(req.getOrderNo()) &&
                isEmpty(req.getMethod()) && isEmpty(req.getAccountingPeriod()) &&
                req.getReceivedDateFrom() == null && req.getReceivedDateTo() == null && isEmpty(req.getStatus());
    }

    private boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }
}