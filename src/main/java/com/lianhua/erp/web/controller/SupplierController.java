package com.lianhua.erp.web.controller;

import com.lianhua.erp.dto.apiResponse.ApiResponseDto;
import com.lianhua.erp.dto.error.*;
import com.lianhua.erp.dto.supplier.*;
import com.lianhua.erp.service.SupplierService;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/suppliers")
@Tag(name = "供應商管理", description = "供應商 CRUD + 搜尋 API")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService supplierService;

    // ============================================================
    // 分頁取得所有供應商
    // ============================================================
    @Operation(
            summary = "分頁取得供應商清單",
            description = """
                    支援 page / size / sort，自動與 React-Admin 分頁整合。
                    例如：/api/suppliers?page=0&size=10&sort=name,asc
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200",
                    description = "成功取得供應商列表"),
            @ApiResponse(responseCode = "500",
                    description = "伺服器錯誤")
    })
    @PageableAsQueryParam      // 🔥 美化 pageable
    @GetMapping
    @PreAuthorize("hasAuthority('purchase:view')")
    public ResponseEntity<ApiResponseDto<Page<SupplierResponseDto>>> getAllSuppliers(
            @ParameterObject Pageable pageable      // 🔥 讓 pageable 展開成 page/size/sort
    ) {
        Page<SupplierResponseDto> page = supplierService.getAllSuppliers(pageable);
        return ResponseEntity.ok(ApiResponseDto.ok(page));
    }

    // ============================================================
    // 取得單一供應商
    // ============================================================
    @Operation(summary = "取得指定供應商資料")
    @ApiResponses({
            @ApiResponse(responseCode = "200",
                    description = "成功取得供應商資料",
                    content = @Content(schema = @Schema(implementation = SupplierResponseDto.class))),
            @ApiResponse(responseCode = "404",
                    description = "找不到供應商",
                    content = @Content(schema = @Schema(implementation = NotFoundResponse.class)))
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('purchase:view')")
    public ResponseEntity<ApiResponseDto<SupplierResponseDto>> getSupplierById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponseDto.ok(supplierService.getSupplierById(id)));
    }

    // ============================================================
    // 建立供應商
    // ============================================================
    @Operation(summary = "建立新供應商")
    @ApiResponses({
            @ApiResponse(responseCode = "201",
                    description = "成功建立供應商",
                    content = @Content(schema = @Schema(implementation = SupplierResponseDto.class))),
            @ApiResponse(responseCode = "400",
                    description = "參數錯誤",
                    content = @Content(schema = @Schema(implementation = BadRequestResponse.class))),
            @ApiResponse(responseCode = "409",
                    description = "資料重複或唯一鍵衝突",
                    content = @Content(schema = @Schema(implementation = ConflictResponse.class)))
    })
    @PostMapping
    @PreAuthorize("hasAuthority('purchase:edit')")
    public ResponseEntity<ApiResponseDto<SupplierResponseDto>> createSupplier(
            @Valid @RequestBody SupplierRequestDto dto) {

        SupplierResponseDto created = supplierService.createSupplier(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.created(created));
    }

    // ============================================================
    // 更新供應商
    // ============================================================
    @Operation(summary = "更新供應商資料")
    @ApiResponses({
            @ApiResponse(responseCode = "200",
                    description = "更新成功",
                    content = @Content(schema = @Schema(implementation = SupplierResponseDto.class))),
            @ApiResponse(responseCode = "404",
                    description = "找不到供應商",
                    content = @Content(schema = @Schema(implementation = NotFoundResponse.class)))
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('purchase:edit')")
    public ResponseEntity<ApiResponseDto<SupplierResponseDto>> updateSupplier(
            @PathVariable Long id,
            @Valid @RequestBody SupplierRequestDto dto) {

        return ResponseEntity.ok(ApiResponseDto.ok(supplierService.updateSupplier(id, dto)));
    }

    // ============================================================
    // 停用供應商（active = false）
    // ============================================================
    @Operation(summary = "停用供應商（active = false）")
    @ApiResponses({
            @ApiResponse(responseCode = "200",
                    description = "停用成功",
                    content = @Content(schema = @Schema(implementation = SupplierResponseDto.class))),
            @ApiResponse(responseCode = "404",
                    description = "找不到供應商",
                    content = @Content(schema = @Schema(implementation = NotFoundResponse.class)))
    })
    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('purchase:edit')")
    public ResponseEntity<ApiResponseDto<SupplierResponseDto>> deactivateSupplier(
            @PathVariable Long id) {

        SupplierResponseDto updated = supplierService.deactivateSupplier(id);
        return ResponseEntity.ok(ApiResponseDto.ok(updated));
    }

    // ============================================================
    // 啟用供應商（active = true）
    // ============================================================
    @Operation(summary = "啟用供應商（active = true）")
    @ApiResponses({
            @ApiResponse(responseCode = "200",
                    description = "啟用成功",
                    content = @Content(schema = @Schema(implementation = SupplierResponseDto.class))),
            @ApiResponse(responseCode = "404",
                    description = "找不到供應商",
                    content = @Content(schema = @Schema(implementation = NotFoundResponse.class)))
    })
    @PutMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('purchase:edit')")
    public ResponseEntity<ApiResponseDto<SupplierResponseDto>> activateSupplier(
            @PathVariable Long id) {

        SupplierResponseDto updated = supplierService.activateSupplier(id);
        return ResponseEntity.ok(ApiResponseDto.ok(updated));
    }


    // ============================================================
    // 刪除供應商
    // ============================================================
    @Operation(summary = "刪除供應商")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('purchase:edit')")
    public ResponseEntity<ApiResponseDto<Void>> deleteSupplier(@PathVariable Long id) {
        supplierService.deleteSupplier(id);
        return ResponseEntity.ok(ApiResponseDto.deleted());
    }

    // ============================================================
    // 分頁搜尋供應商 (模糊搜尋 + 精確搜尋)
    // ============================================================
    @Operation(
            summary = "搜尋供應商（支援分頁 + 模糊搜尋 + 精確搜尋）",
            description = """
                    可依名稱、聯絡人、電話、結帳週期、備註搜尋。
                    支援 page / size / sort，自動整合 React-Admin 分頁。
                    
                    範例：
                    /api/suppliers/search?page=0&size=10&sort=name,asc&supplierName=食品
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "搜尋成功",
                    content = @Content(schema = @Schema(implementation = SupplierResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "搜尋條件全為空",
                    content = @Content(schema = @Schema(implementation = BadRequestResponse.class))),
            @ApiResponse(responseCode = "404", description = "查無匹配資料",
                    content = @Content(schema = @Schema(implementation = NotFoundResponse.class)))
    })
    @PageableAsQueryParam   // 🔥 讓 Swagger 把 Pageable 展開成 page/size/sort
    @GetMapping("/search")
    @PreAuthorize("hasAuthority('purchase:view')")
    public ResponseEntity<ApiResponseDto<Page<SupplierResponseDto>>> searchSuppliers(
            @ParameterObject @ModelAttribute SupplierSearchRequest req, // 🔥 搜尋參數展開
            @ParameterObject Pageable pageable                         // 🔥 分頁展開
    ) {

        Page<SupplierResponseDto> page = supplierService.searchSuppliers(req, pageable);

        return ResponseEntity.ok(ApiResponseDto.ok(page));
    }

    // ============================================================
    // 取得所有啟用中的供應商（active = true）
    // ============================================================
    @Operation(
            summary = "取得啟用中的供應商列表",
            description = "此 API 專供前端下拉選單使用，只回傳 active = true 的供應商"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200",
                    description = "成功取得啟用中的供應商列表",
                    content = @Content(schema = @Schema(implementation = SupplierResponseDto.class))),
            @ApiResponse(responseCode = "404",
                    description = "沒有啟用中的供應商",
                    content = @Content(schema = @Schema(implementation = NotFoundResponse.class)))
    })
    @GetMapping("/active")
    @PreAuthorize("hasAuthority('purchase:view')")
    public ResponseEntity<ApiResponseDto<List<SupplierResponseDto>>> getActiveSuppliers() {

        List<SupplierResponseDto> list = supplierService.getActiveSuppliers();
        return ResponseEntity.ok(ApiResponseDto.ok(list));
    }

}
