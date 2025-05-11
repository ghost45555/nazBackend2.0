package com.example.productmanager.model;

import jakarta.persistence.*;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonBackReference;

@Entity
@Data
@Table(name = "product_nutritional_info")
public class ProductNutritionalInfo {
    @Id
    private Long productId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "product_id")
    @JsonBackReference("product-nutritionalInfo")
    private Product product;

    private String servingSize;
    private String servingsPerContainer;
    private String calories;
    private String totalFat;
    private String saturatedFat;
    private String transFat;
    private String cholesterol;
    private String sodium;
    private String totalCarbohydrates;
    private String dietaryFiber;
    private String sugars;
    private String protein;
    private String vitaminA;
    private String vitaminC;
    private String calcium;
    private String iron;
} 