package com.example.productmanager.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderDTO {
    // Customer information
    private Long userId; // Optional - can be null for guest checkout
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
    
    // Order totals
    private BigDecimal subtotal;
    private BigDecimal shipping;
    private BigDecimal total;
    
    // Items in the order
    private List<OrderItemDTO> items;

    public Long getUserId() { return userId; }

    public void setUserId(Long userId) { this.userId = userId; }
} 