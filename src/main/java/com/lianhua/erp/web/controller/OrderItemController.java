package com.lianhua.erp.web.controller;

import com.lianhua.erp.dto.apiResponse.ApiResponseDto;
import com.lianhua.erp.dto.error.NotFoundResponse;
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
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders/{orderId}/items")
@RequiredArgsConstructor
@Tag(name = "訂單明細管理", description = "Order Items Management API")
public class OrderItemController {
    
    private final OrderItemService service;
    
    @Operation(summary = "查詢所有訂單明細（可分頁與搜尋）",
            description = "可透過 page、size、keyword 查詢訂單明細。例如：/api/order-items?page=0&size=20&keyword=螺絲")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功取得訂單明細列表",
                    content = @Content(schema = @Schema(implementation = OrderItemResponseDto.class)))
    })
    @GetMapping("/order-items")
    public ResponseEntity<ApiResponseDto<Page<OrderItemResponseDto>>> findAllItems(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        
        Page<OrderItemResponseDto> result = service.findAllPaged(page, size, keyword);
        return ResponseEntity.ok(ApiResponseDto.ok(result));
    }
    
    @Operation(summary = "取得指定訂單的所有明細")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功取得明細清單",
                    content = @Content(schema = @Schema(implementation = OrderItemResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "找不到訂單",
                    content = @Content(schema = @Schema(implementation = NotFoundResponse.class)))
    })
    @GetMapping
    public ResponseEntity<ApiResponseDto<List<OrderItemResponseDto>>> findAll(@PathVariable Long orderId) {
        var items = service.findByOrderId(orderId);
        return ResponseEntity.ok(ApiResponseDto.ok(items));
    }
    
    @Operation(summary = "更新指定的訂單明細")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "更新成功",
                    content = @Content(schema = @Schema(implementation = OrderItemResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "請求參數錯誤",
                    content = @Content(schema = @Schema(implementation = com.lianhua.erp.dto.error.BadRequestResponse.class))),
            @ApiResponse(responseCode = "404", description = "找不到訂單或明細",
                    content = @Content(schema = @Schema(implementation = NotFoundResponse.class)))
    })
    @PutMapping("/{itemId}")
    public ResponseEntity<ApiResponseDto<OrderItemResponseDto>> update(
            @PathVariable Long orderId,
            @PathVariable Long itemId,
            @Valid @RequestBody OrderItemRequestDto dto) {
        
        var updated = service.update(orderId, itemId, dto);
        return ResponseEntity.ok(ApiResponseDto.ok(updated));
    }
    
    @Operation(summary = "刪除指定的訂單明細")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "刪除成功"),
            @ApiResponse(responseCode = "404", description = "找不到訂單或明細",
                    content = @Content(schema = @Schema(implementation = NotFoundResponse.class)))
    })
    @DeleteMapping("/{itemId}")
    public ResponseEntity<ApiResponseDto<Void>> delete(
            @PathVariable Long orderId,
            @PathVariable Long itemId) {
        
        service.delete(orderId, itemId);
        return ResponseEntity.ok(ApiResponseDto.ok(null));
    }
}
