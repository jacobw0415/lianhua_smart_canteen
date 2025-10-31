package com.lianhua.erp.web.controller;

import com.lianhua.erp.dto.apiResponse.ApiResponseDto;
import com.lianhua.erp.dto.error.ConflictResponse;
import com.lianhua.erp.dto.error.InternalServerErrorResponse;
import com.lianhua.erp.dto.error.NotFoundResponse;
import com.lianhua.erp.dto.product.*;
import com.lianhua.erp.service.ProductService;
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
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "商品管理", description = "商品資料維護與查詢 API")
public class ProductController {

    private final ProductService service;

    @Operation(
            summary = "建立新商品",
            description = "建立一筆新的商品資料。若商品名稱或資料不合法，將回傳錯誤。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "建立成功",
                    content = @Content(schema = @Schema(implementation = ProductResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "請求參數錯誤"),
            @ApiResponse(responseCode = "409", description = "資料重複或違反約束"),
            @ApiResponse(responseCode = "500", description = "伺服器錯誤")
    })
    @PostMapping
    public ResponseEntity<ApiResponseDto<ProductResponseDto>> create(@Valid @RequestBody ProductRequestDto dto) {
        ProductResponseDto created = service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponseDto.ok(created));
    }

    @Operation(
            summary = "更新商品資料",
            description = "根據商品 ID 更新其屬性資料（例如名稱、價格、是否啟用等）。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "400", description = "請求參數錯誤"),
            @ApiResponse(responseCode = "404", description = "找不到指定商品 ID"),
            @ApiResponse(responseCode = "500", description = "伺服器錯誤")
    })
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseDto<ProductResponseDto>> update(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequestDto dto) {
        return ResponseEntity.ok(ApiResponseDto.ok(service.update(id, dto)));
    }

    @Operation(
            summary = "取得所有商品清單",
            description = "回傳所有商品資料（包含啟用與停用商品）。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查詢成功",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ProductResponseDto.class)))),
            @ApiResponse(responseCode = "500", description = "伺服器錯誤")
    })
    @GetMapping
    public ResponseEntity<ApiResponseDto<List<ProductResponseDto>>> getAll() {
        return ResponseEntity.ok(ApiResponseDto.ok(service.getAll()));
    }

    @Operation(
            summary = "取得啟用中商品清單",
            description = "僅回傳 active=true 的商品。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查詢成功"),
            @ApiResponse(responseCode = "204", description = "目前沒有啟用中商品")
    })
    @GetMapping("/active")
    public ResponseEntity<ApiResponseDto<List<ProductResponseDto>>> getActive() {
        List<ProductResponseDto> list = service.getActiveProducts();
        if (list.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT)
                    .body(ApiResponseDto.error(HttpStatus.NO_CONTENT.value(), "目前沒有啟用中商品"));
        }
        return ResponseEntity.ok(ApiResponseDto.ok(list));
    }

    @Operation(
            summary = "依 ID 取得商品",
            description = "傳入商品 ID 取得詳細資料，不包含關聯清單。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查詢成功"),
            @ApiResponse(responseCode = "404", description = "找不到指定商品 ID"),
            @ApiResponse(responseCode = "500", description = "伺服器錯誤")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDto<ProductResponseDto>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponseDto.ok(service.getById(id)));
    }

    @Operation(
            summary = "取得商品（含銷售與訂單關聯）",
            description = "傳入商品 ID 取得商品與其銷售、訂單明細的 ID 清單（僅顯示 ID）。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查詢成功"),
            @ApiResponse(responseCode = "404", description = "找不到商品或關聯資料"),
            @ApiResponse(responseCode = "500", description = "伺服器錯誤")
    })
    @GetMapping("/{id}/relations")
    public ResponseEntity<ApiResponseDto<ProductResponseDto>> getWithRelations(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponseDto.ok(service.getWithRelations(id)));
    }

    @Operation(
            summary = "依分類 ID 取得商品清單",
            description = "傳入分類 ID，回傳該分類下的所有商品。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查詢成功",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ProductResponseDto.class)))),
            @ApiResponse(responseCode = "204", description = "該分類下目前沒有商品"),
            @ApiResponse(responseCode = "404", description = "找不到指定分類 ID",
                    content = @Content(schema = @Schema(implementation = NotFoundResponse.class))),
            @ApiResponse(responseCode = "500", description = "伺服器錯誤",
                    content = @Content(schema = @Schema(implementation = InternalServerErrorResponse.class)))
    })
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<ApiResponseDto<List<ProductResponseDto>>> getByCategory(
            @PathVariable Long categoryId) {

        List<ProductResponseDto> list = service.getByCategory(categoryId);

        if (list.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT)
                    .body(ApiResponseDto.error(HttpStatus.NO_CONTENT.value(), "該分類下目前沒有商品"));
        }

        return ResponseEntity.ok(ApiResponseDto.ok(list));
    }


    @Operation(
            summary = "刪除商品",
            description = "根據商品 ID 刪除指定資料。若該商品有關聯銷售或訂單，將拒絕刪除。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "刪除成功"),
            @ApiResponse(responseCode = "404", description = "找不到商品 ID",
                    content = @Content(schema = @Schema(implementation = NotFoundResponse.class))),
            @ApiResponse(responseCode = "409", description = "刪除失敗：存在關聯資料",
                    content = @Content(schema = @Schema(implementation = ConflictResponse.class))),
            @ApiResponse(responseCode = "500", description = "伺服器錯誤",
                    content = @Content(schema = @Schema(implementation = InternalServerErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
