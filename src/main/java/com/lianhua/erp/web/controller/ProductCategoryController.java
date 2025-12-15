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


    @Operation(
            summary = "建立新分類",
            description = "建立一筆商品分類資料（name 與 code 需唯一）。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "建立成功",
                    content = @Content(schema = @Schema(implementation = ProductCategoryResponseDto.class))),
            @ApiResponse(responseCode = "409", description = "分類名稱或代碼重複",
                    content = @Content(schema = @Schema(implementation = ConflictResponse.class))),
            @ApiResponse(responseCode = "500", description = "伺服器錯誤",
                    content = @Content(schema = @Schema(implementation = InternalServerErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<ApiResponseDto<ProductCategoryResponseDto>> create(
            @Valid @RequestBody ProductCategoryRequestDto dto) {
        ProductCategoryResponseDto created = service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponseDto.ok(created));
    }


    @Operation(
            summary = "更新分類資料",
            description = "根據分類 ID 更新名稱、代碼、描述或啟用狀態。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "404", description = "找不到分類 ID",
                    content = @Content(schema = @Schema(implementation = NotFoundResponse.class))),
            @ApiResponse(responseCode = "409", description = "名稱或代碼重複",
                    content = @Content(schema = @Schema(implementation = ConflictResponse.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseDto<ProductCategoryResponseDto>> update(
            @PathVariable Long id,
            @Valid @RequestBody ProductCategoryRequestDto dto) {
        return ResponseEntity.ok(ApiResponseDto.ok(service.update(id, dto)));
    }


    @Operation(summary = "取得所有分類清單", description = "回傳所有商品分類資料。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查詢成功",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ProductCategoryResponseDto.class)))),
            @ApiResponse(responseCode = "204", description = "目前沒有分類資料")
    })
    @GetMapping
    public ResponseEntity<ApiResponseDto<List<ProductCategoryResponseDto>>> getAll() {
        List<ProductCategoryResponseDto> list = service.getAll();
        if (list.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT)
                    .body(ApiResponseDto.error(HttpStatus.NO_CONTENT.value(), "目前沒有分類資料"));
        }
        return ResponseEntity.ok(ApiResponseDto.ok(list));
    }


    @Operation(summary = "取得啟用中分類清單", description = "回傳 active=true 的商品分類。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查詢成功"),
            @ApiResponse(responseCode = "204", description = "目前沒有啟用中分類")
    })
    @GetMapping("/active")
    public ResponseEntity<ApiResponseDto<List<ProductCategoryResponseDto>>> getActive() {
        List<ProductCategoryResponseDto> list = service.getActive();
        if (list.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT)
                    .body(ApiResponseDto.error(HttpStatus.NO_CONTENT.value(), "目前沒有啟用中分類"));
        }
        return ResponseEntity.ok(ApiResponseDto.ok(list));
    }


    @Operation(summary = "依 ID 取得分類", description = "傳入分類 ID 取得詳細資料。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查詢成功"),
            @ApiResponse(responseCode = "404", description = "找不到分類 ID",
                    content = @Content(schema = @Schema(implementation = NotFoundResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDto<ProductCategoryResponseDto>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponseDto.ok(service.getById(id)));
    }


    @Operation(
            summary = "刪除分類",
            description = "根據分類 ID 刪除指定資料。若該分類被商品引用則可能刪除失敗。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "刪除成功"),
            @ApiResponse(responseCode = "404", description = "找不到分類 ID",
                    content = @Content(schema = @Schema(implementation = NotFoundResponse.class))),
            @ApiResponse(responseCode = "409", description = "刪除失敗：分類被引用",
                    content = @Content(schema = @Schema(implementation = ConflictResponse.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseDto<Void>> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponseDto.deleted());
    }
}
