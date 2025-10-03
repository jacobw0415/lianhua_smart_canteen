package com.lianhua.erp.mapper;

import com.lianhua.erp.dto.*;
import com.lianhua.erp.domin.*;
import com.lianhua.erp.repository.OrderCustomerRepository;
import com.lianhua.erp.repository.ProductRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class OrderMapper {

    private final OrderCustomerRepository customerRepository;
    private final ProductRepository productRepository;

    public OrderMapper(OrderCustomerRepository customerRepository, ProductRepository productRepository) {
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
    }

    // DTO → Entity
    public Order toEntity(OrderDto dto) {
        if (dto == null) return null;

        Order order = new Order();
        order.setId(dto.getId());
        order.setOrderDate(dto.getOrderDate() != null ? LocalDate.parse(dto.getOrderDate()) : LocalDate.now());
        order.setDeliveryDate(dto.getDeliveryDate() != null ? LocalDate.parse(dto.getDeliveryDate()) : null);
        order.setStatus(Order.Status.valueOf(dto.getStatus()));
        order.setNote(dto.getNote());
        order.setTotalAmount(dto.getTotalAmount());

        if (dto.getCustomerId() != null) {
            OrderCustomer customer = customerRepository.findById(dto.getCustomerId())
                    .orElseThrow(() -> new RuntimeException("Customer not found: " + dto.getCustomerId()));
            order.setCustomer(customer);
        }

        if (dto.getItems() != null) {
            List<OrderItem> items = dto.getItems().stream().map(itemDto -> {
                OrderItem item = new OrderItem();
                item.setId(itemDto.getId());
                item.setOrder(order);

                if (itemDto.getProductId() != null) {
                    Product product = productRepository.findById(itemDto.getProductId())
                            .orElseThrow(() -> new RuntimeException("Product not found: " + itemDto.getProductId()));
                    item.setProduct(product);
                }

                item.setQty(itemDto.getQty());
                item.setUnitPrice(itemDto.getUnitPrice());
                item.setSubtotal(itemDto.getSubtotal());
                item.setDiscount(itemDto.getDiscount());
                item.setTax(itemDto.getTax());
                item.setNote(itemDto.getNote());

                return item;
            }).collect(Collectors.toList());

            order.setItems(items);
        }

        return order;
    }

    // Entity → DTO
    public OrderDto toDto(Order order) {
        if (order == null) return null;

        return OrderDto.builder()
                .id(order.getId())
                .orderDate(order.getOrderDate() != null ? order.getOrderDate().toString() : null)
                .deliveryDate(order.getDeliveryDate() != null ? order.getDeliveryDate().toString() : null)
                .status(String.valueOf(order.getStatus()))
                .totalAmount(order.getTotalAmount())
                .note(order.getNote())
                .customerId(order.getCustomer() != null ? order.getCustomer().getId() : null)
                .items(order.getItems() != null ? order.getItems().stream().map(this::toItemDto).collect(Collectors.toList()) : null)
                .build();
    }

    private OrderItemDto toItemDto(OrderItem item) {
        return OrderItemDto.builder()
                .id(item.getId())
                .orderId(item.getOrder() != null ? item.getOrder().getId() : null)
                .productId(item.getProduct() != null ? item.getProduct().getId() : null)
                .qty(item.getQty())
                .unitPrice(item.getUnitPrice())
                .subtotal(item.getSubtotal())
                .discount(item.getDiscount())
                .tax(item.getTax())
                .note(item.getNote())
                .build();
    }

    public OrderResponseDto toResponseDto(Order order) {
        if (order == null) return null;

        return OrderResponseDto.builder()
                .id(order.getId())
                .orderDate(order.getOrderDate() != null ? order.getOrderDate().toString() : null)
                .deliveryDate(order.getDeliveryDate() != null ? order.getDeliveryDate().toString() : null)
                .status(String.valueOf(order.getStatus()))
                .totalAmount(order.getTotalAmount())
                .note(order.getNote())
                .customer(order.getCustomer() != null ? toCustomerDto(order.getCustomer()) : null)
                .items(order.getItems() != null
                        ? order.getItems().stream().map(this::toItemDetailDto).collect(Collectors.toList())
                        : null)
                .build();
    }

    private OrderCustomerDto toCustomerDto(OrderCustomer customer) {
        return OrderCustomerDto.builder()
                .id(customer.getId())
                .name(customer.getName())
                .contactPerson(customer.getContactPerson())
                .phone(customer.getPhone())
                .address(customer.getAddress())
                .billingCycle(String.valueOf(customer.getBillingCycle()))
                .note(customer.getNote())
                .build();
    }

    private OrderItemDetailDto toItemDetailDto(OrderItem item) {
        return OrderItemDetailDto.builder()
                .id(item.getId())
                .qty(item.getQty())
                .unitPrice(item.getUnitPrice())
                .subtotal(item.getSubtotal())
                .discount(item.getDiscount())
                .tax(item.getTax())
                .note(item.getNote())
                .product(item.getProduct() != null ? ProductDto.builder()
                        .id(item.getProduct().getId())
                        .name(item.getProduct().getName())
                        .category(String.valueOf(item.getProduct().getCategory()))
                        .unitPrice(item.getProduct().getUnitPrice())
                        .active(item.getProduct().getActive())
                        .build() : null)
                .build();
    }

}

