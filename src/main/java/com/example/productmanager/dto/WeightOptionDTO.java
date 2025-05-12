package com.example.productmanager.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeightOptionDTO {
    private Long id; // Optional: useful if you need to identify existing options vs new ones
    private String weightValue; // e.g., "1kg", "500g"
    private BigDecimal price;
    private String packagingPhoto; // URL or path
} 