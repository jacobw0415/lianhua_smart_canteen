package com.lianhua.erp.repository;

import com.lianhua.erp.domin.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {}
