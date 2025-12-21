package com.lianhua.erp.web.controller;

import com.lianhua.erp.dto.apiResponse.ApiResponseDto;
import com.lianhua.erp.dto.error.BadRequestResponse;
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
import org.springdoc.core.annotations.ParameterObject;
import org.springdoc.core.converters.models.PageableAsQueryParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "訂單明細管理", description = "Order Items Management API")
public class OrderItemController {
    
    private final OrderItemService service;
    
    /* ============================================================
     * 📌 分頁取得所有訂單明細清單（不分訂單）
     * ============================================================ */
    @Operation(
            summary = "分頁取得所有訂單明細清單",
            description = """
                    支援 page / size / sort，自動與 React-Admin 分頁整合。
                    例如：/api/order-items?page=0&size=10&sort=id,desc
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功取得訂單明細列表",
                    content = @Content(schema = @Schema(implementation = OrderItemResponseDto.class))),
            @ApiResponse(responseCode = "500", description = "伺服器錯誤")
    })
    @PageableAsQueryParam
    @GetMapping("/api/order_items")
    public ResponseEntity<ApiResponseDto<Page<OrderItemResponseDto>>> getAllOrderItems(
            @ParameterObject Pageable pageable
    ) {
        Page<OrderItemResponseDto> page = service.findAllPaged(pageable);
        return ResponseEntity.ok(ApiResponseDto.ok(page));
    }
    
    /* ============================================================
     * 📌 取得指定訂單的所有明細
     * ============================================================ */
    @Operation(
            summary = "取得指定訂單的所有明細",
            description = "取得指定訂單 ID 的所有明細項目。路徑：/api/orders/{orderId}/items"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功取得明細清單",
                    content = @Content(schema = @Schema(implementation = OrderItemResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "找不到訂單",
                    content = @Content(schema = @Schema(implementation = NotFoundResponse.class)))
    })
    @GetMapping("/api/orders/{orderId}/items")
    public ResponseEntity<ApiResponseDto<List<OrderItemResponseDto>>> findAll(@PathVariable Long orderId) {
        var items = service.findByOrderId(orderId);
        return ResponseEntity.ok(ApiResponseDto.ok(items));
    }
    
    /* ============================================================
     * 📌 新增訂單明細
     * ============================================================ */
    @Operation(
            summary = "新增訂單明細",
            description = "為指定訂單新增一筆明細項目。路徑：/api/orders/{orderId}/items"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "成功新增明細",
                    content = @Content(schema = @Schema(implementation = OrderItemResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "請求參數錯誤",
                    content = @Content(schema = @Schema(implementation = BadRequestResponse.class))),
            @ApiResponse(responseCode = "404", description = "找不到訂單或商品",
                    content = @Content(schema = @Schema(implementation = NotFoundResponse.class)))
    })
    @PostMapping("/api/orders/{orderId}/items")
    public ResponseEntity<ApiResponseDto<OrderItemResponseDto>> create(
            @PathVariable Long orderId,
            @Valid @RequestBody OrderItemRequestDto dto) {
        
        var created = service.create(orderId, dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.created(created));
    }
    
    /* ============================================================
     * 📌 更新指定的訂單明細
     * ============================================================ */
    @Operation(
            summary = "更新指定的訂單明細",
            description = "更新指定訂單的指定明細項目。路徑：/api/orders/{orderId}/items/{itemId}"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "更新成功",
                    content = @Content(schema = @Schema(implementation = OrderItemResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "請求參數錯誤",
                    content = @Content(schema = @Schema(implementation = BadRequestResponse.class))),
            @ApiResponse(responseCode = "404", description = "找不到訂單或明細",
                    content = @Content(schema = @Schema(implementation = NotFoundResponse.class)))
    })
    @PutMapping("/api/orders/{orderId}/items/{itemId}")
    public ResponseEntity<ApiResponseDto<OrderItemResponseDto>> update(
            @PathVariable Long orderId,
            @PathVariable Long itemId,
            @Valid @RequestBody OrderItemRequestDto dto) {
        
        var updated = service.update(orderId, itemId, dto);
        return ResponseEntity.ok(ApiResponseDto.ok(updated));
    }
    
    /* ============================================================
     * 📌 刪除指定的訂單明細
     * ============================================================ */
    @Operation(
            summary = "刪除指定的訂單明細",
            description = "刪除指定訂單的指定明細項目。路徑：/api/orders/{orderId}/items/{itemId}"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "刪除成功"),
            @ApiResponse(responseCode = "404", description = "找不到訂單或明細",
                    content = @Content(schema = @Schema(implementation = NotFoundResponse.class)))
    })
    @DeleteMapping("/api/orders/{orderId}/items/{itemId}")
    public ResponseEntity<ApiResponseDto<Void>> delete(
            @PathVariable Long orderId,
            @PathVariable Long itemId) {
        
        service.delete(orderId, itemId);
        return ResponseEntity.ok(ApiResponseDto.ok(null));
    }
}
