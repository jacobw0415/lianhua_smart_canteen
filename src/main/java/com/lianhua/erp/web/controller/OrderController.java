package com.lianhua.erp.web.controller;

import com.lianhua.erp.dto.order.OrderDto;
import com.lianhua.erp.dto.order.OrderResponseDto;
import com.lianhua.erp.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "團體訂單管理", description = "管理團體訂單 CRUD API")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    @Operation(summary = "取得所有訂單")
    public ResponseEntity<List<OrderDto>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @GetMapping("/{id}")
    @Operation(summary = "依 ID 取得訂單")
    public ResponseEntity<OrderDto> getOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    @PostMapping
    @Operation(summary = "新增訂單")
    public ResponseEntity<OrderDto> createOrder(@RequestBody OrderDto dto) {
        return ResponseEntity.ok(orderService.createOrder(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新訂單")
    public ResponseEntity<OrderDto> updateOrder(@PathVariable Long id, @RequestBody OrderDto dto) {
        return ResponseEntity.ok(orderService.updateOrder(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "刪除訂單")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/details")
    @Operation(summary = "取得訂單（含明細與客戶）")
    public ResponseEntity<OrderResponseDto> getOrderWithDetails(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderWithDetails(id));
    }

}
