package com.lianhua.erp.web.controller;

import com.lianhua.erp.dto.apiResponse.ApiResponseDto;
import com.lianhua.erp.dto.error.ConflictResponse;
import com.lianhua.erp.dto.error.InternalServerErrorResponse;
import com.lianhua.erp.dto.error.NotFoundResponse;
import com.lianhua.erp.dto.receipt.*;
import com.lianhua.erp.service.ReceiptService;
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
@RequestMapping("/api/receipts")
@RequiredArgsConstructor
@Tag(name = "收款管理", description = "收款記錄維護與查詢 API（收款金額自動帶入訂單總額）")
public class ReceiptController {
    
    private final ReceiptService service;
    
    // ------------------------------------------------------
    // 1️⃣ 建立收款記錄（自動帶入訂單金額）
    // ------------------------------------------------------
    @Operation(
            summary = "建立新收款記錄",
            description = "根據訂單 ID 自動建立收款資料，金額自動帶入訂單 total_amount，不可重複建立。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "建立成功",
                    content = @Content(schema = @Schema(implementation = ReceiptResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "請求參數錯誤"),
            @ApiResponse(responseCode = "404", description = "找不到指定訂單 ID",
                    content = @Content(schema = @Schema(implementation = NotFoundResponse.class))),
            @ApiResponse(responseCode = "409", description = "該訂單已存在收款記錄",
                    content = @Content(schema = @Schema(implementation = ConflictResponse.class))),
            @ApiResponse(responseCode = "500", description = "伺服器錯誤",
                    content = @Content(schema = @Schema(implementation = InternalServerErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<ApiResponseDto<ReceiptResponseDto>> create(@Valid @RequestBody ReceiptRequestDto dto) {
        ReceiptResponseDto created = service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponseDto.ok(created));
    }
    
    // ------------------------------------------------------
    // 2️⃣ 更新收款記錄（僅可修改備註與付款方式）
    // ------------------------------------------------------
    @Operation(
            summary = "更新收款資料",
            description = "僅可更新付款方式、參考號碼、備註等欄位，金額不可修改。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "404", description = "找不到指定收款 ID",
                    content = @Content(schema = @Schema(implementation = NotFoundResponse.class))),
            @ApiResponse(responseCode = "500", description = "伺服器錯誤")
    })
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseDto<ReceiptResponseDto>> update(
            @PathVariable Long id,
            @Valid @RequestBody ReceiptRequestDto dto) {
        return ResponseEntity.ok(ApiResponseDto.ok(service.update(id, dto)));
    }
    
    // ------------------------------------------------------
    // 3️⃣ 查詢全部收款
    // ------------------------------------------------------
    @Operation(
            summary = "取得所有收款清單",
            description = "查詢所有收款資料，包含自動帶入的金額與會計期間。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查詢成功",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ReceiptResponseDto.class)))),
            @ApiResponse(responseCode = "500", description = "伺服器錯誤")
    })
    @GetMapping
    public ResponseEntity<ApiResponseDto<List<ReceiptResponseDto>>> getAll() {
        return ResponseEntity.ok(ApiResponseDto.ok(service.findAll()));
    }
    
    // ------------------------------------------------------
    // 4️⃣ 依收款 ID 查詢
    // ------------------------------------------------------
    @Operation(
            summary = "依收款 ID 取得資料",
            description = "傳入收款 ID 取得完整資料（含訂單 ID 與自動帶入金額）。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查詢成功"),
            @ApiResponse(responseCode = "404", description = "找不到指定收款 ID",
                    content = @Content(schema = @Schema(implementation = NotFoundResponse.class))),
            @ApiResponse(responseCode = "500", description = "伺服器錯誤")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDto<ReceiptResponseDto>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponseDto.ok(service.findById(id)));
    }
    
    // ------------------------------------------------------
    // 5️⃣ 依訂單 ID 查詢收款
    // ------------------------------------------------------
    @Operation(
            summary = "依訂單 ID 查詢收款資料",
            description = "傳入訂單 ID，查詢該訂單的收款紀錄。若未收款則回傳 204。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查詢成功"),
            @ApiResponse(responseCode = "204", description = "尚未建立收款資料"),
            @ApiResponse(responseCode = "404", description = "找不到訂單 ID"),
            @ApiResponse(responseCode = "500", description = "伺服器錯誤")
    })
    @GetMapping("/order/{orderId}")
    public ResponseEntity<ApiResponseDto<List<ReceiptResponseDto>>> getByOrder(@PathVariable Long orderId) {
        List<ReceiptResponseDto> list = service.findByOrderId(orderId);
        if (list.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT)
                    .body(ApiResponseDto.error(HttpStatus.NO_CONTENT.value(), "該訂單目前無收款記錄"));
        }
        return ResponseEntity.ok(ApiResponseDto.ok(list));
    }
    
    // ------------------------------------------------------
    // 6️⃣ 刪除收款
    // ------------------------------------------------------
    @Operation(
            summary = "刪除收款記錄",
            description = "根據收款 ID 刪除指定資料，若該訂單已有入帳關聯則不允許刪除。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "刪除成功"),
            @ApiResponse(responseCode = "404", description = "找不到收款記錄",
                    content = @Content(schema = @Schema(implementation = NotFoundResponse.class))),
            @ApiResponse(responseCode = "409", description = "刪除失敗：存在訂單關聯或資料完整性問題",
                    content = @Content(schema = @Schema(implementation = ConflictResponse.class))),
            @ApiResponse(responseCode = "500", description = "伺服器錯誤",
                    content = @Content(schema = @Schema(implementation = InternalServerErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseDto<Void>> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponseDto.ok(null));
    }
}
