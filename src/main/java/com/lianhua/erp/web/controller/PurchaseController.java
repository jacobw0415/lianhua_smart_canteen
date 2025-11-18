package com.lianhua.erp.web.controller;

import com.lianhua.erp.dto.apiResponse.ApiResponseDto;
import com.lianhua.erp.dto.error.*;
import com.lianhua.erp.dto.purchase.*;
import com.lianhua.erp.service.PurchaseService;
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
 * 進貨單管理 API
 */
@RestController
@RequestMapping("/api/purchases")
@Tag(name = "進貨管理", description = "進貨單與付款紀錄管理 API")
@RequiredArgsConstructor
public class PurchaseController {

    private final PurchaseService purchaseService;

    // ============================================================
    // 🔥 分頁取得所有進貨單（比照 SupplierController）
    // ============================================================
    @Operation(
            summary = "分頁取得進貨單清單",
            description = """
            支援 page / size / sort，自動與 React-Admin 分頁整合。
            例如：/api/purchases?page=0&size=10&sort=id,asc
            """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功取得進貨單列表"),
            @ApiResponse(responseCode = "500", description = "伺服器錯誤")
    })
    @PageableAsQueryParam
    @GetMapping
    public ResponseEntity<ApiResponseDto<Page<PurchaseResponseDto>>> getAllPurchases(
            @ParameterObject Pageable pageable
    ) {
        Page<PurchaseResponseDto> page = purchaseService.getAllPurchases(pageable);
        return ResponseEntity.ok(ApiResponseDto.ok(page));
    }

    // ============================================================
    // 單筆查詢
    // ============================================================
    @Operation(summary = "取得指定進貨單（含付款資訊）")
    @ApiResponses({
            @ApiResponse(responseCode = "200",
                    description = "成功取得進貨單資料",
                    content = @Content(schema = @Schema(implementation = PurchaseResponseDto.class))),
            @ApiResponse(responseCode = "404",
                    description = "找不到進貨單",
                    content = @Content(schema = @Schema(implementation = NotFoundResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDto<PurchaseResponseDto>> getPurchaseById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponseDto.ok(purchaseService.getPurchaseById(id)));
    }

    // ============================================================
    // 建立進貨單
    // ============================================================
    @Operation(summary = "新增進貨單（可包含付款紀錄）")
    @ApiResponses({
            @ApiResponse(responseCode = "201",
                    description = "成功建立進貨單",
                    content = @Content(schema = @Schema(implementation = PurchaseResponseDto.class))),
            @ApiResponse(responseCode = "400",
                    description = "參數錯誤",
                    content = @Content(schema = @Schema(implementation = BadRequestResponse.class))),
            @ApiResponse(responseCode = "409",
                    description = "資料衝突或重複",
                    content = @Content(schema = @Schema(implementation = ConflictResponse.class)))
    })
    @PostMapping
    public ResponseEntity<ApiResponseDto<PurchaseResponseDto>> createPurchase(
            @Valid @RequestBody PurchaseRequestDto dto) {

        PurchaseResponseDto created = purchaseService.createPurchase(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.created(created));
    }

    // ============================================================
    // 更新進貨單（付款項目）
    // ============================================================
    @Operation(summary = "更新進貨單（允許新增/修改付款紀錄）")
    @ApiResponses({
            @ApiResponse(responseCode = "200",
                    description = "成功更新進貨單",
                    content = @Content(schema = @Schema(implementation = PurchaseResponseDto.class))),
            @ApiResponse(responseCode = "400",
                    description = "請求參數錯誤",
                    content = @Content(schema = @Schema(implementation = BadRequestResponse.class))),
            @ApiResponse(responseCode = "404",
                    description = "找不到進貨單",
                    content = @Content(schema = @Schema(implementation = NotFoundResponse.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseDto<PurchaseResponseDto>> updatePurchase(
            @PathVariable Long id,
            @Valid @RequestBody PurchaseRequestDto dto) {

        return ResponseEntity.ok(ApiResponseDto.ok(
                purchaseService.updatePurchase(id, dto)
        ));
    }

    // ============================================================
    // 更新狀態
    // ============================================================
    @Operation(summary = "更新進貨單狀態（PENDING / PARTIAL / PAID）")
    @ApiResponses({
            @ApiResponse(responseCode = "200",
                    description = "成功更新狀態",
                    content = @Content(schema = @Schema(implementation = PurchaseResponseDto.class))),
            @ApiResponse(responseCode = "400",
                    description = "無效狀態",
                    content = @Content(schema = @Schema(implementation = BadRequestResponse.class))),
            @ApiResponse(responseCode = "404",
                    description = "找不到進貨單",
                    content = @Content(schema = @Schema(implementation = NotFoundResponse.class)))
    })
    @PutMapping("/{id}/status/{status}")
    public ResponseEntity<ApiResponseDto<PurchaseResponseDto>> updateStatus(
            @PathVariable Long id,
            @PathVariable String status) {

        PurchaseResponseDto updated = purchaseService.updateStatus(id, status);
        return ResponseEntity.ok(ApiResponseDto.ok(updated));
    }

    // ============================================================
    // 刪除進貨單
    // ============================================================
    @Operation(summary = "刪除進貨單（連同付款紀錄）")
    @ApiResponses({
            @ApiResponse(responseCode = "204",
                    description = "成功刪除進貨單"),
            @ApiResponse(responseCode = "404",
                    description = "找不到進貨單",
                    content = @Content(schema = @Schema(implementation = NotFoundResponse.class)))
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePurchase(@PathVariable Long id) {
        purchaseService.deletePurchase(id);
    }
}
