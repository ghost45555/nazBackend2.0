package com.example.productmanager.repository;

import com.example.productmanager.model.ProductNutritionalInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ProductNutritionalInfoRepository extends JpaRepository<ProductNutritionalInfo, Long> {
    Optional<ProductNutritionalInfo> findByProductId(Long productId);
    void deleteByProductId(Long productId);
} 