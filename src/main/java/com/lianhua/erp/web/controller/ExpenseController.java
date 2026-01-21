package com.lianhua.erp.web.controller;

import com.lianhua.erp.dto.apiResponse.ApiResponseDto;
import com.lianhua.erp.dto.error.*;
import com.lianhua.erp.dto.expense.ExpenseDto;
import com.lianhua.erp.dto.expense.ExpenseRequestDto;
import com.lianhua.erp.dto.expense.ExpenseSearchRequest;
import com.lianhua.erp.service.ExpenseService;
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

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
@Tag(name = "費用管理", description = "Expenses Management API")
public class ExpenseController {
    
    private final ExpenseService service;
    
    // ================================
    // 查詢所有支出（分頁）
    // ================================
    @Operation(
            summary = "取得支出紀錄列表（分頁）",
            description = "支援 page / size / sort，自動與 React-Admin 分頁整合"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功取得支出紀錄分頁資料",
                    content = @Content(schema = @Schema(implementation = ExpenseDto.class))),
            @ApiResponse(responseCode = "500", description = "伺服器內部錯誤",
                    content = @Content(schema = @Schema(implementation = InternalServerErrorResponse.class)))
    })
    @PageableAsQueryParam
    @GetMapping
    public ResponseEntity<ApiResponseDto<Page<ExpenseDto>>> getAll(
            @ParameterObject Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponseDto.ok(service.findAll(pageable)));
    }
    
    // ================================
    // 查詢單筆支出
    // ================================
    @Operation(summary = "查詢單筆支出紀錄")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功取得支出紀錄",
                    content = @Content(schema = @Schema(implementation = ExpenseDto.class))),
            @ApiResponse(responseCode = "404", description = "找不到指定支出紀錄",
                    content = @Content(schema = @Schema(implementation = NotFoundResponse.class))),
            @ApiResponse(responseCode = "500", description = "伺服器內部錯誤",
                    content = @Content(schema = @Schema(implementation = InternalServerErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDto<ExpenseDto>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponseDto.ok(service.findById(id)));
    }
    
    // ================================
    // 新增支出
    // ================================
    @Operation(summary = "新增支出紀錄")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "成功新增支出紀錄",
                    content = @Content(schema = @Schema(implementation = ExpenseDto.class))),
            @ApiResponse(responseCode = "400", description = "參數格式錯誤",
                    content = @Content(schema = @Schema(implementation = BadRequestResponse.class))),
            @ApiResponse(responseCode = "409", description = "資料重複或違反唯一約束",
                    content = @Content(schema = @Schema(implementation = ConflictResponse.class))),
            @ApiResponse(responseCode = "500", description = "伺服器內部錯誤",
                    content = @Content(schema = @Schema(implementation = InternalServerErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<ApiResponseDto<ExpenseDto>> create(@Valid @RequestBody ExpenseRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponseDto.ok(service.create(dto)));
    }
    
    // ================================
    // 更新支出
    // ================================
    @Operation(summary = "更新支出紀錄")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功更新支出紀錄",
                    content = @Content(schema = @Schema(implementation = ExpenseDto.class))),
            @ApiResponse(responseCode = "400", description = "輸入參數錯誤",
                    content = @Content(schema = @Schema(implementation = BadRequestResponse.class))),
            @ApiResponse(responseCode = "404", description = "找不到支出紀錄",
                    content = @Content(schema = @Schema(implementation = NotFoundResponse.class))),
            @ApiResponse(responseCode = "409", description = "違反唯一約束條件",
                    content = @Content(schema = @Schema(implementation = ConflictResponse.class))),
            @ApiResponse(responseCode = "500", description = "伺服器內部錯誤",
                    content = @Content(schema = @Schema(implementation = InternalServerErrorResponse.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseDto<ExpenseDto>> update(
            @PathVariable Long id, @Valid @RequestBody ExpenseRequestDto dto) {
        return ResponseEntity.ok(ApiResponseDto.ok(service.update(id, dto)));
    }
    
    // ================================
    // 搜尋支出
    // ================================
    @Operation(
            summary = "搜尋費用支出（支援模糊搜尋與分頁）",
            description = """
                    可依以下條件組合搜尋：
                    - 費用類別名稱（categoryName, 模糊）
                    - 費用類別 ID（categoryId, 精準）
                    - 員工名稱（employeeName, 模糊）
                    - 員工 ID（employeeId, 精準）
                    - 會計期間（accountingPeriod, 精準 YYYY-MM）
                    - 支出日期範圍（fromDate, toDate）
                    - 備註（note, 模糊）
                    
                    與 React-Admin 的 List / Filter 完整整合。
                    範例：
                    /api/expenses/search?page=0&size=10&sort=expenseDate,desc&categoryName=食材&fromDate=2025-01-01
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "搜尋成功",
                    content = @Content(schema = @Schema(implementation = ExpenseDto.class))),
            @ApiResponse(responseCode = "400", description = "搜尋條件錯誤",
                    content = @Content(schema = @Schema(implementation = BadRequestResponse.class))),
            @ApiResponse(responseCode = "500", description = "伺服器內部錯誤",
                    content = @Content(schema = @Schema(implementation = InternalServerErrorResponse.class)))
    })
    @PageableAsQueryParam
    @GetMapping("/search")
    public ResponseEntity<ApiResponseDto<Page<ExpenseDto>>> searchExpenses(
            @ParameterObject ExpenseSearchRequest req,
            @ParameterObject Pageable pageable
    ) {
        Page<ExpenseDto> page = service.searchExpenses(req, pageable);
        return ResponseEntity.ok(ApiResponseDto.ok(page));
    }
    
    // ================================
    // 刪除支出
    // ================================
    @Operation(
            summary = "作廢支出記錄",
            description = "將支出記錄標記為作廢。作廢後的記錄不會參與會計計算（現金流量表、利潤表等），但會保留歷史記錄以供審計追蹤。任何狀態的支出記錄都可以作廢。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "作廢成功"),
            @ApiResponse(responseCode = "400", description = "支出記錄已經作廢",
                    content = @Content(schema = @Schema(implementation = BadRequestResponse.class))),
            @ApiResponse(responseCode = "404", description = "找不到支出記錄",
                    content = @Content(schema = @Schema(implementation = NotFoundResponse.class))),
            @ApiResponse(responseCode = "500", description = "伺服器錯誤",
                    content = @Content(schema = @Schema(implementation = InternalServerErrorResponse.class)))
    })
    @PostMapping("/{id}/void")
    public ResponseEntity<ApiResponseDto<ExpenseDto>> voidExpense(
            @PathVariable Long id,
            @RequestBody(required = false) java.util.Map<String, String> request) {
        
        String reason = request != null ? request.get("reason") : null;
        ExpenseDto result = service.voidExpense(id, reason);
        return ResponseEntity.ok(ApiResponseDto.ok(result));
    }

    // ================================
    // ❌ 禁止直接刪除支出 (回拋友善訊息)
    // ================================
    @Operation(
            summary = "刪除支出紀錄 (禁止使用)",
            description = "根據財務審計規範，支出紀錄不開放直接刪除。請改用 /void 接口進行作廢操作。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "405", description = "不允許刪除操作",
                    content = @Content(schema = @Schema(implementation = BadRequestResponse.class)))
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public void delete(@PathVariable Long id) {

        // 拋出 Method Not Allowed 並附帶自定義訊息
        // 前端 React-admin 會捕捉此訊息並顯示在通知列
        throw new org.springframework.web.server.ResponseStatusException(
                HttpStatus.METHOD_NOT_ALLOWED,
                "支出紀錄具有審計意義，不可直接刪除。若紀錄輸入有誤，請點選「作廢」按鈕，再重新建立新紀錄。"
        );
    }
}
