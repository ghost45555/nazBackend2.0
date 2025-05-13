package com.example.productmanager.repository;

import com.example.productmanager.entity.Order;
import com.example.productmanager.entity.OrderStatus;
import com.example.productmanager.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUser(User user);
    List<Order> findByStatus(OrderStatus status);
    List<Order> findByEmail(String email);
    List<Order> findAllByOrderByCreatedAtDesc();
    
    // New methods for order tracking
    Optional<Order> findByIdAndUser(Long id, User user);
    Optional<Order> findByIdAndEmail(Long id, String email);
    
    // Convenience method for checking order ownership
    Optional<Order> findByIdAndUserId(Long id, Long userId);
} 