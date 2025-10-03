package com.lianhua.erp.controller;

import com.lianhua.erp.dto.SaleRequestDto;
import com.lianhua.erp.dto.SaleResponseDto;
import com.lianhua.erp.service.SaleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sales")
@Tag(name = "散客銷售管理", description = "散客便當銷售 API")
public class SaleController {

    private final SaleService saleService;

    public SaleController(SaleService saleService) {
        this.saleService = saleService;
    }

    @GetMapping
    @Operation(summary = "取得所有銷售紀錄")
    public List<SaleResponseDto> getAllSales() {
        return saleService.getAllSales();
    }

    @GetMapping("/{id}")
    @Operation(summary = "取得單筆銷售紀錄（含產品資訊）")
    public SaleResponseDto getSale(@PathVariable Long id) {
        return saleService.getSaleById(id);
    }

    @PostMapping
    @Operation(summary = "新增銷售紀錄")
    public SaleResponseDto createSale(@RequestBody SaleRequestDto dto) {
        return saleService.createSale(dto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新銷售紀錄")
    public SaleResponseDto updateSale(@PathVariable Long id, @RequestBody SaleRequestDto dto) {
        return saleService.updateSale(id, dto);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "刪除銷售紀錄")
    public void deleteSale(@PathVariable Long id) {
        saleService.deleteSale(id);
    }
}
