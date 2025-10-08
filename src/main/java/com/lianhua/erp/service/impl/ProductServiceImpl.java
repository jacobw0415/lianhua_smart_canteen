package com.lianhua.erp.service.impl;

import com.lianhua.erp.dto.product.ProductDto;
import com.lianhua.erp.domin.Product;
import com.lianhua.erp.mapper.ProductMapper;
import com.lianhua.erp.repository.ProductRepository;
import com.lianhua.erp.service.ProductService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    public ProductServiceImpl(ProductRepository productRepository,
                              ProductMapper productMapper) {
        this.productRepository = productRepository;
        this.productMapper = productMapper;
    }

    @Override
    public List<ProductDto> getAllProducts() {
        return productRepository.findAll().stream()
                .map(productMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public ProductDto getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + id));
        return productMapper.toDto(product);
    }

    @Override
    public ProductDto createProduct(ProductDto dto) {
        Product product = productMapper.toEntity(dto);
        return productMapper.toDto(productRepository.save(product));
    }

    @Override
    public ProductDto updateProduct(Long id, ProductDto dto) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + id));
        productMapper.updateEntityFromDto(dto, product);
        return productMapper.toDto(productRepository.save(product));
    }

    @Override
    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }
}
