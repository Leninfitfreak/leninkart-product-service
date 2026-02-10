package com.example.product.repo;

import com.example.product.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findAllByOrderByIdDesc();
    List<Product> findAllByCreatedByOrderByIdDesc(String createdBy);
}
