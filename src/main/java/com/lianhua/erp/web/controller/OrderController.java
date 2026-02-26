package com.lianhua.erp.web.controller;

import com.lianhua.erp.dto.apiResponse.ApiResponseDto;
import com.lianhua.erp.dto.error.*;
import com.lianhua.erp.dto.order.*;
import com.lianhua.erp.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springdoc.core.converters.models.PageableAsQueryParam;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "訂單管理", description = "Orders Management API")
public class OrderController {

    private final OrderService service;

    // ============================================================
    // ★ 純取得訂單（分頁，但不做模糊搜尋）
    // ============================================================
    @Operation(
            summary = "取得訂單列表（純分頁）",
            description = "支援 page / size / sort，自動與 React-Admin 分頁整合"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功取得訂單分頁資料"),
            @ApiResponse(responseCode = "500", description = "伺服器錯誤")
    })
    @PageableAsQueryParam
    @GetMapping
    @PreAuthorize("hasAuthority('order:view')")
    public ResponseEntity<ApiResponseDto<Page<OrderResponseDto>>> list(
            @ParameterObject Pageable pageable
    ) {
        Page<OrderResponseDto> page = service.page(pageable);
        return ResponseEntity.ok(ApiResponseDto.ok(page));
    }

    // ============================================================
    // ★ 模糊搜尋訂單（分頁 + 搜尋條件）
    // ============================================================
    @Operation(
            summary = "搜尋訂單（模糊搜尋 + 分頁）",
            description = """
                    支援以下搜尋條件 (皆為模糊或區間)：
                    - customerName
                    - note
                    - orderDateFrom / orderDateTo
                    - deliveryDateFrom / deliveryDateTo
                    - status
                    - accountingPeriod
                    - totalAmountMin / totalAmountMax
                    與 React-Admin List / Filter 完整整合。
                    範例：
                    /api/orders/search?page=0&size=10&customerName=聯華
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "搜尋成功"),
            @ApiResponse(responseCode = "400", description = "搜尋參數錯誤",
                    content = @Content(schema = @Schema(implementation = BadRequestResponse.class))),
            @ApiResponse(responseCode = "500", description = "伺服器錯誤")
    })
    @PageableAsQueryParam
    @GetMapping("/search")
    @PreAuthorize("hasAuthority('order:view')")
    public ResponseEntity<ApiResponseDto<Page<OrderResponseDto>>> search(
            @ParameterObject OrderSearchRequest searchRequest,
            @ParameterObject Pageable pageable
    ) {
        Page<OrderResponseDto> page = service.search(searchRequest, pageable);
        return ResponseEntity.ok(ApiResponseDto.ok(page));
    }

    // ============================================================
    // 查詢單筆訂單
    // ============================================================
    @Operation(summary = "查詢單筆訂單紀錄")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功取得訂單資料",
                    content = @Content(schema = @Schema(implementation = OrderResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "找不到訂單紀錄",
                    content = @Content(schema = @Schema(implementation = NotFoundResponse.class)))
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('order:view')")
    public ResponseEntity<ApiResponseDto<OrderResponseDto>> getById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(ApiResponseDto.ok(service.findById(id)));
    }

    // ============================================================
    // 新增訂單
    // ============================================================
    @Operation(summary = "新增訂單")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "成功新增訂單",
                    content = @Content(schema = @Schema(implementation = OrderResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "參數錯誤",
                    content = @Content(schema = @Schema(implementation = BadRequestResponse.class))),
            @ApiResponse(responseCode = "409", description = "資料衝突",
                    content = @Content(schema = @Schema(implementation = ConflictResponse.class)))
    })
    @PostMapping
    @PreAuthorize("hasAuthority('order:edit')")
    public ResponseEntity<ApiResponseDto<OrderResponseDto>> create(
            @Valid @RequestBody OrderRequestDto dto
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.created(service.create(dto)));
    }

    // ============================================================
    // 更新訂單
    // ============================================================
    @Operation(summary = "更新訂單")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('order:edit')")
    public ResponseEntity<ApiResponseDto<OrderResponseDto>> update(
            @PathVariable Long id,
            @Valid @RequestBody OrderRequestDto dto
    ) {
        return ResponseEntity.ok(ApiResponseDto.ok(service.update(id, dto)));
    }

    // ============================================================
    // 刪除訂單
    // ============================================================
    @Operation(summary = "刪除訂單")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('order:edit')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
