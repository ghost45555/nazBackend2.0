package com.example.productmanager.repository;

import com.example.productmanager.model.ProductSpecification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Repository
public interface ProductSpecificationRepository extends JpaRepository<ProductSpecification, Long> {
    List<ProductSpecification> findByProductId(Long productId);
    
    @Modifying
    @Transactional
    @Query("DELETE FROM ProductSpecification ps WHERE ps.product.id = :productId")
    void deleteByProductId(Long productId);
} 