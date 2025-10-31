package com.lianhua.erp.web.controller;

import com.lianhua.erp.dto.apiResponse.ApiResponseDto;
import com.lianhua.erp.dto.error.*;
import com.lianhua.erp.dto.purchase.*;
import com.lianhua.erp.service.PurchaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 進貨單管理 API
 * 功能：查詢、新增、更新、刪除、（含付款紀錄處理）
 */
@RestController
@RequestMapping("/api/purchases")
@Tag(name = "進貨管理", description = "進貨單與付款紀錄管理 API")
@RequiredArgsConstructor
public class PurchaseController {
    
    private final PurchaseService purchaseService;
    
    @Operation(summary = "取得所有進貨單（含付款紀錄）")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功取得所有進貨單",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = PurchaseResponseDto.class)))),
            @ApiResponse(responseCode = "500", description = "伺服器錯誤",
                    content = @Content(schema = @Schema(implementation = InternalServerErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<ApiResponseDto<List<PurchaseResponseDto>>> getAllPurchases() {
        return ResponseEntity.ok(ApiResponseDto.ok(purchaseService.getAllPurchases()));
    }
    
    @Operation(summary = "取得指定進貨單（含付款紀錄）")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功取得進貨單資料",
                    content = @Content(schema = @Schema(implementation = PurchaseResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "找不到進貨單",
                    content = @Content(schema = @Schema(implementation = NotFoundResponse.class))),
            @ApiResponse(responseCode = "500", description = "伺服器錯誤",
                    content = @Content(schema = @Schema(implementation = InternalServerErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDto<PurchaseResponseDto>> getPurchaseById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponseDto.ok(purchaseService.getPurchaseById(id)));
    }
    
    @Operation(summary = "新增進貨單（可同時新增付款紀錄）")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "成功建立進貨單",
                    content = @Content(schema = @Schema(implementation = PurchaseResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "參數錯誤",
                    content = @Content(schema = @Schema(implementation = BadRequestResponse.class))),
            @ApiResponse(responseCode = "409", description = "資料衝突或重複",
                    content = @Content(schema = @Schema(implementation = ConflictResponse.class))),
            @ApiResponse(responseCode = "500", description = "伺服器錯誤",
                    content = @Content(schema = @Schema(implementation = InternalServerErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<ApiResponseDto<PurchaseResponseDto>> createPurchase(
            @Valid @RequestBody PurchaseRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.created(purchaseService.createPurchase(dto)));
    }
    
    @Operation(summary = "更新進貨單資料（可更新付款）")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功更新進貨單",
                    content = @Content(schema = @Schema(implementation = PurchaseResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "請求參數錯誤",
                    content = @Content(schema = @Schema(implementation = BadRequestResponse.class))),
            @ApiResponse(responseCode = "404", description = "找不到進貨單",
                    content = @Content(schema = @Schema(implementation = NotFoundResponse.class))),
            @ApiResponse(responseCode = "500", description = "伺服器錯誤",
                    content = @Content(schema = @Schema(implementation = InternalServerErrorResponse.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseDto<PurchaseResponseDto>> updatePurchase(
            @PathVariable Long id,
            @Valid @RequestBody PurchaseRequestDto dto) {
        return ResponseEntity.ok(ApiResponseDto.ok(purchaseService.updatePurchase(id, dto)));
    }
    
    @Operation(summary = "更新進貨單狀態")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功更新進貨單狀態",
                    content = @Content(schema = @Schema(implementation = PurchaseResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "無效的狀態值",
                    content = @Content(schema = @Schema(implementation = BadRequestResponse.class))),
            @ApiResponse(responseCode = "404", description = "找不到進貨單",
                    content = @Content(schema = @Schema(implementation = NotFoundResponse.class))),
            @ApiResponse(responseCode = "500", description = "伺服器錯誤",
                    content = @Content(schema = @Schema(implementation = InternalServerErrorResponse.class)))
    })
    @PutMapping("/{id}/status/{status}")
    public ResponseEntity<PurchaseResponseDto> updateStatus(
            @PathVariable Long id,
            @PathVariable String status) {
        PurchaseResponseDto updated = purchaseService.updateStatus(id, status);
        return ResponseEntity.ok(updated);
    }
    
    @Operation(summary = "刪除進貨單（含付款紀錄）")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "成功刪除進貨單"),
            @ApiResponse(responseCode = "404", description = "找不到進貨單",
                    content = @Content(schema = @Schema(implementation = NotFoundResponse.class))),
            @ApiResponse(responseCode = "500", description = "伺服器錯誤",
                    content = @Content(schema = @Schema(implementation = InternalServerErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePurchase(@PathVariable Long id) {
        purchaseService.deletePurchase(id);
    }
}
