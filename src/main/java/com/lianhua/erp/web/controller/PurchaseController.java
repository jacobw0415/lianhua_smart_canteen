package com.lianhua.erp.web.controller;

import com.lianhua.erp.dto.apiResponse.ApiResponseDto;
import com.lianhua.erp.dto.error.*;
import com.lianhua.erp.dto.export.ExportPayload;
import com.lianhua.erp.dto.purchase.*;
import com.lianhua.erp.export.ExportFormat;
import com.lianhua.erp.export.ExportScope;
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
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

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
    @PreAuthorize("hasAuthority('purchase:view')")
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
    @PreAuthorize("hasAuthority('purchase:view')")
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
                    description = "參數錯誤（包含供應商停用無法建立進貨單）",
                    content = @Content(schema = @Schema(implementation = BadRequestResponse.class))),
            @ApiResponse(responseCode = "409",
                    description = "資料衝突或重複",
                    content = @Content(schema = @Schema(implementation = ConflictResponse.class)))
    })
    @PostMapping
    @PreAuthorize("hasAuthority('purchase:edit')")
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
                    description = "請求參數錯誤（包含供應商停用不可更新進貨單）",
                    content = @Content(schema = @Schema(implementation = BadRequestResponse.class))),
            @ApiResponse(responseCode = "404",
                    description = "找不到進貨單",
                    content = @Content(schema = @Schema(implementation = NotFoundResponse.class)))
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('purchase:edit')")
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
    @PreAuthorize("hasAuthority('purchase:edit')")
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
    @PreAuthorize("hasAuthority('purchase:edit')")
    public void deletePurchase(@PathVariable Long id) {
        purchaseService.deletePurchase(id);
    }

    // ============================================================
    // 作廢進貨單
    // ============================================================
    @Operation(
            summary = "作廢進貨單",
            description = """
                    將進貨單標記為作廢。作廢後會自動作廢所有相關的有效付款單。
                    任何狀態的進貨單都可以作廢（PENDING / PARTIAL / PAID）。
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "作廢成功"),
            @ApiResponse(responseCode = "400", description = "進貨單已經作廢",
                    content = @Content(schema = @Schema(implementation = BadRequestResponse.class))),
            @ApiResponse(responseCode = "404", description = "找不到進貨單",
                    content = @Content(schema = @Schema(implementation = NotFoundResponse.class))),
            @ApiResponse(responseCode = "500", description = "伺服器錯誤",
                    content = @Content(schema = @Schema(implementation = InternalServerErrorResponse.class)))
    })
    @PostMapping("/{id}/void")
    @PreAuthorize("hasAuthority('purchase:edit')")
    public ResponseEntity<ApiResponseDto<PurchaseResponseDto>> voidPurchase(
            @PathVariable Long id,
            @RequestBody(required = false) java.util.Map<String, String> request) {
        
        String reason = request != null ? request.get("reason") : null;
        PurchaseResponseDto result = purchaseService.voidPurchase(id, reason);
        return ResponseEntity.ok(ApiResponseDto.ok(result));
    }

    // ============================================================
    // 🔍 搜尋進貨單
    // ============================================================
    @Operation(
            summary = "搜尋進貨單（支援分頁 + 模糊與精準搜尋）",
            description = """
                    可依以下條件組合搜尋：
                    - 供應商名稱（supplierName, 模糊）
                    - 品項（item, 模糊）
                    - 狀態（status, 精準）
                    - 會計期間（accountingPeriod, 精準 YYYY-MM）
                    - 供應商 ID（supplierId, 精準）
                    - 進貨單編號（purchaseNo, 模糊）
                    - 起始日期（fromDate >=）
                    - 結束日期（toDate <=）
                    
                    與 React-Admin 的 List / Filter 完整整合。
                    範例：
                    /api/purchases/search?page=0&size=10&sort=id,asc&supplierName=食品&fromDate=2025-01-01
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200",
                    description = "搜尋成功",
                    content = @Content(schema = @Schema(implementation = PurchaseResponseDto.class))),
            @ApiResponse(responseCode = "400",
                    description = "搜尋條件錯誤",
                    content = @Content(schema = @Schema(implementation = BadRequestResponse.class))),
            @ApiResponse(responseCode = "404",
                    description = "無符合條件資料",
                    content = @Content(schema = @Schema(implementation = NotFoundResponse.class)))
    })
    @PageableAsQueryParam
    @GetMapping("/search")
    @PreAuthorize("hasAuthority('purchase:view')")
    public ResponseEntity<ApiResponseDto<Page<PurchaseResponseDto>>> searchPurchases(
            @ParameterObject @ModelAttribute PurchaseSearchRequest req,   //  自動綁定查詢參數
            @ParameterObject Pageable pageable                            //  Page / size / sort
    ) {
        Page<PurchaseResponseDto> page = purchaseService.searchPurchases(req, pageable);
        return ResponseEntity.ok(ApiResponseDto.ok(page));
    }

    // ============================================================
    // ✅ 匯出進貨單（與 /search 相同條件）
    // ============================================================
    @Operation(
            summary = "匯出進貨單列表",
            description = """
                    篩選條件與 GET /api/purchases/search 相同。
                    - scope=all（預設）：匯出全部符合條件資料（受 app.export.max-rows 限制）
                    - scope=page：匯出目前列表分頁
                    - format：xlsx（預設）或 csv
                    """
    )
    @PageableAsQueryParam
    @GetMapping(value = "/export", produces = {
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "text/csv; charset=UTF-8"
    })
    @PreAuthorize("hasAuthority('purchase:view')")
    public ResponseEntity<byte[]> exportPurchases(
            @ParameterObject @ModelAttribute PurchaseSearchRequest req,
            @ParameterObject Pageable pageable,
            @RequestParam(required = false) String format,
            @RequestParam(required = false) String scope
    ) {
        String resolvedScope = (scope == null || scope.isBlank()) ? "all" : scope;
        ExportPayload payload = purchaseService.exportPurchases(
                req,
                pageable,
                ExportFormat.fromQueryParam(format),
                ExportScope.fromQueryParam(resolvedScope)
        );

        ContentDisposition disposition = ContentDisposition.builder("attachment")
                .filename(payload.filename(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType(payload.mediaType()))
                .body(payload.data());
    }
}
