package com.lianhua.erp.web.controller;

import com.lianhua.erp.dto.order.OrderCustomerDto;
import com.lianhua.erp.dto.order.OrderResponseDto;
import com.lianhua.erp.service.OrderCustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/order-customers")
@Tag(name = "訂單客戶管理", description = "管理訂單客戶 (學校/公司/企業) 的 API")
public class OrderCustomerController {

    private final OrderCustomerService customerService;

    public OrderCustomerController(OrderCustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    @Operation(summary = "取得所有客戶")
    public ResponseEntity<List<OrderCustomerDto>> getAllCustomers() {
        return ResponseEntity.ok(customerService.getAllCustomers());
    }

    @GetMapping("/{id}")
    @Operation(summary = "依 ID 取得客戶")
    public ResponseEntity<OrderCustomerDto> getCustomerById(@PathVariable Long id) {
        return ResponseEntity.ok(customerService.getCustomerById(id));
    }

    @PostMapping
    @Operation(summary = "新增客戶")
    public ResponseEntity<OrderCustomerDto> createCustomer(@RequestBody OrderCustomerDto dto) {
        return ResponseEntity.ok(customerService.createCustomer(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新客戶")
    public ResponseEntity<OrderCustomerDto> updateCustomer(@PathVariable Long id, @RequestBody OrderCustomerDto dto) {
        return ResponseEntity.ok(customerService.updateCustomer(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "刪除客戶")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Long id) {
        customerService.deleteCustomer(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/orders")
    @Operation(summary = "查詢某客戶的所有訂單（含明細與產品資訊）")
    public ResponseEntity<List<OrderResponseDto>> getOrdersByCustomer(@PathVariable Long id) {
        return ResponseEntity.ok(customerService.getOrdersByCustomerId(id));
    }
}
