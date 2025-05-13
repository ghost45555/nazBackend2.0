package com.example.productmanager.controller;

import com.example.productmanager.dto.OrderStatusDTO;
import com.example.productmanager.service.OrderStatusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/order-statuses")
@CrossOrigin(origins = {"http://localhost:5500", "http://127.0.0.1:5500"}, maxAge = 3600)
public class OrderStatusController {

    @Autowired
    private OrderStatusService orderStatusService;

    @GetMapping
    public ResponseEntity<List<OrderStatusDTO>> getAllOrderStatuses() {
        List<OrderStatusDTO> orderStatuses = orderStatusService.getAllOrderStatuses();
        return ResponseEntity.ok(orderStatuses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderStatusDTO> getOrderStatusById(@PathVariable Long id) {
        OrderStatusDTO orderStatus = orderStatusService.getOrderStatusById(id);
        return orderStatus != null
                ? ResponseEntity.ok(orderStatus)
                : ResponseEntity.notFound().build();
    }

    @GetMapping("/name/{name}")
    public ResponseEntity<OrderStatusDTO> getOrderStatusByName(@PathVariable String name) {
        OrderStatusDTO orderStatus = orderStatusService.getOrderStatusByName(name);
        return orderStatus != null
                ? ResponseEntity.ok(orderStatus)
                : ResponseEntity.notFound().build();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('ORDER_MANAGER')")
    public ResponseEntity<OrderStatusDTO> createOrderStatus(@RequestBody OrderStatusDTO orderStatusDTO) {
        OrderStatusDTO createdStatus = orderStatusService.createOrderStatus(orderStatusDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdStatus);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ORDER_MANAGER')")
    public ResponseEntity<OrderStatusDTO> updateOrderStatus(
            @PathVariable Long id,
            @RequestBody OrderStatusDTO orderStatusDTO) {
        OrderStatusDTO updatedStatus = orderStatusService.updateOrderStatus(id, orderStatusDTO);
        return updatedStatus != null
                ? ResponseEntity.ok(updatedStatus)
                : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ORDER_MANAGER')")
    public ResponseEntity<Void> deleteOrderStatus(@PathVariable Long id) {
        boolean deleted = orderStatusService.deleteOrderStatus(id);
        return deleted
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
} 