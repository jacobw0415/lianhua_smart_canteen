package com.lianhua.erp.web.controller;

import com.lianhua.erp.dto.apiResponse.ApiResponseDto;
import com.lianhua.erp.dto.error.ConflictResponse;
import com.lianhua.erp.dto.error.InternalServerErrorResponse;
import com.lianhua.erp.dto.error.NotFoundResponse;
import com.lianhua.erp.dto.product.*;
import com.lianhua.erp.service.ProductCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/product_categories")
@RequiredArgsConstructor
@Tag(name = "商品分類管理", description = "商品分類 CRUD 與查詢 API")
public class ProductCategoryController {

    private final ProductCategoryService service;

    // ================================================================
    // 建立商品分類
    // ================================================================
    @Operation(
            summary = "建立新分類",
            description = "建立一筆新的商品分類資料，分類名稱與代碼需為唯一值。"
    )
    @PostMapping
    public ResponseEntity<ApiResponseDto<ProductCategoryResponseDto>> create(
            @Valid @RequestBody ProductCategoryRequestDto dto) {
        ProductCategoryResponseDto created = service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponseDto.ok(created));
    }

    // ================================================================
    // 更新商品分類
    // ================================================================
    @Operation(
            summary = "更新分類資料",
            description = "依分類 ID 更新分類的基本資料（名稱、代碼、描述等）。"
    )
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseDto<ProductCategoryResponseDto>> update(
            @PathVariable Long id,
            @Valid @RequestBody ProductCategoryRequestDto dto) {
        return ResponseEntity.ok(ApiResponseDto.ok(service.update(id, dto)));
    }

    // ================================================================
    // 取得全部分類
    // ================================================================
    @Operation(
            summary = "取得所有分類清單",
            description = "查詢系統中所有商品分類（包含啟用與停用）。"
    )
    @GetMapping
    public ResponseEntity<ApiResponseDto<List<ProductCategoryResponseDto>>> getAll() {
        List<ProductCategoryResponseDto> list = service.getAll();
        if (list.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT)
                    .body(ApiResponseDto.error(HttpStatus.NO_CONTENT.value(), "目前沒有分類資料"));
        }
        return ResponseEntity.ok(ApiResponseDto.ok(list));
    }

    // ================================================================
    // 取得啟用中的分類
    // ================================================================
    @Operation(
            summary = "取得啟用中分類清單",
            description = "查詢目前可用於業務流程的商品分類（active = true）。"
    )
    @GetMapping("/active")
    public ResponseEntity<ApiResponseDto<List<ProductCategoryResponseDto>>> getActive() {
        List<ProductCategoryResponseDto> list = service.getActive();
        if (list.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT)
                    .body(ApiResponseDto.error(HttpStatus.NO_CONTENT.value(), "目前沒有啟用中分類"));
        }
        return ResponseEntity.ok(ApiResponseDto.ok(list));
    }

    // ================================================================
    // 依 ID 查詢分類
    // ================================================================
    @Operation(
            summary = "依 ID 取得分類",
            description = "根據分類 ID 取得單一商品分類的詳細資料。"
    )
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDto<ProductCategoryResponseDto>> getById(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponseDto.ok(service.getById(id)));
    }

    // ================================================================
    // 停用商品分類
    // ================================================================
    @Operation(
            summary = "停用商品分類",
            description = "將指定商品分類設為停用，停用後該分類不可用於新增銷售單。"
    )
    @PutMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponseDto<ProductCategoryResponseDto>> deactivate(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponseDto.ok(service.deactivate(id)));
    }

    // ================================================================
    // 啟用商品分類
    // ================================================================
    @Operation(
            summary = "啟用商品分類",
            description = "將指定商品分類重新設為啟用狀態，使其可再次用於業務流程。"
    )
    @PutMapping("/{id}/activate")
    public ResponseEntity<ApiResponseDto<ProductCategoryResponseDto>> activate(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponseDto.ok(service.activate(id)));
    }

    // ================================================================
    // 模糊搜尋商品分類
    // ================================================================
    @Operation(
            summary = "搜尋商品分類",
            description = "依分類名稱、代碼或啟用狀態進行模糊搜尋，未提供條件時回傳全部分類。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "搜尋成功",
                    content = @Content(array = @ArraySchema(
                            schema = @Schema(implementation = ProductCategoryResponseDto.class)
                    ))),
            @ApiResponse(responseCode = "204", description = "查無符合條件的分類資料")
    })
    @GetMapping("/search")
    public ResponseEntity<ApiResponseDto<List<ProductCategoryResponseDto>>> search(
            ProductCategorySearchRequest search) {

        List<ProductCategoryResponseDto> list = service.search(search);

        if (list.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT)
                    .body(ApiResponseDto.error(
                            HttpStatus.NO_CONTENT.value(),
                            "查無符合條件的分類資料"
                    ));
        }

        return ResponseEntity.ok(ApiResponseDto.ok(list));
    }

    // ================================================================
    // 刪除商品分類
    // ================================================================
    @Operation(
            summary = "刪除分類",
            description = "依分類 ID 刪除商品分類，若分類已被商品引用則可能刪除失敗。"
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseDto<Void>> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponseDto.deleted());
    }
}
