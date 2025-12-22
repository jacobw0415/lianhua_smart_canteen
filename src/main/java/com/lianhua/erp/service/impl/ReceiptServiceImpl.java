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
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ReceiptServiceImpl implements ReceiptService {

    private final ReceiptRepository receiptRepository;
    private final OrderRepository orderRepository;
    private final ReceiptMapper mapper;

    // =====================================================
    // 建立收款（金額自動計算，不可超收）
    // =====================================================
    @Override
    public ReceiptResponseDto create(ReceiptRequestDto dto) {

        Order order = orderRepository.findById(dto.getOrderId())
                .orElseThrow(() ->
                        new EntityNotFoundException("找不到訂單 ID：" + dto.getOrderId())
                );

        BigDecimal paidAmount =
                receiptRepository.sumAmountByOrderId(order.getId());

        if (paidAmount == null) {
            paidAmount = BigDecimal.ZERO;
        }

        BigDecimal receivable =
                order.getTotalAmount().subtract(paidAmount);

        if (receivable.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "此訂單已完成收款，無法再新增收款紀錄"
            );
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
                        .format(DateTimeFormatter.ofPattern("yyyy-MM"))
        );

        receiptRepository.save(receipt);

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
                .orElseThrow(() ->
                        new EntityNotFoundException("找不到收款 ID：" + id)
                );

        BigDecimal originalAmount = receipt.getAmount(); // 🔒 鎖金額

        mapper.updateEntityFromDto(dto, receipt);

        // 強制還原金額（防止 Mapper 誤改）
        receipt.setAmount(originalAmount);

        // 若有修改收款日期，重新計算會計期間
        if (receipt.getReceivedDate() != null) {
            receipt.setAccountingPeriod(
                    receipt.getReceivedDate()
                            .format(DateTimeFormatter.ofPattern("yyyy-MM"))
            );
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
                .orElseThrow(() ->
                        new EntityNotFoundException("找不到收款 ID：" + id)
                );

        Order order = receipt.getOrder();

        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "訂單已完成收款，不可刪除收款紀錄"
            );
        }

        receiptRepository.delete(receipt);

        // ⭐ 刪除後重算
        recalcPaymentStatus(order);
        advanceOrderStatusIfNeeded(order);

        log.info("🗑️ 刪除收款：receiptId={}, orderId={}", id, order.getId());
    }

    // =====================================================
    // 查詢
    // =====================================================
    @Override
    @Transactional(readOnly = true)
    public Page<ReceiptResponseDto> findAll(Pageable pageable) {
        return receiptRepository.findAll(pageable)
                .map(mapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public ReceiptResponseDto findById(Long id) {
        return receiptRepository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() ->
                        new EntityNotFoundException("找不到收款 ID：" + id)
                );
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
            Pageable pageable
    ) {

        boolean empty =
                isEmpty(req.getCustomerName()) &&
                        isEmpty(req.getOrderNo()) &&
                        isEmpty(req.getMethod()) &&
                        isEmpty(req.getAccountingPeriod()) &&
                        isEmpty(req.getFromDate()) &&
                        isEmpty(req.getToDate());

        if (empty) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "搜尋條件不可全為空，至少需提供一項搜尋欄位"
            );
        }

        Specification<Receipt> spec = ReceiptSpecifications.build(req);

        try {
            return receiptRepository.findAll(spec, pageable)
                    .map(mapper::toDto);
        } catch (PropertyReferenceException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "無效排序欄位：" + ex.getPropertyName()
            );
        }
    }

    // =====================================================
    // ⭐ 核心：重算 payment_status
    // =====================================================
    private void recalcPaymentStatus(Order order) {

        BigDecimal paidAmount =
                receiptRepository.sumAmountByOrderId(order.getId());

        if (paidAmount == null) {
            paidAmount = BigDecimal.ZERO;
        }

        BigDecimal totalAmount = order.getTotalAmount();

        if (paidAmount.compareTo(BigDecimal.ZERO) == 0) {
            order.setPaymentStatus(PaymentStatus.UNPAID);
        } else if (paidAmount.compareTo(totalAmount) < 0) {
            order.setPaymentStatus(PaymentStatus.PARTIAL);
        } else {
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
