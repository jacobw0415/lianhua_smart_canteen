package com.lianhua.erp.web.controller;

import com.lianhua.erp.dto.apiResponse.ApiResponseDto;
import com.lianhua.erp.dto.error.*;
import com.lianhua.erp.dto.supplier.*;
import com.lianhua.erp.service.SupplierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

/**
 * 供應商管理 API
 * 包含：查詢、建立、更新、刪除。
 */
@RestController
@RequestMapping("/api/suppliers")
@Tag(name = "供應商管理", description = "供應商 CRUD API")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService supplierService;

    // ============================================================
    // 取得所有供應商
    // ============================================================
    @Operation(summary = "取得所有供應商")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "成功取得供應商列表", content = @Content(schema = @Schema(implementation = SupplierDto.class))), @ApiResponse(responseCode = "500", description = "伺服器錯誤", content = @Content(schema = @Schema(implementation = InternalServerErrorResponse.class)))})
    @GetMapping
    public ResponseEntity<ApiResponseDto<List<SupplierDto>>> getAllSuppliers() {
        return ResponseEntity.ok(ApiResponseDto.ok(supplierService.getAllSuppliers()));
    }

    // ============================================================
    // 取得單一供應商
    // ============================================================
    @Operation(summary = "取得指定供應商資料")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "成功取得供應商資料", content = @Content(schema = @Schema(implementation = SupplierDto.class))), @ApiResponse(responseCode = "404", description = "找不到供應商", content = @Content(schema = @Schema(implementation = NotFoundResponse.class))), @ApiResponse(responseCode = "500", description = "伺服器錯誤", content = @Content(schema = @Schema(implementation = InternalServerErrorResponse.class)))})
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDto<SupplierDto>> getSupplierById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponseDto.ok(supplierService.getSupplierById(id)));
    }

    // ============================================================
    // 建立供應商
    // ============================================================
    @Operation(summary = "建立新供應商")
    @ApiResponses({@ApiResponse(responseCode = "201", description = "成功建立供應商", content = @Content(schema = @Schema(implementation = SupplierDto.class))), @ApiResponse(responseCode = "400", description = "參數錯誤", content = @Content(schema = @Schema(implementation = BadRequestResponse.class))), @ApiResponse(responseCode = "409", description = "資料重複或供應商已存在", content = @Content(schema = @Schema(implementation = ConflictResponse.class))), @ApiResponse(responseCode = "500", description = "伺服器錯誤", content = @Content(schema = @Schema(implementation = InternalServerErrorResponse.class)))})
    @PostMapping
    public ResponseEntity<ApiResponseDto<SupplierDto>> createSupplier(@Valid @RequestBody SupplierRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponseDto.created(supplierService.createSupplier(dto)));
    }

    // ============================================================
    // 更新供應商
    // ============================================================
    @Operation(summary = "更新供應商資料")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "成功更新供應商", content = @Content(schema = @Schema(implementation = SupplierDto.class))), @ApiResponse(responseCode = "400", description = "更新參數錯誤", content = @Content(schema = @Schema(implementation = BadRequestResponse.class))), @ApiResponse(responseCode = "404", description = "找不到供應商", content = @Content(schema = @Schema(implementation = NotFoundResponse.class))), @ApiResponse(responseCode = "409", description = "供應商名稱已存在，違反唯一約束", content = @Content(schema = @Schema(implementation = ConflictResponse.class))), @ApiResponse(responseCode = "500", description = "伺服器錯誤", content = @Content(schema = @Schema(implementation = InternalServerErrorResponse.class)))})
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseDto<SupplierDto>> updateSupplier(@PathVariable Long id, @Valid @RequestBody SupplierRequestDto dto) {
        return ResponseEntity.ok(ApiResponseDto.ok(supplierService.updateSupplier(id, dto)));
    }

    // ============================================================
    // 刪除供應商
    // ============================================================
    @Operation(summary = "刪除指定供應商")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "成功刪除供應商", content = @Content(schema = @Schema(implementation = NoContentResponse.class))), @ApiResponse(responseCode = "404", description = "找不到供應商", content = @Content(schema = @Schema(implementation = NotFoundResponse.class))), @ApiResponse(responseCode = "500", description = "伺服器錯誤", content = @Content(schema = @Schema(implementation = InternalServerErrorResponse.class)))})
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseDto<Void>> deleteSupplier(@PathVariable Long id) {
        supplierService.deleteSupplier(id);
        return ResponseEntity.ok(ApiResponseDto.deleted());
    }

    // ============================================================
    // 搜尋供應商
    // ============================================================
    @Operation(summary = "搜尋供應商", description = "可依名稱、聯絡人、電話、結帳週期、備註進行模糊或精確搜尋")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "成功", content = @Content(schema = @Schema(implementation = SupplierDto[].class))), @ApiResponse(responseCode = "400", description = "搜尋參數錯誤", content = @Content(schema = @Schema(implementation = BadRequestResponse.class))), @ApiResponse(responseCode = "500", description = "伺服器錯誤", content = @Content(schema = @Schema(implementation = InternalServerErrorResponse.class)))})
    @GetMapping("/search")
    public ResponseEntity<ApiResponseDto<List<SupplierDto>>> searchSuppliers(@Parameter(description = "供應商名稱(模糊搜尋)") @RequestParam(required = false) String supplierName, @Parameter(description = "聯絡人(模糊搜尋)") @RequestParam(required = false) String contact, @Parameter(description = "電話(模糊搜尋)") @RequestParam(required = false) String phone, @Parameter(description = "結帳週期(精確搜尋)") @RequestParam(required = false) String billingCycle, @Parameter(description = "備註(模糊搜尋)") @RequestParam(required = false) String note) {

        // ✨ 使用 ModelAttribute 統一封裝（關鍵！）
        SupplierSearchRequest req = new SupplierSearchRequest();
        req.setSupplierName(supplierName);
        req.setContact(contact);
        req.setPhone(phone);
        req.setBillingCycle(billingCycle);
        req.setNote(note);

        List<SupplierDto> results = supplierService.searchSuppliers(req);
        return ResponseEntity.ok(ApiResponseDto.ok(results));
    }

}
