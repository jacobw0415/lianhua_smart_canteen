package com.lianhua.erp.web.controller;

import com.lianhua.erp.dto.apiResponse.ApiResponseDto;
import com.lianhua.erp.dto.error.*;
import com.lianhua.erp.dto.expense.ExpenseCategoryDto;
import com.lianhua.erp.dto.expense.ExpenseCategoryRequestDto;
import com.lianhua.erp.service.ExpenseCategoryService;
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
@RequestMapping("/api/expense-categories")
@RequiredArgsConstructor
@Tag(name = "費用類別管理", description = "Expense Category Management API")
public class ExpenseCategoryController {
    
    private final ExpenseCategoryService service;
    
    // ================================
    // 查詢全部類別
    // ================================
    @GetMapping
    @Operation(summary = "查詢所有費用類別", description = "可選擇是否僅顯示啟用中項目")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功取得費用類別清單",
                    content = @Content(schema = @Schema(implementation = ExpenseCategoryDto.class))),
            @ApiResponse(responseCode = "500", description = "伺服器內部錯誤",
                    content = @Content(schema = @Schema(implementation = InternalServerErrorResponse.class)))
    })
    public ResponseEntity<ApiResponseDto<List<ExpenseCategoryDto>>> findAll(
            @RequestParam(defaultValue = "false") boolean activeOnly) {
        return ResponseEntity.ok(ApiResponseDto.ok(service.findAll(activeOnly)));
    }
    
    // ================================
    // 查詢單筆類別
    // ================================
    @GetMapping("/{id}")
    @Operation(summary = "查詢指定費用類別", description = "根據 ID 取得單一費用類別詳細資訊")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功取得費用類別",
                    content = @Content(schema = @Schema(implementation = ExpenseCategoryDto.class))),
            @ApiResponse(responseCode = "404", description = "找不到指定費用類別",
                    content = @Content(schema = @Schema(implementation = NotFoundResponse.class))),
            @ApiResponse(responseCode = "500", description = "伺服器內部錯誤",
                    content = @Content(schema = @Schema(implementation = InternalServerErrorResponse.class)))
    })
    public ResponseEntity<ApiResponseDto<ExpenseCategoryDto>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponseDto.ok(service.findById(id)));
    }
    
    // ================================
    // 新增類別
    // ================================
    @PostMapping
    @Operation(summary = "新增費用類別")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "成功新增費用類別",
                    content = @Content(schema = @Schema(implementation = ExpenseCategoryDto.class))),
            @ApiResponse(responseCode = "400", description = "輸入參數錯誤",
                    content = @Content(schema = @Schema(implementation = BadRequestResponse.class))),
            @ApiResponse(responseCode = "409", description = "資料重複或違反唯一約束",
                    content = @Content(schema = @Schema(implementation = ConflictResponse.class))),
            @ApiResponse(responseCode = "500", description = "伺服器內部錯誤",
                    content = @Content(schema = @Schema(implementation = InternalServerErrorResponse.class)))
    })
    public ResponseEntity<ApiResponseDto<ExpenseCategoryDto>> create(
            @Valid @RequestBody ExpenseCategoryRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.ok(service.create(dto)));
    }
    
    // ================================
    // 更新類別
    // ================================
    @PutMapping("/{id}")
    @Operation(summary = "更新費用類別")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功更新費用類別",
                    content = @Content(schema = @Schema(implementation = ExpenseCategoryDto.class))),
            @ApiResponse(responseCode = "400", description = "輸入參數錯誤",
                    content = @Content(schema = @Schema(implementation = BadRequestResponse.class))),
            @ApiResponse(responseCode = "404", description = "找不到費用類別",
                    content = @Content(schema = @Schema(implementation = NotFoundResponse.class))),
            @ApiResponse(responseCode = "409", description = "資料重複或違反唯一約束",
                    content = @Content(schema = @Schema(implementation = ConflictResponse.class))),
            @ApiResponse(responseCode = "500", description = "伺服器內部錯誤",
                    content = @Content(schema = @Schema(implementation = InternalServerErrorResponse.class)))
    })
    public ResponseEntity<ApiResponseDto<ExpenseCategoryDto>> update(
            @PathVariable Long id, @Valid @RequestBody ExpenseCategoryRequestDto dto) {
        return ResponseEntity.ok(ApiResponseDto.ok(service.update(id, dto)));
    }
    
    // ================================
    // 刪除類別
    // ================================
    @DeleteMapping("/{id}")
    @Operation(summary = "刪除費用類別")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "成功刪除"),
            @ApiResponse(responseCode = "404", description = "找不到費用類別",
                    content = @Content(schema = @Schema(implementation = NotFoundResponse.class))),
            @ApiResponse(responseCode = "409", description = "刪除失敗，資料被其他表參照",
                    content = @Content(schema = @Schema(implementation = ConflictResponse.class))),
            @ApiResponse(responseCode = "500", description = "伺服器內部錯誤",
                    content = @Content(schema = @Schema(implementation = InternalServerErrorResponse.class)))
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
