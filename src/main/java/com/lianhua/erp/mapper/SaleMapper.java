package com.lianhua.erp.mapper;

import com.lianhua.erp.dto.product.ProductDto;
import com.lianhua.erp.dto.sale.SaleRequestDto;
import com.lianhua.erp.dto.sale.SaleResponseDto;
import com.lianhua.erp.domin.Product;
import com.lianhua.erp.domin.Sale;
import com.lianhua.erp.repository.ProductRepository;
import org.springframework.stereotype.Component;

@Component
public class SaleMapper {

    private final ProductRepository productRepository;

    public SaleMapper(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public SaleResponseDto toResponseDto(Sale sale) {
        if (sale == null) return null;

        Product product = sale.getProduct();

        return SaleResponseDto.builder()
                .id(sale.getId())
                .saleDate(sale.getSaleDate() != null ? sale.getSaleDate().toString() : null)
                .qty(sale.getQty())
                .amount(sale.getAmount())
                .payMethod(sale.getPayMethod() != null ? sale.getPayMethod().name() : null)
                .product(product != null
                        ? ProductDto.builder()
                        .id(product.getId())
                        .name(product.getName())
                        .category(String.valueOf(product.getCategory()))
                        .unitPrice(product.getUnitPrice())
                        .active(product.getActive())
                        .build()
                        : null)
                .build();
    }

    public Sale toEntity(SaleRequestDto dto) {
        if (dto == null) return null;

        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found: " + dto.getProductId()));

        return Sale.builder()
                .saleDate(dto.getSaleDate() != null ? java.time.LocalDate.parse(dto.getSaleDate()) : null)
                .product(product)
                .qty(dto.getQty())
                .amount(dto.getAmount())
                .payMethod(dto.getPayMethod() != null ? Sale.PayMethod.valueOf(dto.getPayMethod()) : null)
                .build();
    }

    public void updateEntityFromDto(SaleRequestDto dto, Sale sale) {
        if (dto.getSaleDate() != null) {
            sale.setSaleDate(java.time.LocalDate.parse(dto.getSaleDate()));
        }
        if (dto.getProductId() != null) {
            Product product = productRepository.findById(dto.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found: " + dto.getProductId()));
            sale.setProduct(product);
        }
        sale.setQty(dto.getQty());
        sale.setAmount(dto.getAmount());
        if (dto.getPayMethod() != null) {
            sale.setPayMethod(Sale.PayMethod.valueOf(dto.getPayMethod()));
        }
    }
}
