package com.lianhua.erp.web.controller;

import com.lianhua.erp.dto.product.ProductDto;
import com.lianhua.erp.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@Tag(name = "產品管理", description = "產品 CRUD API")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    @Operation(summary = "取得所有產品")
    public List<ProductDto> getAllProducts() {
        return productService.getAllProducts();
    }

    @GetMapping("/{id}")
    @Operation(summary = "取得單一產品")
    public ProductDto getProduct(@PathVariable Long id) {
        return productService.getProductById(id);
    }

    @PostMapping
    @Operation(summary = "新增產品")
    public ProductDto createProduct(@RequestBody ProductDto dto) {
        return productService.createProduct(dto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新產品")
    public ProductDto updateProduct(@PathVariable Long id, @RequestBody ProductDto dto) {
        return productService.updateProduct(id, dto);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "刪除產品")
    public void deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
    }
}
