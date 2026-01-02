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
import org.springdoc.core.annotations.ParameterObject;
import org.springdoc.core.converters.models.PageableAsQueryParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    // 查詢全部員工（分頁）
    // ================================
    @GetMapping
    @Operation(
            summary = "查詢所有員工（分頁）",
            description = "取得系統內所有員工資料，支援 page / size / sort，自動與 React-Admin 分頁整合"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功取得員工分頁資料",
                    content = @Content(schema = @Schema(implementation = EmployeeResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "分頁參數錯誤",
                    content = @Content(schema = @Schema(implementation = BadRequestResponse.class))),
            @ApiResponse(responseCode = "500", description = "伺服器內部錯誤",
                    content = @Content(schema = @Schema(implementation = InternalServerErrorResponse.class)))
    })
    @PageableAsQueryParam
    public ResponseEntity<ApiResponseDto<Page<EmployeeResponseDto>>> findAll(
            @ParameterObject Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponseDto.ok(service.findAll(pageable)));
    }
    
    // ================================
    // 查詢啟用中的員工
    // ================================
    @GetMapping("/active")
    @Operation(
            summary = "查詢啟用中的員工",
            description = "取得系統內所有啟用中（ACTIVE）的員工資料，用於下拉選單等場景"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功取得啟用中員工清單",
                    content = @Content(schema = @Schema(implementation = EmployeeResponseDto.class))),
            @ApiResponse(responseCode = "204", description = "目前沒有啟用中的員工",
                    content = @Content(schema = @Schema(implementation = NoContentResponse.class))),
            @ApiResponse(responseCode = "500", description = "伺服器內部錯誤",
                    content = @Content(schema = @Schema(implementation = InternalServerErrorResponse.class)))
    })
    public ResponseEntity<ApiResponseDto<List<EmployeeResponseDto>>> getActive() {
        List<EmployeeResponseDto> list = service.getActive();
        if (list.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT)
                    .body(ApiResponseDto.error(HttpStatus.NO_CONTENT.value(), "目前沒有啟用中的員工"));
        }
        return ResponseEntity.ok(ApiResponseDto.ok(list));
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
            @ApiResponse(responseCode = "400", description = "輸入參數錯誤（必填欄位未填寫、欄位長度超過限制、格式錯誤等）",
                    content = @Content(schema = @Schema(implementation = BadRequestResponse.class))),
            @ApiResponse(responseCode = "409", description = "員工名稱已存在",
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
            @ApiResponse(responseCode = "400", description = "輸入參數錯誤（必填欄位未填寫、欄位長度超過限制、格式錯誤等）",
                    content = @Content(schema = @Schema(implementation = BadRequestResponse.class))),
            @ApiResponse(responseCode = "404", description = "找不到員工",
                    content = @Content(schema = @Schema(implementation = NotFoundResponse.class))),
            @ApiResponse(responseCode = "409", description = "員工名稱已存在",
                    content = @Content(schema = @Schema(implementation = ConflictResponse.class))),
            @ApiResponse(responseCode = "500", description = "伺服器內部錯誤",
                    content = @Content(schema = @Schema(implementation = InternalServerErrorResponse.class)))
    })
    public ResponseEntity<ApiResponseDto<EmployeeResponseDto>> update(
            @PathVariable Long id, @Valid @RequestBody EmployeeRequestDto dto) {
        return ResponseEntity.ok(ApiResponseDto.ok(service.update(id, dto)));
    }
    
    // ================================
    // 搜尋員工（支援分頁 + 模糊搜尋）
    // ================================
    @Operation(
            summary = "搜尋員工（支援分頁 + 模糊搜尋）",
            description = """
                    可依姓名、職位、狀態搜尋。
                    支援 page / size / sort，自動整合 React-Admin 分頁。
                    
                    範例：
                    /api/employees/search?page=0&size=10&sort=fullName,asc&fullName=王&status=ACTIVE
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "搜尋成功",
                    content = @Content(schema = @Schema(implementation = EmployeeResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "搜尋條件全為空或分頁參數錯誤",
                    content = @Content(schema = @Schema(implementation = BadRequestResponse.class))),
            @ApiResponse(responseCode = "404", description = "查無匹配資料",
                    content = @Content(schema = @Schema(implementation = NotFoundResponse.class))),
            @ApiResponse(responseCode = "500", description = "伺服器內部錯誤",
                    content = @Content(schema = @Schema(implementation = InternalServerErrorResponse.class)))
    })
    @PageableAsQueryParam
    @GetMapping("/search")
    public ResponseEntity<ApiResponseDto<Page<EmployeeResponseDto>>> searchEmployees(
            @ParameterObject EmployeeSearchRequest request,
            @ParameterObject Pageable pageable
    ) {
        Page<EmployeeResponseDto> page = service.searchEmployees(request, pageable);
        return ResponseEntity.ok(ApiResponseDto.ok(page));
    }
    
    // ================================
    // 啟用員工
    // ================================
    @PutMapping("/{id}/activate")
    @Operation(
            summary = "啟用員工",
            description = "將指定員工的狀態設為 ACTIVE（啟用），使其可再次用於業務流程"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功啟用員工",
                    content = @Content(schema = @Schema(implementation = EmployeeResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "找不到員工",
                    content = @Content(schema = @Schema(implementation = NotFoundResponse.class))),
            @ApiResponse(responseCode = "500", description = "伺服器內部錯誤",
                    content = @Content(schema = @Schema(implementation = InternalServerErrorResponse.class)))
    })
    public ResponseEntity<ApiResponseDto<EmployeeResponseDto>> activate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponseDto.ok(service.activate(id)));
    }
    
    // ================================
    // 停用員工
    // ================================
    @PutMapping("/{id}/deactivate")
    @Operation(
            summary = "停用員工",
            description = "將指定員工的狀態設為 INACTIVE（停用），停用後該員工不會出現在下拉選單中"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功停用員工",
                    content = @Content(schema = @Schema(implementation = EmployeeResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "找不到員工",
                    content = @Content(schema = @Schema(implementation = NotFoundResponse.class))),
            @ApiResponse(responseCode = "500", description = "伺服器內部錯誤",
                    content = @Content(schema = @Schema(implementation = InternalServerErrorResponse.class)))
    })
    public ResponseEntity<ApiResponseDto<EmployeeResponseDto>> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponseDto.ok(service.deactivate(id)));
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
