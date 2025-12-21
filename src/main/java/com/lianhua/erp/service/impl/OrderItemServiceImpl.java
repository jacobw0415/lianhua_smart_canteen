package com.lianhua.erp.service.impl;

import com.lianhua.erp.domain.*;
import com.lianhua.erp.dto.orderItem.OrderItemRequestDto;
import com.lianhua.erp.dto.orderItem.OrderItemResponseDto;
import com.lianhua.erp.mapper.OrderItemMapper;
import com.lianhua.erp.repository.*;
import com.lianhua.erp.service.OrderItemService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OrderItemServiceImpl implements OrderItemService {

    private final OrderItemRepository itemRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final OrderItemMapper mapper;
    
    @Override
    public List<OrderItemResponseDto> findByOrderId(Long orderId) {
        return itemRepository.findByOrder_Id(orderId)
                .stream().map(mapper::toResponseDto).toList();
    }

    @Override
    public OrderItemResponseDto create(Long orderId, OrderItemRequestDto dto) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("找不到訂單 ID: " + orderId));
        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new EntityNotFoundException("找不到商品 ID: " + dto.getProductId()));

        // ✅ 從商品表自動帶入單價
        BigDecimal unitPrice = product.getUnitPrice();
        if (unitPrice == null) {
            throw new IllegalStateException("商品「" + product.getName() + "」未設定單價。");
        }

        // ✅ 計算小計（不考慮折扣與稅額）
        BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(dto.getQty()));

        OrderItem item = mapper.toEntity(dto);
        item.setOrder(order);
        item.setProduct(product);
        item.setUnitPrice(unitPrice);  // ✅ 使用商品表的單價
        item.setSubtotal(subtotal);
        item.setAccountingPeriod(order.getAccountingPeriod());

        log.info("新增訂單明細：orderId={}, productId={}, qty={}, unitPrice={}, subtotal={}",
                orderId, dto.getProductId(), dto.getQty(), unitPrice, subtotal);

        itemRepository.save(item);

        // 更新訂單總金額
        BigDecimal newTotal = itemRepository.sumTotalByOrderId(orderId);
        order.setTotalAmount(newTotal);
        orderRepository.save(order);

        return mapper.toResponseDto(item);
    }

    @Override
    public OrderItemResponseDto update(Long orderId, Long itemId, OrderItemRequestDto dto) {
        OrderItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("找不到訂單明細 ID: " + itemId));

        // ✅ 從商品表自動帶入單價（如果商品有變更）
        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new EntityNotFoundException("找不到商品 ID: " + dto.getProductId()));
        
        BigDecimal unitPrice = product.getUnitPrice();
        if (unitPrice == null) {
            throw new IllegalStateException("商品「" + product.getName() + "」未設定單價。");
        }

        // ✅ 計算小計（不考慮折扣與稅額）
        BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(dto.getQty()));

        item.setProduct(product);
        item.setQty(dto.getQty());
        item.setUnitPrice(unitPrice);  // ✅ 使用商品表的單價
        item.setNote(dto.getNote());
        item.setSubtotal(subtotal);
        
        log.info("更新訂單明細：orderId={}, itemId={}, productId={}, qty={}, unitPrice={}, subtotal={}",
                orderId, itemId, dto.getProductId(), dto.getQty(), unitPrice, subtotal);

        itemRepository.save(item);

        // 更新訂單金額
        BigDecimal newTotal = itemRepository.sumTotalByOrderId(orderId);
        Order order = item.getOrder();
        order.setTotalAmount(newTotal);
        orderRepository.save(order);

        return mapper.toResponseDto(item);
    }

    @Override
    public void delete(Long orderId, Long itemId) {
        OrderItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("找不到訂單明細 ID: " + itemId));

        itemRepository.delete(item);

        // 更新訂單總額
        BigDecimal newTotal = itemRepository.sumTotalByOrderId(orderId);
        Order order = item.getOrder();
        order.setTotalAmount(newTotal);
        orderRepository.save(order);
    }
    
    @Override
    public List<OrderItemResponseDto> findAll() {
        return itemRepository.findAll()
                .stream().map(mapper::toResponseDto).toList();
    }
    
    @Override
    public Page<OrderItemResponseDto> findAllPaged(Pageable pageable) {
        return itemRepository.findAll(pageable).map(mapper::toResponseDto);
    }
    
    @Override
    public Page<OrderItemResponseDto> findAllPaged(int page, int size, String keyword) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<OrderItem> resultPage;
        
        if (keyword != null && !keyword.trim().isEmpty()) {
            resultPage = itemRepository.search(keyword, pageable);
        } else {
            resultPage = itemRepository.findAll(pageable);
        }
        
        return resultPage.map(mapper::toResponseDto);
    }
    
}
