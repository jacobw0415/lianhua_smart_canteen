package com.lianhua.erp.service.impl;

import com.lianhua.erp.domin.*;
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
    private Pageable pageable;
    
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

        OrderItem item = mapper.toEntity(dto);
        item.setOrder(order);
        item.setProduct(product);

        // 自動計算小計與會計期
        item.setSubtotal(dto.getUnitPrice().multiply(BigDecimal.valueOf(dto.getQty())));
        item.setAccountingPeriod(order.getAccountingPeriod());

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

        item.setQty(dto.getQty());
        item.setUnitPrice(dto.getUnitPrice());
        item.setDiscount(dto.getDiscount());
        item.setTax(dto.getTax());
        item.setNote(dto.getNote());
        item.setSubtotal(dto.getUnitPrice().multiply(BigDecimal.valueOf(dto.getQty())));
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
