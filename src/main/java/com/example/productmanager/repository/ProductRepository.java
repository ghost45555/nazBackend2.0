package com.example.productmanager.repository;

import com.example.productmanager.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByCategoryId(Long categoryId);
    List<Product> findByIsNewArrivalTrue();
    List<Product> findByIsBestSellerTrue();
    List<Product> findByIsFeaturedTrue();
    Optional<Product> findByName(String name);
} 