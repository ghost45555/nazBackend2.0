package com.example.productmanager.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO {
    private Long id;
    private Long userId;
    private String email;
    private String firstName;
    private String lastName;
    private String address;
    private String apartment;
    private String city;
    private String postalCode;
    private String phone;
    private String orderNotes;
    private String paymentMethod;
    private BigDecimal subtotal;
    private BigDecimal shipping;
    private BigDecimal total;
    private Long statusId;
    private String statusName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<OrderItemDTO> orderItems;
}