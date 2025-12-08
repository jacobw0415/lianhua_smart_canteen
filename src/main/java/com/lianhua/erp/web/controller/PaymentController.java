package com.lianhua.erp.web.controller;

import com.lianhua.erp.dto.apiResponse.ApiResponseDto;
import com.lianhua.erp.dto.error.NotFoundResponse;
import com.lianhua.erp.dto.payment.PaymentResponseDto;
import com.lianhua.erp.dto.payment.PaymentSearchRequest;
import com.lianhua.erp.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springdoc.core.converters.models.PageableAsQueryParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "付款管理", description = "付款資料維護與查詢 API")
public class PaymentController {

    private final PaymentService paymentService;


    /* ============================================================
     * 📌 付款紀錄列表（分頁版）
     * ============================================================ */
    @Operation(
            summary = "分頁取得付款紀錄清單",
            description = """
                    支援 page / size / sort，自動與 React-Admin 分頁整合。
                    例如：/api/payments?page=0&size=10&sort=id,asc
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功取得付款紀錄列表"),
            @ApiResponse(responseCode = "500", description = "伺服器錯誤")
    })
    @PageableAsQueryParam
    @GetMapping
    public ResponseEntity<ApiResponseDto<Page<PaymentResponseDto>>> getAllPayments(
            @ParameterObject Pageable pageable
    ) {
        Page<PaymentResponseDto> page = paymentService.findAll(pageable);
        return ResponseEntity.ok(ApiResponseDto.ok(page));
    }


    /* ============================================================
     * 📌 依進貨單 ID 查詢付款紀錄（沿用原邏輯）
     * ============================================================ */
    @Operation(summary = "依進貨單 ID 取得付款紀錄")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查詢成功"),
            @ApiResponse(responseCode = "404", description = "找不到對應的付款紀錄",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            schema = @Schema(implementation = NotFoundResponse.class)
                    )),
            @ApiResponse(responseCode = "500", description = "伺服器錯誤")
    })
    @GetMapping("/{purchaseId}")
    public ResponseEntity<ApiResponseDto<PaymentResponseDto>> getPaymentsByPurchase(
            @PathVariable Long purchaseId
    ) {
        PaymentResponseDto dto = paymentService.findByPurchaseId(purchaseId);
        return ResponseEntity.ok(ApiResponseDto.ok(dto));
    }


    /* ============================================================
     * 📌 刪除某進貨單底下的所有付款紀錄
     * ============================================================ */
    @Operation(summary = "刪除指定進貨單的所有付款紀錄")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "刪除成功"),
            @ApiResponse(responseCode = "404", description = "找不到要刪除的紀錄",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            schema = @Schema(implementation = NotFoundResponse.class)
                    )),
            @ApiResponse(responseCode = "500", description = "伺服器錯誤")
    })
    @DeleteMapping("/{purchaseId}")
    public ResponseEntity<ApiResponseDto<Void>> deletePaymentsByPurchase(
            @PathVariable Long purchaseId
    ) {
        paymentService.deleteByPurchaseId(purchaseId);
        return ResponseEntity.ok(ApiResponseDto.ok(null));
    }

    /* ============================================================
     * 📌 付款紀錄搜尋（支援模糊搜尋 + 分頁 + 動態條件）
     * ============================================================ */
    @Operation(
            summary = "搜尋付款紀錄（支援模糊搜尋與分頁）",
            description = """
                可依供應商名稱、品項摘要、付款方式、會計期間、付款日期區間進行搜尋。
                支援 page / size / sort，自動整合 React-Admin 查詢方式。
                查詢示例：
                  /api/payments/search?filter={...}&page=0&size=10&sort=payDate,desc
                """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "搜尋成功"),
            @ApiResponse(responseCode = "400", description = "搜尋條件無效"),
            @ApiResponse(responseCode = "500", description = "伺服器錯誤")
    })
    @PageableAsQueryParam
    @GetMapping("/search")
    public ResponseEntity<ApiResponseDto<Page<PaymentResponseDto>>> searchPayments(
            @ParameterObject PaymentSearchRequest req,
            @ParameterObject Pageable pageable
    ) {
        Page<PaymentResponseDto> page = paymentService.searchPayments(req, pageable);
        return ResponseEntity.ok(ApiResponseDto.ok(page));
    }
}
