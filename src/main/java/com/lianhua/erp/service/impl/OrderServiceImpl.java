package com.lianhua.erp.service.impl;

import com.lianhua.erp.domain.*;
import com.lianhua.erp.dto.order.*;
import com.lianhua.erp.dto.orderItem.OrderItemRequestDto;
import com.lianhua.erp.mapper.*;
import com.lianhua.erp.repository.*;
import com.lianhua.erp.service.OrderService;
import com.lianhua.erp.service.impl.spec.OrderSpecifications;
import com.lianhua.erp.numbering.OrderNoGenerator;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderCustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final OrderItemRepository itemRepository;
    private final OrderMapper orderMapper;
    private final OrderItemMapper itemMapper;
    private final OrderNoGenerator orderNoGenerator;

    // ================================
    // 查詢（分頁）
    // ================================
    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponseDto> page(Pageable pageable) {
        return orderRepository.findAll(pageable)
                .map(order -> orderMapper.toResponseDto(order, itemMapper));
    }

    // ================================
    // 搜尋 + 分頁
    // ================================
    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponseDto> search(
            OrderSearchRequest searchRequest,
            Pageable pageable) {

        Specification<Order> spec =
                OrderSpecifications.bySearchRequest(searchRequest);

        return orderRepository.findAll(spec, pageable)
                .map(order -> orderMapper.toResponseDto(order, itemMapper));
    }

    // ================================
    // 查詢單筆
    // ================================
    @Override
    @Transactional(readOnly = true)
    public OrderResponseDto findById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("找不到訂單 ID: " + id));

        return orderMapper.toResponseDto(order, itemMapper);
    }

    // ================================
    // 建立訂單
    // ================================
    @Override
    public OrderResponseDto create(OrderRequestDto dto) {

        // 🔒 強化 1：訂單必須包含至少一項商品
        List<OrderItemRequestDto> items = dto.getItems();
        if (items == null || items.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "訂單至少需包含一項商品"
            );
        }

        // 1️⃣ 驗證客戶
        OrderCustomer customer = customerRepository.findById(dto.getCustomerId())
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "找不到客戶 ID: " + dto.getCustomerId()));

        // 2️⃣ 驗證建單狀態
        if (dto.getOrderStatus() != OrderStatus.PENDING &&
                dto.getOrderStatus() != OrderStatus.CONFIRMED) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "建單時僅允許 PENDING 或 CONFIRMED 狀態"
            );
        }

        // 3️⃣ 防止重複建單：
        // 同一客戶 + 同一天 + 同商品 不允許
        for (OrderItemRequestDto itemDto : items) {

            boolean hasDuplicate =
                    itemRepository
                            .existsByOrder_Customer_IdAndOrder_OrderDateAndProduct_Id(
                                    dto.getCustomerId(),
                                    dto.getOrderDate(),
                                    itemDto.getProductId()
                            );

            if (hasDuplicate) {
                Product product =
                        productRepository.findById(itemDto.getProductId())
                                .orElse(null);

                String productName =
                        product != null
                                ? product.getName()
                                : "商品 ID: " + itemDto.getProductId();

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        String.format(
                                "該客戶於 %s 已下訂商品「%s」，請勿重複建立相同商品的訂單",
                                dto.getOrderDate(),
                                productName
                        )
                );
            }
        }

        // 4️⃣ 建立訂單主檔
        Order order = orderMapper.toEntity(dto);
        order.setCustomer(customer);
        order.setOrderStatus(dto.getOrderStatus());
        order.setPaymentStatus(PaymentStatus.UNPAID);
        order.setAccountingPeriod(
                dto.getOrderDate()
                        .format(DateTimeFormatter.ofPattern("yyyy-MM")));
        order.setTotalAmount(BigDecimal.ZERO);

        // 5️⃣ 產生訂單編號
        String orderNo =
                orderNoGenerator.generate(dto.getOrderDate());
        order.setOrderNo(orderNo);

        orderRepository.save(order);

        // 6️⃣ 建立訂單明細並計算總金額
        BigDecimal total = BigDecimal.ZERO;

        for (OrderItemRequestDto itemDto : items) {

            Product product = productRepository.findById(itemDto.getProductId())
                    .orElseThrow(() ->
                            new EntityNotFoundException(
                                    "找不到商品 ID: " + itemDto.getProductId()));

            BigDecimal unitPrice = product.getUnitPrice();
            BigDecimal subtotal =
                    unitPrice.multiply(
                            BigDecimal.valueOf(itemDto.getQty()));

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProduct(product);
            item.setQty(itemDto.getQty());
            item.setUnitPrice(unitPrice);
            item.setSubtotal(subtotal);
            item.setAccountingPeriod(order.getAccountingPeriod());
            item.setNote(itemDto.getNote());

            itemRepository.save(item);
            order.getItems().add(item);

            total = total.add(subtotal);
        }

        // 7️⃣ 更新總金額
        order.setTotalAmount(total);
        orderRepository.save(order);

        log.info("✅ 建立訂單成功：orderNo={}, total={}", orderNo, total);

        return orderMapper.toResponseDto(order, itemMapper);
    }

    // ================================
    // 更新訂單（僅允許流程欄位）
    // ================================
    @Override
    public OrderResponseDto update(Long id, OrderRequestDto dto) {

        Order order = orderRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "找不到訂單 ID: " + id));

        // ❌ 已交付或取消不可修改
        if (order.getOrderStatus() == OrderStatus.DELIVERED ||
                order.getOrderStatus() == OrderStatus.CANCELLED) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "已交付或已取消的訂單不可修改"
            );
        }

        // ⚠️ 取消訂單前必須未收款
        if (dto.getOrderStatus() == OrderStatus.CANCELLED &&
                order.getPaymentStatus() != PaymentStatus.UNPAID) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "已有收款紀錄的訂單不可取消，請先處理退款"
            );
        }

        /*
         * 🔒 強化 2：
         * update 僅允許修改「流程性欄位」
         * 不允許修改：
         * - orderNo
         * - customer
         * - orderDate
         * - totalAmount
         * - items
         */
        order.setOrderStatus(dto.getOrderStatus());
        order.setNote(dto.getNote());
        order.setDeliveryDate(dto.getDeliveryDate());

        orderRepository.save(order);

        return orderMapper.toResponseDto(order, itemMapper);
    }

    // ================================
    // 刪除訂單（嚴格限制）
    // ================================
    @Override
    public void delete(Long id) {

        Order order = orderRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "找不到訂單 ID: " + id));

        if (order.getPaymentStatus() != PaymentStatus.UNPAID) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "已有收款紀錄的訂單不可刪除"
            );
        }

        orderRepository.delete(order);
    }
}
