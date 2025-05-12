package com.example.productmanager.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NutritionalInfoDTO {
    private String servingSize;
    private String calories;
    private String protein;
    private String fat;
    private String carbohydrates;
    private String fiber;
    private String sugar;
    private String sodium;
    private String iron;
    // Add other string fields as needed (e.g., saturatedFat, transFat, cholesterol, vitaminA, etc.)
} 