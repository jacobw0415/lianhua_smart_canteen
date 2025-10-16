package com.lianhua.erp.web.controller;

import com.lianhua.erp.dto.apiResponse.ApiResponseDto;
import com.lianhua.erp.dto.error.InternalServerErrorResponse;
import com.lianhua.erp.dto.error.NotFoundResponse;
import com.lianhua.erp.dto.payment.PaymentResponseDto;
import com.lianhua.erp.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "取得所有付款紀錄", description = "回傳所有付款紀錄清單")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查詢成功",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = PaymentResponseDto.class)))),
            @ApiResponse(responseCode = "500", description = "伺服器內部錯誤",
                    content = @Content(schema = @Schema(implementation = InternalServerErrorResponse.class)))
    })
    @GetMapping
    public List<PaymentResponseDto> findAll() {
        return paymentService.findAll();
    }

    @Operation(summary = "依進貨單 ID 取得付款紀錄", description = "輸入 purchaseId 取得對應付款資料")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查詢成功",
                    content = @Content(schema = @Schema(implementation = PaymentResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "找不到對應的付款紀錄",
                    content = @Content(schema = @Schema(implementation = NotFoundResponse.class))),
            @ApiResponse(responseCode = "500", description = "伺服器內部錯誤",
                    content = @Content(schema = @Schema(implementation = InternalServerErrorResponse.class)))
    })
    @GetMapping("/{purchaseId}")
    public PaymentResponseDto findByPurchase(@PathVariable Long purchaseId) {
        return paymentService.findByPurchaseId(purchaseId);
    }

    @Operation(summary = "刪除付款紀錄", description = "依進貨單 ID 刪除對應的付款紀錄")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "刪除成功",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "找不到要刪除的紀錄",
                    content = @Content(schema = @Schema(implementation = NotFoundResponse.class))),
            @ApiResponse(responseCode = "500", description = "伺服器內部錯誤",
                    content = @Content(schema = @Schema(implementation = InternalServerErrorResponse.class)))
    })
    @DeleteMapping("/{purchaseId}")
    public void deleteByPurchase(@PathVariable Long purchaseId) {
        paymentService.deleteByPurchaseId(purchaseId);
    }
}
