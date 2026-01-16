package com.lianhua.erp.service.impl;

import com.lianhua.erp.domain.*;
import com.lianhua.erp.dto.receipt.*;
import com.lianhua.erp.mapper.ReceiptMapper;
import com.lianhua.erp.repository.OrderRepository;
import com.lianhua.erp.repository.ReceiptRepository;
import com.lianhua.erp.service.ReceiptService;
import com.lianhua.erp.service.impl.spec.ReceiptSpecifications;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ReceiptServiceImpl implements ReceiptService {

    private final ReceiptRepository receiptRepository;
    private final OrderRepository orderRepository;
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
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "此訂單已完成收款，無法再新增收款紀錄");
        }

        Receipt receipt = mapper.toEntity(dto);
        receipt.setOrder(order);

        // 🔐 金額只在建立時計算
        receipt.setAmount(receivable);

        // 收款日期預設今日
        if (receipt.getReceivedDate() == null) {
            receipt.setReceivedDate(LocalDate.now());
        }

        // 會計期間 yyyy-MM
        receipt.setAccountingPeriod(
                receipt.getReceivedDate()
                        .format(DateTimeFormatter.ofPattern("yyyy-MM")));

        receiptRepository.save(receipt);

        Receipt saved = receiptRepository.save(receipt);

        // ⭐ 新增：發送新增收款事件通知
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("no", order.getOrderNo()); // 關聯訂單編號
        payload.put("amount", saved.getAmount());

        log.info("🚀 發送新增收款事件：訂單編號 {}", order.getOrderNo());
        eventPublisher.publishEvent(new com.lianhua.erp.event.ReceiptEvent(this, saved, "RECEIPT_CREATED", payload));

        // ⭐ 重算狀態
        recalcPaymentStatus(order);
        advanceOrderStatusIfNeeded(order);

        log.info("✅ 新增收款：orderId={}, amount={}",
                order.getId(), receipt.getAmount());

        return mapper.toDto(receipt);
    }

    // =====================================================
    // 更新收款（禁止修改金額）
    // =====================================================
    @Override
    public ReceiptResponseDto update(Long id, ReceiptRequestDto dto) {

        Receipt receipt = receiptRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("找不到收款 ID：" + id));

        // ⚠️ 已作廢的收款單不可修改
        if (receipt.getStatus() == ReceiptStatus.VOIDED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "已作廢的收款單不可修改");
        }

        BigDecimal originalAmount = receipt.getAmount(); // 🔒 鎖金額

        mapper.updateEntityFromDto(dto, receipt);

        // 強制還原金額（防止 Mapper 誤改）
        receipt.setAmount(originalAmount);

        // 若有修改收款日期，重新計算會計期間
        if (receipt.getReceivedDate() != null) {
            receipt.setAccountingPeriod(
                    receipt.getReceivedDate()
                            .format(DateTimeFormatter.ofPattern("yyyy-MM")));
        }

        receiptRepository.save(receipt);

        Order order = receipt.getOrder();

        // ⭐ 重算狀態
        recalcPaymentStatus(order);
        advanceOrderStatusIfNeeded(order);

        log.info("✏️ 更新收款：receiptId={}, amount={}",
                id, receipt.getAmount());

        return mapper.toDto(receipt);
    }

    // =====================================================
    // 刪除收款（已完成收款不可刪）
    // =====================================================
    @Override
    public void delete(Long id) {

        Receipt receipt = receiptRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("找不到收款 ID：" + id));

        // ⚠️ 已作廢的收款單不可刪除（應保留記錄）
        if (receipt.getStatus() == ReceiptStatus.VOIDED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "已作廢的收款單不可刪除");
        }

        Order order = receipt.getOrder();

        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "訂單已完成收款，不可刪除收款紀錄");
        }

        receiptRepository.delete(receipt);

        // ⭐ 刪除後重算
        recalcPaymentStatus(order);
        advanceOrderStatusIfNeeded(order);

        log.info("🗑️ 刪除收款：receiptId={}, orderId={}", id, order.getId());
    }

    // =====================================================
    // 作廢收款單
    // =====================================================
    @Override
    public ReceiptResponseDto voidReceipt(Long id, String reason) {
        Receipt receipt = receiptRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("找不到收款 ID：" + id));

        // 檢查是否已作廢
        if (receipt.getStatus() == ReceiptStatus.VOIDED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "此收款單已經作廢");
        }

        // ⭐ 任何狀態都可以作廢（不需要檢查付款狀態）
        receipt.setStatus(ReceiptStatus.VOIDED);
        receipt.setVoidedAt(LocalDateTime.now());
        receipt.setVoidReason(reason);

        receiptRepository.save(receipt);

        // ⭐ 重新計算關聯訂單的付款狀態（自動排除已作廢的收款）
        Order order = receipt.getOrder();
        recalcPaymentStatus(order);
        advanceOrderStatusIfNeeded(order);

        log.info("✅ 作廢收款：receiptId={}, orderId={}, reason={}",
                id, order.getId(), reason);

        // ⭐ 重新查詢以確保關聯資料被載入（用於映射 orderNo 和 customerName）
        Receipt savedReceipt = receiptRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("找不到收款 ID：" + id));

        // ⭐ 關鍵修正：封裝 Payload 並發送作廢事件
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("no", order.getOrderNo()); // 關聯訂單編號
        payload.put("amount", savedReceipt.getAmount());
        payload.put("reason", reason); // 傳遞作廢原因

        log.info("🚀 發送收款單作廢事件：訂單 {}", order.getOrderNo());
        // 發送 RECEIPT_VOIDED 事件
        eventPublisher.publishEvent(new com.lianhua.erp.event.ReceiptEvent(this, savedReceipt, "RECEIPT_VOIDED", payload));

        // 返回更新後的收款單 DTO（滿足 React Admin 的要求）
        return mapper.toDto(savedReceipt);
    }

    // =====================================================
    // 查詢
    // =====================================================
    @Override
    @Transactional(readOnly = true)
    public Page<ReceiptResponseDto> findAll(Pageable pageable) {
        // 使用 Specification 確保關聯資料被載入（用於映射 orderNo 和 customerName）
        // 顯示所有收款（包括已作廢的），前端可透過 status 欄位區分
        Specification<Receipt> fetchSpec = (root, query, cb) -> {
            if (!query.getResultType().equals(Long.class) && !query.getResultType().equals(long.class)) {
                root.fetch("order", jakarta.persistence.criteria.JoinType.LEFT);
                root.fetch("order", jakarta.persistence.criteria.JoinType.LEFT)
                        .fetch("customer", jakarta.persistence.criteria.JoinType.LEFT);
            }
            return null;
        };

        // 顯示所有收款（包括已作廢的）
        return receiptRepository.findAll(fetchSpec, pageable)
                .map(mapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public ReceiptResponseDto findById(Long id) {
        return receiptRepository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new EntityNotFoundException("找不到收款 ID：" + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReceiptResponseDto> findByOrderId(Long orderId) {
        return receiptRepository.findByOrderId(orderId)
                .stream()
                .map(mapper::toDto)
                .toList();
    }

    // =====================================================
    // 收款搜尋（Specification）
    // =====================================================
    @Override
    @Transactional(readOnly = true)
    public Page<ReceiptResponseDto> searchReceipts(
            ReceiptSearchRequest req,
            Pageable pageable) {

        // 檢查是否至少有一項搜尋條件（includeVoided 和 status 不計入搜尋條件）
        boolean empty = req.getId() == null &&
                isEmpty(req.getCustomerName()) &&
                isEmpty(req.getOrderNo()) &&
                isEmpty(req.getMethod()) &&
                isEmpty(req.getAccountingPeriod()) &&
                isEmpty(req.getFromDate()) &&
                isEmpty(req.getToDate()) &&
                req.getReceivedDateFrom() == null &&
                req.getReceivedDateTo() == null &&
                isEmpty(req.getStatus());

        if (empty) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "搜尋條件不可全為空，至少需提供一項搜尋欄位");
        }

        Specification<Receipt> spec = ReceiptSpecifications.build(req);

        try {
            return receiptRepository.findAll(spec, pageable)
                    .map(mapper::toDto);
        } catch (PropertyReferenceException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "無效排序欄位：" + ex.getPropertyName());
        }
    }

    // =====================================================
    // ⭐ 核心：重算 payment_status
    // =====================================================
    private void recalcPaymentStatus(Order order) {

        BigDecimal paidAmount = receiptRepository.sumAmountByOrderId(order.getId());

        if (paidAmount == null) {
            paidAmount = BigDecimal.ZERO;
        }

        BigDecimal totalAmount = order.getTotalAmount();

        // ⭐ 如果订单曾经有收款记录（包括已作废的），即使现在有效收款为0，也应该保持 PAID 状态
        // 这样可以防止已收款的订单在收款单被作废后变成 UNPAID，从而被错误地取消或删除
        boolean hasAnyReceipt = receiptRepository.hasAnyReceiptByOrderId(order.getId());

        if (paidAmount.compareTo(BigDecimal.ZERO) == 0) {
            // 如果曾经有收款记录，即使现在都被作废了，也应该保持 PAID 状态
            if (hasAnyReceipt) {
                order.setPaymentStatus(PaymentStatus.PAID);
            } else {
                order.setPaymentStatus(PaymentStatus.UNPAID);
            }
        } else if (paidAmount.compareTo(totalAmount) < 0) {
            // 部分收款：由於 PaymentStatus 只有 UNPAID 和 PAID，部分收款也視為 PAID
            order.setPaymentStatus(PaymentStatus.PAID);
        } else {
            // 已全額收款
            order.setPaymentStatus(PaymentStatus.PAID);
        }

        orderRepository.save(order);
    }

    // =====================================================
    // ⭐ 業務流程推進（可未來抽成策略）
    // =====================================================
    private void advanceOrderStatusIfNeeded(Order order) {

        if (order.getPaymentStatus() == PaymentStatus.PAID &&
                (order.getOrderStatus() == OrderStatus.PENDING
                        || order.getOrderStatus() == OrderStatus.CONFIRMED)) {

            order.setOrderStatus(OrderStatus.DELIVERED);
            orderRepository.save(order);
        }
    }

    // =====================================================
    // 工具
    // =====================================================
    private boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }
}
