package com.lianhua.erp.service.impl;

import com.lianhua.erp.domain.*;
import com.lianhua.erp.dto.export.ExportPayload;
import com.lianhua.erp.dto.order.*;
import com.lianhua.erp.dto.orderItem.OrderItemRequestDto;
import com.lianhua.erp.export.ExportFilenameUtils;
import com.lianhua.erp.export.ExportFormat;
import com.lianhua.erp.export.ExportScope;
import com.lianhua.erp.export.TabularExporter;
import com.lianhua.erp.mapper.*;
import com.lianhua.erp.repository.*;
import com.lianhua.erp.service.OrderService;
import com.lianhua.erp.service.impl.spec.OrderSpecifications;
import com.lianhua.erp.numbering.OrderNoGenerator;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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
public class OrderServiceImpl implements OrderService {

    private static final String[] ORDER_EXPORT_HEADERS = new String[]{
            "訂單編號", "客戶", "訂單狀態", "收款狀態", "訂單金額", "訂單日期", "交貨日期"
    };

    @Value("${app.export.max-rows:50000}")
    private int maxExportRows;

    private final OrderRepository orderRepository;
    private final OrderCustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final OrderItemRepository itemRepository;
    private final OrderMapper orderMapper;
    private final OrderItemMapper itemMapper;
    private final OrderNoGenerator orderNoGenerator;
    private final ReceiptRepository receiptRepository;

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
    // 匯出（與 search 相同篩選）
    // ================================
    @Override
    @Transactional(readOnly = true)
    public ExportPayload exportOrders(
            OrderSearchRequest searchRequest,
            Pageable pageable,
            ExportFormat format,
            ExportScope scope) {

        OrderSearchRequest req = searchRequest == null ? new OrderSearchRequest() : searchRequest;
        Pageable p = scope == ExportScope.ALL ? Pageable.unpaged() : pageable;

        if (scope == ExportScope.ALL) {
            long total = orderRepository.count(OrderSpecifications.bySearchRequest(req));
            if (total > maxExportRows) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "匯出筆數超過上限 (" + maxExportRows + ")，請縮小篩選條件");
            }
        }

        Page<OrderResponseDto> page = search(req, p);
        List<String[]> rows = page.getContent().stream()
                .map(OrderServiceImpl::toOrderExportRow)
                .toList();

        byte[] data = switch (format) {
            case XLSX -> TabularExporter.toXlsx("訂單", ORDER_EXPORT_HEADERS, rows);
            case CSV -> TabularExporter.toCsvUtf8Bom(ORDER_EXPORT_HEADERS, rows);
        };

        String filename = ExportFilenameUtils.build("orders", format);
        return new ExportPayload(data, filename, format.mediaType());
    }

    private static String[] toOrderExportRow(OrderResponseDto o) {
        return new String[]{
                nz(o.getOrderNo()),
                nz(o.getCustomerName()),
                o.getOrderStatus() == null ? "" : o.getOrderStatus().name(),
                o.getPaymentStatus() == null ? "" : o.getPaymentStatus().name(),
                o.getTotalAmount() == null ? "" : o.getTotalAmount().toPlainString(),
                o.getOrderDate() == null ? "" : o.getOrderDate().toString(),
                o.getDeliveryDate() == null ? "" : o.getDeliveryDate().toString()
        };
    }

    private static String nz(String s) {
        return s == null ? "" : s;
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

        // 🔒 驗證日期：交貨日期不能為空且必須晚於或等於訂單日期
        if (dto.getDeliveryDate() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "交貨日期不可為空"
            );
        }
        if (dto.getOrderDate() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "訂單日期不可為空"
            );
        }
        if (dto.getDeliveryDate().isBefore(dto.getOrderDate())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "交貨日期不可早於訂單日期"
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

        // ⚠️ 取消訂單前必須未收款（檢查是否有任何收款記錄，包括已作廢的）
        if (dto.getOrderStatus() == OrderStatus.CANCELLED) {
                // 檢查是否有任何收款記錄（包括已作廢的）
                boolean hasAnyReceipt = receiptRepository.hasAnyReceiptByOrderId(order.getId());

                if (hasAnyReceipt) {
                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "已有收款紀錄的訂單不可取消，請先處理退款"
                        );
                }
        }

        // 🔒 驗證日期：交貨日期不能為空且必須晚於或等於訂單日期
        if (dto.getDeliveryDate() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "交貨日期不可為空"
            );
        }
        // 使用訂單原有的 orderDate 進行驗證（因為 update 不允許修改 orderDate）
        LocalDate orderDateToCheck = order.getOrderDate();
        if (dto.getDeliveryDate().isBefore(orderDateToCheck)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "交貨日期不可早於訂單日期"
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

        // ⚠️ 檢查是否有任何收款記錄（包括已作廢的），如果有則不可刪除
        boolean hasAnyReceipt = receiptRepository.hasAnyReceiptByOrderId(order.getId());

        if (hasAnyReceipt) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "已有收款紀錄的訂單不可刪除"
            );
        }

        orderRepository.delete(order);
    }

    // ================================
    // 🚀 新增：作廢訂單狀態同步
    // ================================
    @Transactional
    public void voidOrder(String orderNo, String voidReason) {
        log.info("🔄 開始同步訂單作廢狀態：orderNo={}, reason={}", orderNo, voidReason);

        // 1. 查找訂單 (建議在 Repository 新增 findByOrderNo)
        Order order = orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new EntityNotFoundException("找不到訂單編號：" + orderNo));

        // 2. 更新作廢欄位 (對接您在 Entity 新增的欄位)
        order.setRecordStatus("VOIDED");
        order.setVoidedAt(java.time.LocalDateTime.now());
        order.setVoidReason(voidReason);

        // 3. 業務邏輯：如果訂單作廢，通常狀態也會轉為 CANCELLED 或保持原樣但鎖定
        // order.setOrderStatus(OrderStatus.CANCELLED);

        orderRepository.save(order);
        log.info("✅ 訂單作廢狀態同步完成：orderNo={}", orderNo);
    }
}
