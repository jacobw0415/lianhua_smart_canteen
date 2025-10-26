package com.lianhua.erp.web.controller;

import com.lianhua.erp.dto.apiResponse.ApiResponseDto;
import com.lianhua.erp.dto.error.*;
import com.lianhua.erp.dto.orderCustomer.OrderCustomerRequestDto;
import com.lianhua.erp.dto.orderCustomer.OrderCustomerResponseDto;
import com.lianhua.erp.service.OrderCustomerService;
import io.swagger.v3.oas.annotations.Operation;
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

@RestController
@RequestMapping("/api/order-customers")
@RequiredArgsConstructor
@Tag(name = "訂單客戶管理", description = "Order Customer Management API")
public class OrderCustomerController {
    
    private final OrderCustomerService service;
    
    // ================================
    // 取得所有客戶
    // ================================
    @Operation(summary = "取得所有訂單客戶")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功取得所有客戶紀錄",
                    content = @Content(schema = @Schema(implementation = OrderCustomerResponseDto.class))),
            @ApiResponse(responseCode = "500", description = "伺服器內部錯誤",
                    content = @Content(schema = @Schema(implementation = InternalServerErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<ApiResponseDto<List<OrderCustomerResponseDto>>> getAll() {
        return ResponseEntity.ok(ApiResponseDto.ok(service.findAll()));
    }
    
    // ================================
    // 取得單筆客戶
    // ================================
    @Operation(summary = "查詢單筆訂單客戶")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功取得客戶資料",
                    content = @Content(schema = @Schema(implementation = OrderCustomerResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "找不到指定客戶紀錄",
                    content = @Content(schema = @Schema(implementation = NotFoundResponse.class))),
            @ApiResponse(responseCode = "500", description = "伺服器內部錯誤",
                    content = @Content(schema = @Schema(implementation = InternalServerErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDto<OrderCustomerResponseDto>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponseDto.ok(service.findById(id)));
    }
    
    // ================================
    // 新增客戶
    // ================================
    @Operation(summary = "新增訂單客戶")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "成功新增客戶",
                    content = @Content(schema = @Schema(implementation = OrderCustomerResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "參數格式錯誤",
                    content = @Content(schema = @Schema(implementation = BadRequestResponse.class))),
            @ApiResponse(responseCode = "409", description = "客戶名稱重複或違反唯一約束",
                    content = @Content(schema = @Schema(implementation = ConflictResponse.class))),
            @ApiResponse(responseCode = "500", description = "伺服器內部錯誤",
                    content = @Content(schema = @Schema(implementation = InternalServerErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<ApiResponseDto<OrderCustomerResponseDto>> create(
            @Valid @RequestBody OrderCustomerRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.ok(service.create(dto)));
    }
    
    // ================================
    // 更新客戶
    // ================================
    @Operation(summary = "更新訂單客戶資料")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功更新客戶資料",
                    content = @Content(schema = @Schema(implementation = OrderCustomerResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "輸入參數錯誤",
                    content = @Content(schema = @Schema(implementation = BadRequestResponse.class))),
            @ApiResponse(responseCode = "404", description = "找不到客戶紀錄",
                    content = @Content(schema = @Schema(implementation = NotFoundResponse.class))),
            @ApiResponse(responseCode = "409", description = "客戶名稱重複或違反唯一約束",
                    content = @Content(schema = @Schema(implementation = ConflictResponse.class))),
            @ApiResponse(responseCode = "500", description = "伺服器內部錯誤",
                    content = @Content(schema = @Schema(implementation = InternalServerErrorResponse.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseDto<OrderCustomerResponseDto>> update(
            @PathVariable Long id, @Valid @RequestBody OrderCustomerRequestDto dto) {
        return ResponseEntity.ok(ApiResponseDto.ok(service.update(id, dto)));
    }
    
    // ================================
    // 刪除客戶
    // ================================
    @Operation(summary = "刪除訂單客戶")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "成功刪除客戶紀錄"),
            @ApiResponse(responseCode = "404", description = "找不到客戶紀錄",
                    content = @Content(schema = @Schema(implementation = NotFoundResponse.class))),
            @ApiResponse(responseCode = "500", description = "伺服器內部錯誤",
                    content = @Content(schema = @Schema(implementation = InternalServerErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
