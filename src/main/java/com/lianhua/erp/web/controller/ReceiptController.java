package com.lianhua.erp.web.controller;

import com.lianhua.erp.dto.apiResponse.ApiResponseDto;
import com.lianhua.erp.dto.export.ExportPayload;
import com.lianhua.erp.dto.error.BadRequestResponse;
import com.lianhua.erp.dto.error.ConflictResponse;
import com.lianhua.erp.dto.error.InternalServerErrorResponse;
import com.lianhua.erp.dto.error.NotFoundResponse;
import com.lianhua.erp.dto.receipt.*;
import com.lianhua.erp.export.ExportFormat;
import com.lianhua.erp.export.ExportScope;
import com.lianhua.erp.service.ReceiptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springdoc.core.converters.models.PageableAsQueryParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
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
    @PreAuthorize("hasAuthority('sale:edit')")
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
    @PreAuthorize("hasAuthority('sale:edit')")
    public ResponseEntity<ApiResponseDto<ReceiptResponseDto>> update(
            @PathVariable Long id,
            @Valid @RequestBody ReceiptRequestDto dto) {
        return ResponseEntity.ok(ApiResponseDto.ok(service.update(id, dto)));
    }
    
    // ------------------------------------------------------
    // 3️⃣ 查詢全部收款（分頁版）
    // ------------------------------------------------------
    @Operation(
            summary = "分頁取得收款清單",
            description = """
                    支援 page / size / sort，自動與 React-Admin 分頁整合。
                    例如：/api/receipts?page=0&size=10&sort=id,asc
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功取得收款列表"),
            @ApiResponse(responseCode = "500", description = "伺服器錯誤")
    })
    @PageableAsQueryParam
    @GetMapping
    @PreAuthorize("hasAuthority('receipt:view')")
    public ResponseEntity<ApiResponseDto<Page<ReceiptResponseDto>>> getAll(
            @ParameterObject Pageable pageable
    ) {
        Page<ReceiptResponseDto> page = service.findAll(pageable);
        return ResponseEntity.ok(ApiResponseDto.ok(page));
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
    @PreAuthorize("hasAuthority('receipt:view')")
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
    @PreAuthorize("hasAuthority('receipt:view')")
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
    @PreAuthorize("hasAuthority('sale:edit')")
    public ResponseEntity<ApiResponseDto<Void>> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponseDto.ok(null));
    }

    // ------------------------------------------------------
    // 7️⃣ 作廢收款單
    // ------------------------------------------------------
    @Operation(
            summary = "作廢收款單",
            description = "將收款單標記為作廢。作廢後會重新計算訂單的付款狀態。任何狀態的收款單都可以作廢。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "作廢成功"),
            @ApiResponse(responseCode = "400", description = "收款單已經作廢",
                    content = @Content(schema = @Schema(implementation = BadRequestResponse.class))),
            @ApiResponse(responseCode = "404", description = "找不到收款單",
                    content = @Content(schema = @Schema(implementation = NotFoundResponse.class))),
            @ApiResponse(responseCode = "500", description = "伺服器錯誤",
                    content = @Content(schema = @Schema(implementation = InternalServerErrorResponse.class)))
    })
    @PostMapping("/{id}/void")
    @PreAuthorize("hasAuthority('sale:edit')")
    public ResponseEntity<ApiResponseDto<ReceiptResponseDto>> voidReceipt(
            @PathVariable Long id,
            @RequestBody(required = false) java.util.Map<String, String> request) {
        
        String reason = request != null ? request.get("reason") : null;
        ReceiptResponseDto result = service.voidReceipt(id, reason);
        return ResponseEntity.ok(ApiResponseDto.ok(result));
    }

    /* ============================================================
     * 📌 收款紀錄搜尋（支援模糊搜尋 + 分頁 + 動態條件）
     * ============================================================ */
    @Operation(
            summary = "搜尋收款紀錄（支援模糊搜尋與分頁）",
            description = """
                可依客戶名稱、訂單編號、收款方式、會計期間、收款日期區間進行搜尋。
                支援 page / size / sort，自動整合 React-Admin 查詢方式。
                查詢示例：
                  /api/receipts/search?filter={...}&page=0&size=10&sort=receivedDate,desc
                """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "搜尋成功"),
            @ApiResponse(responseCode = "400", description = "搜尋條件無效"),
            @ApiResponse(responseCode = "500", description = "伺服器錯誤")
    })
    @PageableAsQueryParam
    @GetMapping("/search")
    @PreAuthorize("hasAuthority('receipt:view')")
    public ResponseEntity<ApiResponseDto<Page<ReceiptResponseDto>>> searchReceipts(
            @ParameterObject ReceiptSearchRequest req,
            @ParameterObject Pageable pageable
    ) {
        Page<ReceiptResponseDto> page = service.searchReceipts(req, pageable);
        return ResponseEntity.ok(ApiResponseDto.ok(page));
    }

    // ------------------------------------------------------
    // ✅ 匯出收款紀錄（與 /search 相同條件）
    // ------------------------------------------------------
    @Operation(
            summary = "匯出收款紀錄",
            description = """
                篩選條件與 GET /api/receipts/search 相同。
                - scope=page（預設）：匯出目前列表分頁。
                - scope=all：匯出全部符合條件資料（受 app.export.max-rows 限制）。
                - format：xlsx（預設）或 csv。
                """
    )
    @PageableAsQueryParam
    @GetMapping(value = "/export", produces = {
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "text/csv; charset=UTF-8"
    })
    @PreAuthorize("hasAuthority('receipt:view')")
    public ResponseEntity<byte[]> exportReceipts(
            @ParameterObject ReceiptSearchRequest req,
            @ParameterObject Pageable pageable,
            @RequestParam(required = false) String format,
            @RequestParam(required = false) String scope
    ) {
        ExportPayload payload = service.exportReceipts(
                req,
                pageable,
                ExportFormat.fromQueryParam(format),
                ExportScope.fromQueryParam(scope)
        );

        ContentDisposition disposition = ContentDisposition.builder("attachment")
                .filename(payload.filename(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType(payload.mediaType()))
                .body(payload.data());
    }
}
