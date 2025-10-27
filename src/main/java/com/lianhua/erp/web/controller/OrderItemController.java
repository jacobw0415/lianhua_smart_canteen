package com.lianhua.erp.web.controller;

import com.lianhua.erp.dto.apiResponse.ApiResponseDto;
import com.lianhua.erp.dto.error.*;
import com.lianhua.erp.dto.order.*;
import com.lianhua.erp.dto.orderItem.OrderItemRequestDto;
import com.lianhua.erp.dto.orderItem.OrderItemResponseDto;
import com.lianhua.erp.service.OrderItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/orders/{orderId}/items")
@RequiredArgsConstructor
@Tag(name = "訂單明細管理", description = "Order Items Management API")
public class OrderItemController {

    private final OrderItemService service;

    @Operation(summary = "取得訂單的所有明細")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功取得明細清單",
                    content = @Content(schema = @Schema(implementation = OrderItemResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "找不到訂單",
                    content = @Content(schema = @Schema(implementation = NotFoundResponse.class)))
    })
    @GetMapping
    public ResponseEntity<ApiResponseDto<List<OrderItemResponseDto>>> findAll(@PathVariable Long orderId) {
        return ResponseEntity.ok(ApiResponseDto.ok(service.findByOrderId(orderId)));
    }

    @Operation(summary = "新增訂單明細")
    @PostMapping
    public ResponseEntity<ApiResponseDto<OrderItemResponseDto>> create(
            @PathVariable Long orderId, @Valid @RequestBody OrderItemRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponseDto.ok(service.create(orderId, dto)));
    }

    @Operation(summary = "更新訂單明細")
    @PutMapping("/{itemId}")
    public ResponseEntity<ApiResponseDto<OrderItemResponseDto>> update(
            @PathVariable Long orderId, @PathVariable Long itemId, @Valid @RequestBody OrderItemRequestDto dto) {
        return ResponseEntity.ok(ApiResponseDto.ok(service.update(orderId, itemId, dto)));
    }

    @Operation(summary = "刪除訂單明細")
    @DeleteMapping("/{itemId}")
    public ResponseEntity<Void> delete(@PathVariable Long orderId, @PathVariable Long itemId) {
        service.delete(orderId, itemId);
        return ResponseEntity.noContent().build();
    }
}
