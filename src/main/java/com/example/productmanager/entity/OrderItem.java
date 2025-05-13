package com.example.productmanager.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import com.example.productmanager.model.Product;

@Data
@Entity
@Table(name = "order_items")
public class OrderItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;
    
    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;
    
    @Column(name = "product_name", nullable = false)
    private String productName;
    
    @Column(name = "product_image")
    private String productImage;
    
    @Column(name = "packaging_photo")
    private String packagingPhoto;
    
    @Column
    private Integer weight;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;
    
    @Column(nullable = false)
    private Integer quantity;
} 