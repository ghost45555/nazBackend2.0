package com.example.productmanager.model;

import jakarta.persistence.*;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Getter
@Setter
@Table(name = "product_nutritional_info")
public class ProductNutritionalInfo {
    @Id
    private Long productId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "product_id")
    @JsonIgnoreProperties({"nutritionalInfo"})
    private Product product;

    @Column(name = "serving_size")
    private String servingSize;

    @Column(name = "servings_per_container")
    private String servingsPerContainer;

    @Column(name = "calories")
    private String calories;

    @Column(name = "total_fat")
    private String totalFat;

    @Column(name = "saturated_fat")
    private String saturatedFat;

    @Column(name = "trans_fat")
    private String transFat;

    @Column(name = "cholesterol")
    private String cholesterol;

    @Column(name = "sodium")
    private String sodium;

    @Column(name = "total_carbohydrates")
    private String totalCarbohydrates;

    @Column(name = "dietary_fiber")
    private String dietaryFiber;

    @Column(name = "sugars")
    private String sugars;

    @Column(name = "protein")
    private String protein;

    @Column(name = "vitamin_a")
    private String vitaminA;

    @Column(name = "vitamin_c")
    private String vitaminC;

    @Column(name = "calcium")
    private String calcium;

    @Column(name = "iron")
    private String iron;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProductNutritionalInfo that = (ProductNutritionalInfo) o;
        return Objects.equals(productId, that.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId);
    }

    @Override
    public String toString() {
        return "ProductNutritionalInfo{" +
                "productId=" + productId +
                ", servingSize='" + servingSize + '\'' +
                ", servingsPerContainer='" + servingsPerContainer + '\'' +
                ", calories='" + calories + '\'' +
                '}';
    }
} 