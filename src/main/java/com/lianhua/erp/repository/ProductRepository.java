package com.lianhua.erp.repository;

import com.lianhua.erp.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    List<Product> findByActiveTrue();

    List<Product> findByCategoryId(Long categoryId);

    boolean existsByName(String name);

    boolean existsByCategoryId(Long categoryId);

    /**
     * 檢查是否存在指定名稱的商品（排除指定 ID 的商品）
     * 用於更新商品時檢查名稱唯一性
     */
    boolean existsByNameAndIdNot(String name, Long id);
}
