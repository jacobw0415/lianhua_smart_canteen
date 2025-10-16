package com.lianhua.erp.web.controller;

import com.lianhua.erp.dto.apiResponse.ApiResponseDto;
import com.lianhua.erp.dto.sale.*;
import com.lianhua.erp.service.SalesService;
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
@RequestMapping("/api/sales")
@RequiredArgsConstructor
@Tag(name = "銷售管理", description = "銷售紀錄查詢與管理 API")
public class SalesController {
    
    private final SalesService service;
    
    @Operation(summary = "建立銷售紀錄", description = "建立新的銷售資料。若同日同商品已存在則會拒絕。")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "建立成功",
                    content = @Content(schema = @Schema(implementation = SalesResponseDto.class))),
            @ApiResponse(responseCode = "409", description = "重複紀錄或唯一性衝突"),
            @ApiResponse(responseCode = "404", description = "找不到商品 ID")
    })
    @PostMapping
    public ResponseEntity<ApiResponseDto<SalesResponseDto>> create(@Valid @RequestBody SalesRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.ok(service.create(dto)));
    }
    
    @Operation(summary = "更新銷售紀錄", description = "依照 ID 更新銷售紀錄。")
    @ApiResponse(responseCode = "200", description = "更新成功")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseDto<SalesResponseDto>> update(
            @PathVariable Long id,
            @Valid @RequestBody SalesRequestDto dto) {
        return ResponseEntity.ok(ApiResponseDto.ok(service.update(id, dto)));
    }
    
    @Operation(summary = "刪除銷售紀錄")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "刪除成功"),
            @ApiResponse(responseCode = "404", description = "找不到指定 ID")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseDto<Void>> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponseDto.deleted());
    }
    
    @Operation(summary = "取得所有銷售紀錄")
    @ApiResponse(responseCode = "200", description = "查詢成功")
    @GetMapping
    public ResponseEntity<ApiResponseDto<List<SalesResponseDto>>> findAll() {
        return ResponseEntity.ok(ApiResponseDto.ok(service.findAll()));
    }
    
    @Operation(summary = "依商品 ID 查詢銷售紀錄")
    @ApiResponse(responseCode = "200", description = "查詢成功")
    @GetMapping("/product/{productId}")
    public ResponseEntity<ApiResponseDto<List<SalesResponseDto>>> findByProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(ApiResponseDto.ok(service.findByProduct(productId)));
    }
    
    @Operation(summary = "依 ID 查詢單筆銷售紀錄")
    @ApiResponse(responseCode = "200", description = "查詢成功")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDto<SalesResponseDto>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponseDto.ok(service.findById(id)));
    }
}
