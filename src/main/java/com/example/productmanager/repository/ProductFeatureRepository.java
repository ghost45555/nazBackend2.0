package com.example.productmanager.repository;

import com.example.productmanager.model.ProductFeature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Repository
public interface ProductFeatureRepository extends JpaRepository<ProductFeature, Long> {
    List<ProductFeature> findByProductId(Long productId);
    
    @Modifying
    @Transactional
    @Query("DELETE FROM ProductFeature pf WHERE pf.product.id = :productId")
    void deleteByProductId(Long productId);
} 