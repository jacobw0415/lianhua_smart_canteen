package com.lianhua.erp.service.impl;

import com.lianhua.erp.domin.*;
import com.lianhua.erp.dto.order.*;
import com.lianhua.erp.dto.orderItem.OrderItemRequestDto;
import com.lianhua.erp.mapper.*;
import com.lianhua.erp.repository.*;
import com.lianhua.erp.service.OrderService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    
    // ================================
    // 查詢所有訂單
    // ================================
    @Override
    public List<OrderResponseDto> findAll() {
        return orderRepository.findAll().stream()
                .map(o -> orderMapper.toResponseDto(o, itemMapper))
                .toList();
    }
    
    // ================================
    // 查詢單筆訂單
    // ================================
    @Override
    public OrderResponseDto findById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("找不到訂單 ID: " + id));
        return orderMapper.toResponseDto(order, itemMapper);
    }
    
    // ================================
    // 建立訂單（可同時含明細）
    // ================================
    @Override
    public OrderResponseDto create(OrderRequestDto dto) {
        // 驗證客戶
        OrderCustomer customer = customerRepository.findById(dto.getCustomerId())
                .orElseThrow(() -> new EntityNotFoundException("找不到客戶 ID: " + dto.getCustomerId()));
        
        // 檢查唯一約束 (customer_id + order_date)
        if (orderRepository.existsByCustomer_IdAndOrderDate(dto.getCustomerId(), dto.getOrderDate())) {
            throw new DataIntegrityViolationException("該客戶於該日期已有訂單，請勿重複建立。");
        }
        
        // 建立主表
        Order order = orderMapper.toEntity(dto);
        order.setCustomer(customer);
        order.setStatus(Order.Status.PENDING);
        order.setAccountingPeriod(dto.getOrderDate().format(DateTimeFormatter.ofPattern("yyyy-MM")));
        order.setTotalAmount(BigDecimal.ZERO);
        
        // 先儲存主表以取得 order.id
        orderRepository.save(order);
        
        // 若包含明細，一併處理
        if (dto.getItems() != null && !dto.getItems().isEmpty()) {
            BigDecimal total = BigDecimal.ZERO;
            
            for (OrderItemRequestDto itemDto : dto.getItems()) {
                // 1️⃣ 驗證商品存在
                Product product = productRepository.findById(itemDto.getProductId())
                        .orElseThrow(() -> new EntityNotFoundException("找不到商品 ID: " + itemDto.getProductId()));
                
                // 2️⃣ 從商品表自動帶入單價
                BigDecimal unitPrice = product.getUnitPrice();
                if (unitPrice == null) {
                    throw new IllegalStateException("商品「" + product.getName() + "」未設定單價。");
                }
                
                // 3️⃣ 預設折扣與稅額
                BigDecimal discount = itemDto.getDiscount() != null ? itemDto.getDiscount() : BigDecimal.ZERO;
                BigDecimal tax = itemDto.getTax() != null ? itemDto.getTax() : BigDecimal.ZERO;
                
                // 4️⃣ 計算小計
                BigDecimal qty = BigDecimal.valueOf(itemDto.getQty());
                BigDecimal subtotal = unitPrice.multiply(qty).subtract(discount).add(tax);
                
                // 5️⃣ 建立明細
                OrderItem item = new OrderItem();
                item.setOrder(order);
                item.setProduct(product);
                item.setQty(itemDto.getQty());
                item.setUnitPrice(unitPrice);
                item.setDiscount(discount);
                item.setTax(tax);
                item.setSubtotal(subtotal);
                item.setAccountingPeriod(order.getAccountingPeriod());
                item.setNote(itemDto.getNote());
                
                itemRepository.save(item);
                
                order.getItems().add(item);
                
                // 6️⃣ 累計總金額
                total = total.add(subtotal);
            }
            
            // 7️⃣ 更新訂單總金額
            order.setTotalAmount(total);
            orderRepository.save(order);
        }
        
        return orderMapper.toResponseDto(order, itemMapper);
    }
    
    // ================================
    // 更新訂單（日期、交貨日、備註、狀態）
    // ================================
    @Override
    public OrderResponseDto update(Long id, OrderRequestDto dto) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("找不到訂單 ID: " + id));
        
        order.setOrderDate(dto.getOrderDate());
        order.setDeliveryDate(dto.getDeliveryDate());
        order.setNote(dto.getNote());
        orderRepository.save(order);
        
        return orderMapper.toResponseDto(order, itemMapper);
    }
    
    // ================================
    // 刪除訂單
    // ================================
    @Override
    public void delete(Long id) {
        if (!orderRepository.existsById(id)) {
            throw new EntityNotFoundException("找不到訂單 ID: " + id);
        }
        orderRepository.deleteById(id);
    }
}
