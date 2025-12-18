package com.lianhua.erp.web.controller;

import com.lianhua.erp.dto.apiResponse.ApiResponseDto;
import com.lianhua.erp.dto.error.*;
import com.lianhua.erp.dto.sale.*;
import com.lianhua.erp.service.SalesService;

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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 銷售管理 API
 */
@RestController
@RequestMapping("/api/sales")
@Tag(name = "銷售管理", description = "銷售紀錄管理 API")
@RequiredArgsConstructor
public class SalesController {

    private final SalesService salesService;

    // ============================================================
    // 🔥 分頁取得所有銷售紀錄（比照 PurchaseController）
    // ============================================================
    @Operation(
            summary = "分頁取得銷售紀錄清單",
            description = """
                    支援 page / size / sort，自動與 React-Admin 分頁整合。
                    例如：/api/sales?page=0&size=10&sort=id,asc
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功取得銷售紀錄列表"),
            @ApiResponse(responseCode = "500", description = "伺服器錯誤")
    })
    @PageableAsQueryParam
    @GetMapping
    public ResponseEntity<ApiResponseDto<Page<SalesResponseDto>>> getAllSales(
            @ParameterObject Pageable pageable
    ) {
        Page<SalesResponseDto> page = salesService.getAllSales( pageable);
        return ResponseEntity.ok(ApiResponseDto.ok(page));
    }

    // ============================================================
    // 單筆查詢
    // ============================================================
    @Operation(summary = "取得指定銷售紀錄")
    @ApiResponses({
            @ApiResponse(responseCode = "200",
                    description = "成功取得銷售資料",
                    content = @Content(schema = @Schema(implementation = SalesResponseDto.class))),
            @ApiResponse(responseCode = "404",
                    description = "找不到銷售紀錄",
                    content = @Content(schema = @Schema(implementation = NotFoundResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDto<SalesResponseDto>> getSaleById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(ApiResponseDto.ok(
                salesService.findById(id)
        ));
    }

    // ============================================================
    // 建立銷售紀錄
    // ============================================================
    @Operation(summary = "新增銷售紀錄")
    @ApiResponses({
            @ApiResponse(responseCode = "201",
                    description = "成功建立銷售紀錄",
                    content = @Content(schema = @Schema(implementation = SalesResponseDto.class))),
            @ApiResponse(responseCode = "400",
                    description = "參數錯誤",
                    content = @Content(schema = @Schema(implementation = BadRequestResponse.class))),
            @ApiResponse(responseCode = "409",
                    description = "資料衝突或重複",
                    content = @Content(schema = @Schema(implementation = ConflictResponse.class)))
    })
    @PostMapping
    public ResponseEntity<ApiResponseDto<SalesResponseDto>> createSale(
            @Valid @RequestBody SalesRequestDto dto
    ) {
        SalesResponseDto created = salesService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.created(created));
    }

    // ============================================================
    // 更新銷售紀錄
    // ============================================================
    @Operation(summary = "更新銷售紀錄")
    @ApiResponses({
            @ApiResponse(responseCode = "200",
                    description = "成功更新銷售紀錄",
                    content = @Content(schema = @Schema(implementation = SalesResponseDto.class))),
            @ApiResponse(responseCode = "400",
                    description = "參數錯誤",
                    content = @Content(schema = @Schema(implementation = BadRequestResponse.class))),
            @ApiResponse(responseCode = "404",
                    description = "找不到銷售紀錄",
                    content = @Content(schema = @Schema(implementation = NotFoundResponse.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseDto<SalesResponseDto>> updateSale(
            @PathVariable Long id,
            @Valid @RequestBody SalesRequestDto dto
    ) {
        return ResponseEntity.ok(ApiResponseDto.ok(
                salesService.update(id, dto)
        ));
    }

    // ============================================================
    // 刪除銷售紀錄
    // ============================================================
    @Operation(summary = "刪除銷售紀錄")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "成功刪除銷售紀錄"),
            @ApiResponse(responseCode = "404",
                    description = "找不到銷售紀錄",
                    content = @Content(schema = @Schema(implementation = NotFoundResponse.class)))
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSale(@PathVariable Long id) {
        salesService.delete(id);
    }

    // ============================================================
    // 🔍 搜尋銷售紀錄（分頁 + 條件）
    // ============================================================
    @Operation(
            summary = "搜尋銷售紀錄（支援分頁 + 模糊與精準搜尋）",
            description = """
                    可依以下條件組合搜尋：
                    - 商品名稱（productName, 模糊）
                    - 付款方式（payMethod, 精準）
                    - 起始日期（saleDateFrom >=）
                    - 結束日期（saleDateTo <=）

                    與 React-Admin 的 List / Filter 完整整合。
                    範例：
                    /api/sales/search?page=0&size=10&sort=id,asc&productName=便當&saleDateFrom=2025-01-01
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200",
                    description = "搜尋成功",
                    content = @Content(schema = @Schema(implementation = SalesResponseDto.class))),
            @ApiResponse(responseCode = "400",
                    description = "搜尋條件錯誤",
                    content = @Content(schema = @Schema(implementation = BadRequestResponse.class))),
            @ApiResponse(responseCode = "404",
                    description = "無符合條件資料",
                    content = @Content(schema = @Schema(implementation = NotFoundResponse.class)))
    })
    @PageableAsQueryParam
    @GetMapping("/search")
    public ResponseEntity<ApiResponseDto<Page<SalesResponseDto>>> searchSales(
            @ParameterObject @ModelAttribute SaleSearchRequestDto req,
            @ParameterObject Pageable pageable
    ) {
        Page<SalesResponseDto> page = salesService.search(req, pageable);
        return ResponseEntity.ok(ApiResponseDto.ok(page));
    }
}
