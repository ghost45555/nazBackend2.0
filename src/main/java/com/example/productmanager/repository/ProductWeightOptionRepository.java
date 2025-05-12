package com.example.productmanager.repository;

import com.example.productmanager.model.ProductWeightOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProductWeightOptionRepository extends JpaRepository<ProductWeightOption, Long> {
    List<ProductWeightOption> findByProductId(Long productId);

    @Modifying
    @Query("DELETE FROM ProductWeightOption pwo WHERE pwo.product.id = :productId")
    void deleteByProductId(Long productId);
} 