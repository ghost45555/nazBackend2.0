package com.example.productmanager.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Data
@Table(name = "product_weight_options")
public class ProductWeightOption {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    @JsonIgnoreProperties({"weightOptions"})
    private Product product;

    @Column(name = "weight_value", nullable = false)
    private Integer weightValue;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "packaging_photo")
    private String packagingPhoto;
    
    @Column(columnDefinition = "LONGBLOB")
    @JsonIgnore
    private byte[] packagingPhotoData;
} 