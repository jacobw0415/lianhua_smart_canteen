package com.lianhua.erp.web.controller;

import com.lianhua.erp.dto.apiResponse.ApiResponseDto;
import com.lianhua.erp.dto.employee.*;
import com.lianhua.erp.dto.error.*;
import com.lianhua.erp.service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
@Tag(name = "員工管理", description = "Employee Management API")
public class EmployeeController {
    
    private final EmployeeService service;
    
    // ================================
    // 查詢全部員工
    // ================================
    @GetMapping
    @Operation(summary = "查詢所有員工", description = "取得系統內所有員工資料")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功取得員工清單",
                    content = @Content(schema = @Schema(implementation = EmployeeResponseDto.class))),
            @ApiResponse(responseCode = "500", description = "伺服器內部錯誤",
                    content = @Content(schema = @Schema(implementation = InternalServerErrorResponse.class)))
    })
    public ResponseEntity<ApiResponseDto<List<EmployeeResponseDto>>> findAll() {
        return ResponseEntity.ok(ApiResponseDto.ok(service.findAll()));
    }
    
    // ================================
    // 查詢單筆員工
    // ================================
    @GetMapping("/{id}")
    @Operation(summary = "查詢指定員工", description = "根據 ID 取得單一員工詳細資料")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功取得員工資料",
                    content = @Content(schema = @Schema(implementation = EmployeeResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "找不到指定員工",
                    content = @Content(schema = @Schema(implementation = NotFoundResponse.class))),
            @ApiResponse(responseCode = "500", description = "伺服器內部錯誤",
                    content = @Content(schema = @Schema(implementation = InternalServerErrorResponse.class)))
    })
    public ResponseEntity<ApiResponseDto<EmployeeResponseDto>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponseDto.ok(service.findById(id)));
    }
    
    // ================================
    // 新增員工
    // ================================
    @PostMapping
    @Operation(summary = "新增員工")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "成功新增員工",
                    content = @Content(schema = @Schema(implementation = EmployeeResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "輸入參數錯誤",
                    content = @Content(schema = @Schema(implementation = BadRequestResponse.class))),
            @ApiResponse(responseCode = "409", description = "資料重複或違反唯一約束",
                    content = @Content(schema = @Schema(implementation = ConflictResponse.class))),
            @ApiResponse(responseCode = "500", description = "伺服器內部錯誤",
                    content = @Content(schema = @Schema(implementation = InternalServerErrorResponse.class)))
    })
    public ResponseEntity<ApiResponseDto<EmployeeResponseDto>> create(
            @Valid @RequestBody EmployeeRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.ok(service.create(dto)));
    }
    
    // ================================
    // 更新員工
    // ================================
    @PutMapping("/{id}")
    @Operation(summary = "更新員工")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功更新員工資料",
                    content = @Content(schema = @Schema(implementation = EmployeeResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "輸入參數錯誤",
                    content = @Content(schema = @Schema(implementation = BadRequestResponse.class))),
            @ApiResponse(responseCode = "404", description = "找不到員工",
                    content = @Content(schema = @Schema(implementation = NotFoundResponse.class))),
            @ApiResponse(responseCode = "409", description = "資料重複或違反唯一約束",
                    content = @Content(schema = @Schema(implementation = ConflictResponse.class))),
            @ApiResponse(responseCode = "500", description = "伺服器內部錯誤",
                    content = @Content(schema = @Schema(implementation = InternalServerErrorResponse.class)))
    })
    public ResponseEntity<ApiResponseDto<EmployeeResponseDto>> update(
            @PathVariable Long id, @Valid @RequestBody EmployeeRequestDto dto) {
        return ResponseEntity.ok(ApiResponseDto.ok(service.update(id, dto)));
    }
    
    // ================================
    // 刪除員工
    // ================================
    @DeleteMapping("/{id}")
    @Operation(summary = "刪除員工")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "成功刪除"),
            @ApiResponse(responseCode = "404", description = "找不到員工",
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
