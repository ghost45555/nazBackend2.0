package com.example.productmanager.model;

import jakarta.persistence.*;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonBackReference;

@Entity
@Data
@Table(name = "product_specifications")
public class ProductSpecification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    @JsonBackReference("product-specifications")
    private Product product;

    @Column(name = "spec_name", nullable = false, length = 100)
    private String specName;

    @Column(name = "spec_value", nullable = false, length = 255)
    private String specValue;
} 