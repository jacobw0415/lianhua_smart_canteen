package com.lianhua.erp.web.controller;

import com.lianhua.erp.dto.purchase.PurchaseDto;
import com.lianhua.erp.service.PurchaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/purchases")
@Tag(name = "採購管理", description = "採購單 (含付款明細) API")
public class PurchaseController {

    private final PurchaseService purchaseService;

    public PurchaseController(PurchaseService purchaseService) {
        this.purchaseService = purchaseService;
    }

    // === Purchase CRUD ===

    @GetMapping
    @Operation(summary = "查詢所有採購單（含付款明細）")
    public List<PurchaseDto> getAllPurchases() {
        return purchaseService.getAllPurchases();
    }

    @GetMapping("/{id}")
    @Operation(summary = "查詢單筆採購單（含付款明細）")
    public PurchaseDto getPurchase(@PathVariable Long id) {
        return purchaseService.getPurchaseById(id);
    }

    @PostMapping
    @Operation(summary = "新增採購單（可同時建立付款明細）")
    public PurchaseDto createPurchase(@RequestBody PurchaseDto dto) {
        return purchaseService.createPurchase(dto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新採購單（可同時更新付款明細）")
    public PurchaseDto updatePurchase(@PathVariable Long id, @RequestBody PurchaseDto dto) {
        return purchaseService.updatePurchase(id, dto);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "刪除採購單（連同付款明細）")
    public void deletePurchase(@PathVariable Long id) {
        purchaseService.deletePurchase(id);
    }
}
