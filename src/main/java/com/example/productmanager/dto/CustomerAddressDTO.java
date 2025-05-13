package com.example.productmanager.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerAddressDTO {
    private Long id;
    private Long userId;
    private String address;
    private String apartment;
    private String city;
    private String postalCode;
    private Boolean isDefault;
} 