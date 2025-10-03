package com.lianhua.erp.web.controller;

import com.lianhua.erp.dto.SupplierDto;
import com.lianhua.erp.service.SupplierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/suppliers")
@Tag(name = "供應商管理", description = "管理供應商的 CRUD API")
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @GetMapping
    @Operation(summary = "取得所有供應商", description = "回傳所有供應商清單")
    public List<SupplierDto> getAllSuppliers() {
        return supplierService.getAllSuppliers();
    }

    @GetMapping("/{id}")
    @Operation(summary = "取得單筆供應商", description = "根據 ID 取得供應商")
    public SupplierDto getSupplierById(@PathVariable Long id) {
        return supplierService.getSupplierById(id);
    }

    @PostMapping
    @Operation(summary = "新增供應商", description = "建立一位新的供應商")
    public SupplierDto createSupplier(@RequestBody SupplierDto dto) {
        return supplierService.createSupplier(dto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新供應商", description = "更新一筆現有的供應商資料")
    public SupplierDto updateSupplier(@PathVariable Long id, @RequestBody SupplierDto dto) {
        return supplierService.updateSupplier(id, dto);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "刪除供應商", description = "刪除指定 ID 的供應商")
    public void deleteSupplier(@PathVariable Long id) {
        supplierService.deleteSupplier(id);
    }
}
