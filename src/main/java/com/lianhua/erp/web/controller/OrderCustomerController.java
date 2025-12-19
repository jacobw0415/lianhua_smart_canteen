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

import org.springdoc.core.annotations.ParameterObject;
import org.springdoc.core.converters.models.PageableAsQueryParam;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 訂單客戶管理 API
 */
@RestController
@RequestMapping("/api/order_customers")
@Tag(name = "訂單客戶管理", description = "Order Customer Management API")
@RequiredArgsConstructor
public class OrderCustomerController {

    private final OrderCustomerService service;

    // ============================================================
    //  分頁取得所有訂單客戶（比照 SalesController）
    // ============================================================
    @Operation(
            summary = "分頁取得訂單客戶清單",
            description = """
                    支援 page / size / sort，自動與 React-Admin 分頁整合。
                    例如：
                    /api/order-customers?page=0&size=10&sort=id,asc
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功取得訂單客戶列表"),
            @ApiResponse(responseCode = "500", description = "伺服器錯誤")
    })
    @PageableAsQueryParam
    @GetMapping
    public ResponseEntity<ApiResponseDto<Page<OrderCustomerResponseDto>>> getAllCustomers(
            @ParameterObject Pageable pageable
    ) {
        Page<OrderCustomerResponseDto> page = service.page(pageable);
        return ResponseEntity.ok(ApiResponseDto.ok(page));
    }

    // ============================================================
    // 單筆查詢
    // ============================================================
    @Operation(summary = "取得指定訂單客戶")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "成功取得訂單客戶資料",
                    content = @Content(schema = @Schema(implementation = OrderCustomerResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "找不到訂單客戶",
                    content = @Content(schema = @Schema(implementation = NotFoundResponse.class))
            )
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDto<OrderCustomerResponseDto>> getCustomerById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                ApiResponseDto.ok(service.findById(id))
        );
    }

    // ============================================================
    // 建立訂單客戶
    // ============================================================
    @Operation(summary = "新增訂單客戶")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "成功建立訂單客戶",
                    content = @Content(schema = @Schema(implementation = OrderCustomerResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "參數錯誤",
                    content = @Content(schema = @Schema(implementation = BadRequestResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "客戶名稱重複或資料衝突",
                    content = @Content(schema = @Schema(implementation = ConflictResponse.class))
            )
    })
    @PostMapping
    public ResponseEntity<ApiResponseDto<OrderCustomerResponseDto>> createCustomer(
            @Valid @RequestBody OrderCustomerRequestDto dto
    ) {
        OrderCustomerResponseDto created = service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.created(created));
    }

    // ============================================================
    // 更新訂單客戶
    // ============================================================
    @Operation(summary = "更新訂單客戶資料")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "成功更新訂單客戶",
                    content = @Content(schema = @Schema(implementation = OrderCustomerResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "參數錯誤",
                    content = @Content(schema = @Schema(implementation = BadRequestResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "找不到訂單客戶",
                    content = @Content(schema = @Schema(implementation = NotFoundResponse.class))
            )
    })
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseDto<OrderCustomerResponseDto>> updateCustomer(
            @PathVariable Long id,
            @Valid @RequestBody OrderCustomerRequestDto dto
    ) {
        return ResponseEntity.ok(
                ApiResponseDto.ok(service.update(id, dto))
        );
    }

    // ============================================================
    // 刪除訂單客戶
    // ============================================================
    @Operation(summary = "刪除訂單客戶")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "成功刪除訂單客戶"),
            @ApiResponse(
                    responseCode = "404",
                    description = "找不到訂單客戶",
                    content = @Content(schema = @Schema(implementation = NotFoundResponse.class))
            )
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCustomer(@PathVariable Long id) {
        service.delete(id);
    }

    // ============================================================
    // 🔍 搜尋訂單客戶（分頁 + 模糊搜尋）
    // ============================================================
    @Operation(
            summary = "搜尋訂單客戶（支援分頁 + 模糊搜尋）",
            description = """
                    可依以下條件組合搜尋：
                    - 客戶名稱（name，模糊）
                    - 聯絡人（contactPerson，模糊）
                    - 電話（phone，模糊）
                    - 地址（address，模糊）
                    - 結帳週期（billingCycle，模糊）
                    - 備註（note，模糊）

                    與 React-Admin List / Filter 完整整合。
                    範例：
                    /api/order-customers/search?page=0&size=10&sort=id,asc&name=聯華
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "搜尋成功",
                    content = @Content(schema = @Schema(implementation = OrderCustomerResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "搜尋條件錯誤",
                    content = @Content(schema = @Schema(implementation = BadRequestResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "無符合條件的訂單客戶",
                    content = @Content(schema = @Schema(implementation = NotFoundResponse.class))
            )
    })
    @PageableAsQueryParam
    @GetMapping("/search")
    public ResponseEntity<ApiResponseDto<Page<OrderCustomerResponseDto>>> searchCustomers(
            @ParameterObject @ModelAttribute OrderCustomerRequestDto request,
            @ParameterObject Pageable pageable
    ) {
        Page<OrderCustomerResponseDto> page = service.search(request, pageable);
        return ResponseEntity.ok(ApiResponseDto.ok(page));
    }
}
