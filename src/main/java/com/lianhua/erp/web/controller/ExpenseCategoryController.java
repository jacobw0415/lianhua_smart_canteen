package com.lianhua.erp.web.controller;

import com.lianhua.erp.dto.apiResponse.ApiResponseDto;
import com.lianhua.erp.dto.error.*;
import com.lianhua.erp.dto.expense.ExpenseCategoryRequestDto;
import com.lianhua.erp.dto.expense.ExpenseCategoryResponseDto;
import com.lianhua.erp.dto.expense.ExpenseCategorySearchRequest;
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
@RequestMapping("/api/expense_categories")
@RequiredArgsConstructor
@Tag(name = "費用類別管理", description = "Expense Category Management API")
public class ExpenseCategoryController {
    
    private final ExpenseCategoryService service;
    
    // ================================
    // 查詢全部類別
    // ================================
    @GetMapping
    @Operation(summary = "查詢所有費用類別", description = "取得系統內所有費用類別（包含啟用與停用）")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功取得費用類別清單",
                    content = @Content(schema = @Schema(implementation = ExpenseCategoryResponseDto.class))),
            @ApiResponse(responseCode = "500", description = "伺服器內部錯誤",
                    content = @Content(schema = @Schema(implementation = InternalServerErrorResponse.class)))
    })
    public ResponseEntity<ApiResponseDto<List<ExpenseCategoryResponseDto>>> getAll() {
        List<ExpenseCategoryResponseDto> list = service.getAll();
        if (list.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT)
                    .body(ApiResponseDto.error(HttpStatus.NO_CONTENT.value(), "目前沒有費用類別資料"));
        }
        return ResponseEntity.ok(ApiResponseDto.ok(list));
    }
    
    // ================================
    // 查詢啟用中的類別
    // ================================
    @GetMapping("/active")
    @Operation(summary = "查詢啟用中的費用類別", description = "查詢目前可用於業務流程的費用類別（active = true）")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功取得啟用中費用類別清單",
                    content = @Content(schema = @Schema(implementation = ExpenseCategoryResponseDto.class))),
            @ApiResponse(responseCode = "204", description = "目前沒有啟用中費用類別",
                    content = @Content(schema = @Schema(implementation = NoContentResponse.class))),
            @ApiResponse(responseCode = "500", description = "伺服器內部錯誤",
                    content = @Content(schema = @Schema(implementation = InternalServerErrorResponse.class)))
    })
    public ResponseEntity<ApiResponseDto<List<ExpenseCategoryResponseDto>>> getActive() {
        List<ExpenseCategoryResponseDto> list = service.getActive();
        if (list.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT)
                    .body(ApiResponseDto.error(HttpStatus.NO_CONTENT.value(), "目前沒有啟用中費用類別"));
        }
        return ResponseEntity.ok(ApiResponseDto.ok(list));
    }
    
    // ================================
    // 查詢單筆類別
    // ================================
    @GetMapping("/{id}")
    @Operation(summary = "查詢指定費用類別", description = "根據 ID 取得單一費用類別詳細資訊")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功取得費用類別",
                    content = @Content(schema = @Schema(implementation = ExpenseCategoryResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "找不到指定費用類別",
                    content = @Content(schema = @Schema(implementation = NotFoundResponse.class))),
            @ApiResponse(responseCode = "500", description = "伺服器內部錯誤",
                    content = @Content(schema = @Schema(implementation = InternalServerErrorResponse.class)))
    })
    public ResponseEntity<ApiResponseDto<ExpenseCategoryResponseDto>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponseDto.ok(service.getById(id)));
    }
    
    // ================================
    // 新增類別
    // ================================
    @PostMapping
    @Operation(summary = "新增費用類別")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "成功新增費用類別",
                    content = @Content(schema = @Schema(implementation = ExpenseCategoryResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "輸入參數錯誤",
                    content = @Content(schema = @Schema(implementation = BadRequestResponse.class))),
            @ApiResponse(responseCode = "409", description = "資料重複或違反唯一約束",
                    content = @Content(schema = @Schema(implementation = ConflictResponse.class))),
            @ApiResponse(responseCode = "500", description = "伺服器內部錯誤",
                    content = @Content(schema = @Schema(implementation = InternalServerErrorResponse.class)))
    })
    public ResponseEntity<ApiResponseDto<ExpenseCategoryResponseDto>> create(
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
                    content = @Content(schema = @Schema(implementation = ExpenseCategoryResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "輸入參數錯誤",
                    content = @Content(schema = @Schema(implementation = BadRequestResponse.class))),
            @ApiResponse(responseCode = "404", description = "找不到費用類別",
                    content = @Content(schema = @Schema(implementation = NotFoundResponse.class))),
            @ApiResponse(responseCode = "409", description = "資料重複或違反唯一約束",
                    content = @Content(schema = @Schema(implementation = ConflictResponse.class))),
            @ApiResponse(responseCode = "500", description = "伺服器內部錯誤",
                    content = @Content(schema = @Schema(implementation = InternalServerErrorResponse.class)))
    })
    public ResponseEntity<ApiResponseDto<ExpenseCategoryResponseDto>> update(
            @PathVariable Long id, @Valid @RequestBody ExpenseCategoryRequestDto dto) {
        return ResponseEntity.ok(ApiResponseDto.ok(service.update(id, dto)));
    }
    
    // ================================
    // 啟用類別
    // ================================
    @PutMapping("/{id}/activate")
    @Operation(summary = "啟用費用類別", description = "將指定費用類別重新設為啟用狀態，使其可再次用於業務流程。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功啟用費用類別",
                    content = @Content(schema = @Schema(implementation = ExpenseCategoryResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "找不到費用類別",
                    content = @Content(schema = @Schema(implementation = NotFoundResponse.class))),
            @ApiResponse(responseCode = "500", description = "伺服器內部錯誤",
                    content = @Content(schema = @Schema(implementation = InternalServerErrorResponse.class)))
    })
    public ResponseEntity<ApiResponseDto<ExpenseCategoryResponseDto>> activate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponseDto.ok(service.activate(id)));
    }
    
    // ================================
    // 停用類別
    // ================================
    @PutMapping("/{id}/deactivate")
    @Operation(summary = "停用費用類別", description = "將指定費用類別設為停用，停用後該類別不可用於新增費用記錄。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功停用費用類別",
                    content = @Content(schema = @Schema(implementation = ExpenseCategoryResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "輸入參數錯誤",
                    content = @Content(schema = @Schema(implementation = BadRequestResponse.class))),
            @ApiResponse(responseCode = "404", description = "找不到費用類別",
                    content = @Content(schema = @Schema(implementation = NotFoundResponse.class))),
            @ApiResponse(responseCode = "500", description = "伺服器內部錯誤",
                    content = @Content(schema = @Schema(implementation = InternalServerErrorResponse.class)))
    })
    public ResponseEntity<ApiResponseDto<ExpenseCategoryResponseDto>> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponseDto.ok(service.deactivate(id)));
    }
    
    // ================================
    // 搜尋類別
    // ================================
    @GetMapping("/search")
    @Operation(summary = "搜尋費用類別", description = "依類別名稱、會計科目代碼、啟用狀態進行搜尋，未提供條件時回傳全部類別。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "搜尋成功",
                    content = @Content(schema = @Schema(implementation = ExpenseCategoryResponseDto.class))),
            @ApiResponse(responseCode = "204", description = "查無符合條件的費用類別資料",
                    content = @Content(schema = @Schema(implementation = NoContentResponse.class))),
            @ApiResponse(responseCode = "500", description = "伺服器內部錯誤",
                    content = @Content(schema = @Schema(implementation = InternalServerErrorResponse.class)))
    })
    public ResponseEntity<ApiResponseDto<List<ExpenseCategoryResponseDto>>> search(
            ExpenseCategorySearchRequest search) {
        
        List<ExpenseCategoryResponseDto> list = service.search(search);
        
        if (list.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT)
                    .body(ApiResponseDto.error(
                            HttpStatus.NO_CONTENT.value(),
                            "查無符合條件的費用類別資料"
                    ));
        }
        
        return ResponseEntity.ok(ApiResponseDto.ok(list));
    }
    
    // ================================
    // 刪除類別
    // ================================
    @DeleteMapping("/{id}")
    @Operation(summary = "刪除費用類別", description = "依類別 ID 刪除費用類別，若類別已被費用記錄引用則無法刪除。")
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
