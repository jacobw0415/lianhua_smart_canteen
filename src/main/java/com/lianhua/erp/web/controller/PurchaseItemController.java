package com.lianhua.erp.web.controller;

import com.lianhua.erp.dto.apiResponse.ApiResponseDto;
import com.lianhua.erp.dto.error.BadRequestResponse;
import com.lianhua.erp.dto.error.NotFoundResponse;
import com.lianhua.erp.dto.purchase.PurchaseItemDto;
import com.lianhua.erp.dto.purchase.PurchaseItemRequestDto;
import com.lianhua.erp.service.PurchaseItemService;
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

import java.util.List;

/**
 * 採購明細管理 API
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "採購明細管理", description = "Purchase Items Management API")
public class PurchaseItemController {
    
    private final PurchaseItemService service;
    
    /* ============================================================
     * 📌 分頁取得所有採購明細清單（不分採購單）
     * ============================================================ */
    @Operation(
            summary = "分頁取得所有採購明細清單",
            description = """
                    支援 page / size / sort，自動與 React-Admin 分頁整合。
                    例如：/api/purchase-items?page=0&size=10&sort=id,desc
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功取得採購明細列表",
                    content = @Content(schema = @Schema(implementation = PurchaseItemDto.class))),
            @ApiResponse(responseCode = "500", description = "伺服器錯誤")
    })
    @PageableAsQueryParam
    @GetMapping("/api/purchase-items")
    public ResponseEntity<ApiResponseDto<Page<PurchaseItemDto>>> getAllPurchaseItems(
            @ParameterObject Pageable pageable
    ) {
        Page<PurchaseItemDto> page = service.findAllPaged(pageable);
        return ResponseEntity.ok(ApiResponseDto.ok(page));
    }
    
    /* ============================================================
     * 📌 取得指定採購單的所有明細
     * ============================================================ */
    @Operation(
            summary = "取得指定採購單的所有明細",
            description = "取得指定採購單 ID 的所有明細項目。路徑：/api/purchases/{purchaseId}/items"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功取得明細清單",
                    content = @Content(schema = @Schema(implementation = PurchaseItemDto.class))),
            @ApiResponse(responseCode = "404", description = "找不到採購單",
                    content = @Content(schema = @Schema(implementation = NotFoundResponse.class)))
    })
    @GetMapping("/api/purchases/{purchaseId}/items")
    public ResponseEntity<ApiResponseDto<List<PurchaseItemDto>>> findByPurchaseId(
            @PathVariable Long purchaseId) {
        var items = service.findByPurchaseId(purchaseId);
        return ResponseEntity.ok(ApiResponseDto.ok(items));
    }
    
    /* ============================================================
     * 📌 新增採購明細
     * ============================================================ */
    @Operation(
            summary = "新增採購明細",
            description = """
                    為指定採購單新增一筆明細項目。路徑：/api/purchases/{purchaseId}/items
                    
                    注意事項：
                    - 已全額付清的採購單不可新增明細
                    - 已作廢的採購單不可新增明細
                    - 新增後會自動重新計算採購單總金額
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "成功新增明細",
                    content = @Content(schema = @Schema(implementation = PurchaseItemDto.class))),
            @ApiResponse(responseCode = "400", description = "請求參數錯誤或業務規則違反",
                    content = @Content(schema = @Schema(implementation = BadRequestResponse.class))),
            @ApiResponse(responseCode = "404", description = "找不到採購單",
                    content = @Content(schema = @Schema(implementation = NotFoundResponse.class)))
    })
    @PostMapping("/api/purchases/{purchaseId}/items")
    public ResponseEntity<ApiResponseDto<PurchaseItemDto>> create(
            @PathVariable Long purchaseId,
            @Valid @RequestBody PurchaseItemRequestDto dto) {
        
        var created = service.create(purchaseId, dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.created(created));
    }
    
    /* ============================================================
     * 📌 更新指定的採購明細
     * ============================================================ */
    @Operation(
            summary = "更新指定的採購明細",
            description = """
                    更新指定採購單的指定明細項目。路徑：/api/purchases/{purchaseId}/items/{itemId}
                    
                    注意事項：
                    - 已全額付清的採購單不可修改明細
                    - 已作廢的採購單不可修改明細
                    - 更新後會自動重新計算採購單總金額
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功更新明細",
                    content = @Content(schema = @Schema(implementation = PurchaseItemDto.class))),
            @ApiResponse(responseCode = "400", description = "請求參數錯誤或業務規則違反",
                    content = @Content(schema = @Schema(implementation = BadRequestResponse.class))),
            @ApiResponse(responseCode = "404", description = "找不到採購單或明細",
                    content = @Content(schema = @Schema(implementation = NotFoundResponse.class)))
    })
    @PutMapping("/api/purchases/{purchaseId}/items/{itemId}")
    public ResponseEntity<ApiResponseDto<PurchaseItemDto>> update(
            @PathVariable Long purchaseId,
            @PathVariable Long itemId,
            @Valid @RequestBody PurchaseItemRequestDto dto) {
        
        var updated = service.update(purchaseId, itemId, dto);
        return ResponseEntity.ok(ApiResponseDto.ok(updated));
    }
    
    /* ============================================================
     * 📌 刪除指定的採購明細
     * ============================================================ */
    @Operation(
            summary = "刪除指定的採購明細",
            description = """
                    刪除指定採購單的指定明細項目。路徑：/api/purchases/{purchaseId}/items/{itemId}
                    
                    注意事項：
                    - 已全額付清的採購單不可刪除明細
                    - 已作廢的採購單不可刪除明細
                    - 採購單至少需要保留一筆明細
                    - 刪除後會自動重新計算採購單總金額
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "刪除成功"),
            @ApiResponse(responseCode = "400", description = "業務規則違反（如為最後一筆明細）",
                    content = @Content(schema = @Schema(implementation = BadRequestResponse.class))),
            @ApiResponse(responseCode = "404", description = "找不到採購單或明細",
                    content = @Content(schema = @Schema(implementation = NotFoundResponse.class)))
    })
    @DeleteMapping("/api/purchases/{purchaseId}/items/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long purchaseId,
            @PathVariable Long itemId) {
        
        service.delete(purchaseId, itemId);
    }
}

