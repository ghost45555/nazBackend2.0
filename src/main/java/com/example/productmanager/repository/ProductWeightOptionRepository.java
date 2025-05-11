package com.example.productmanager.repository;

import com.example.productmanager.model.ProductWeightOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProductWeightOptionRepository extends JpaRepository<ProductWeightOption, Long> {
    List<ProductWeightOption> findByProductId(Long productId);
    void deleteByProductId(Long productId);
} 