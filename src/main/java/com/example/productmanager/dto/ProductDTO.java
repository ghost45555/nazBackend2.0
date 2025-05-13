package com.example.productmanager.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDTO {
    private Long id;
    private String name;
    private String description;
    private String imageUrl;
    private Long categoryId;
    private String categoryName; // Added for display purposes on edit form
    private Boolean isNewArrival;
    private Boolean isBestSeller;
    private Boolean isFeatured;
    private BigDecimal pricePerKg; // Assuming this field exists in your 'products' table
    private Boolean hasDiscount;
    private Integer discountPercentage;
    private Integer inventory;

    private List<Long> certificationIds;
    private List<String> features; // Assuming these are just text features
    private List<SpecificationDTO> specifications;
    private NutritionalInfoDTO nutritionalInfo;
    private List<WeightOptionDTO> weightOptions;

    // Consider adding fields for existing certifications, features, etc.,
    // if you want to display them alongside the selection/input mechanisms
    // e.g., private List<CertificationDTO> existingCertifications;
} 