package com.lianhua.erp.web.controller;

import com.lianhua.erp.dto.apiResponse.ApiResponseDto;
import com.lianhua.erp.dto.error.*;
import com.lianhua.erp.dto.export.ExportPayload;
import com.lianhua.erp.dto.order.*;
import com.lianhua.erp.export.ExportFormat;
import com.lianhua.erp.export.ExportScope;
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
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.nio.charset.StandardCharsets;

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
    // ★ 匯出訂單列表（篩選與 /search 相同；scope=all 為全部符合列）
    // ============================================================
    @Operation(
            summary = "匯出訂單列表",
            description = """
                    篩選條件與 GET /api/orders/search 相同（含訂單日／交貨日區間、狀態等）。
                    - scope=all（預設）：依篩選條件匯出全部符合列（受 app.export.max-rows 限制）。
                    - scope=page：與目前列表相同的 page / size / sort，只匯出本頁。
                    - format：xlsx（預設）或 csv。
                    """
    )
    @PageableAsQueryParam
    @GetMapping(value = "/export", produces = {
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "text/csv; charset=UTF-8"
    })
    @PreAuthorize("hasAuthority('order:view')")
    public ResponseEntity<byte[]> exportOrders(
            @ParameterObject OrderSearchRequest searchRequest,
            @ParameterObject Pageable pageable,
            @RequestParam(required = false) String format,
            @RequestParam(required = false) String scope
    ) {
        String resolvedScope = (scope == null || scope.isBlank()) ? "all" : scope;
        ExportPayload payload = service.exportOrders(
                searchRequest,
                pageable,
                ExportFormat.fromQueryParam(format),
                ExportScope.fromQueryParam(resolvedScope));

        ContentDisposition disposition = ContentDisposition.builder("attachment")
                .filename(payload.filename(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType(payload.mediaType()))
                .body(payload.data());
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
