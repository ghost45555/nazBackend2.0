package com.example.productmanager.service;

import com.example.productmanager.dto.OrderStatusDTO;
import com.example.productmanager.entity.OrderStatus;
import com.example.productmanager.repository.OrderStatusRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class OrderStatusService {

    @Autowired
    private OrderStatusRepository orderStatusRepository;

    /**
     * Initialize default order statuses
     */
    @PostConstruct
    public void init() {
        if (orderStatusRepository.count() == 0) {
            createDefaultOrderStatuses();
        }
    }

    private void createDefaultOrderStatuses() {
        String[][] defaultStatuses = {
            {"Pending", "Order has been placed but not yet processed"},
            {"Processing", "Order is being prepared"},
            {"Shipped", "Order has been shipped"},
            {"Delivered", "Order has been delivered"},
            {"Cancelled", "Order has been cancelled"},
            {"Refunded", "Order has been refunded"}
        };

        for (String[] status : defaultStatuses) {
            OrderStatus orderStatus = new OrderStatus();
            orderStatus.setName(status[0]);
            orderStatus.setDescription(status[1]);
            orderStatusRepository.save(orderStatus);
        }
    }

    @Transactional(readOnly = true)
    public List<OrderStatusDTO> getAllOrderStatuses() {
        return orderStatusRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrderStatusDTO getOrderStatusById(Long id) {
        return orderStatusRepository.findById(id)
                .map(this::convertToDTO)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public OrderStatusDTO getOrderStatusByName(String name) {
        return orderStatusRepository.findByName(name)
                .map(this::convertToDTO)
                .orElse(null);
    }

    @Transactional
    public OrderStatusDTO createOrderStatus(OrderStatusDTO orderStatusDTO) {
        OrderStatus orderStatus = convertToEntity(orderStatusDTO);
        OrderStatus savedOrderStatus = orderStatusRepository.save(orderStatus);
        return convertToDTO(savedOrderStatus);
    }

    @Transactional
    public OrderStatusDTO updateOrderStatus(Long id, OrderStatusDTO orderStatusDTO) {
        Optional<OrderStatus> existingOrderStatus = orderStatusRepository.findById(id);
        if (existingOrderStatus.isPresent()) {
            OrderStatus orderStatus = existingOrderStatus.get();
            orderStatus.setName(orderStatusDTO.getName());
            orderStatus.setDescription(orderStatusDTO.getDescription());
            return convertToDTO(orderStatusRepository.save(orderStatus));
        }
        return null;
    }

    @Transactional
    public boolean deleteOrderStatus(Long id) {
        if (orderStatusRepository.existsById(id)) {
            orderStatusRepository.deleteById(id);
            return true;
        }
        return false;
    }

    private OrderStatusDTO convertToDTO(OrderStatus orderStatus) {
        return new OrderStatusDTO(
                orderStatus.getId(),
                orderStatus.getName(),
                orderStatus.getDescription()
        );
    }

    private OrderStatus convertToEntity(OrderStatusDTO orderStatusDTO) {
        OrderStatus orderStatus = new OrderStatus();
        if (orderStatusDTO.getId() != null) {
            orderStatus.setId(orderStatusDTO.getId());
        }
        orderStatus.setName(orderStatusDTO.getName());
        orderStatus.setDescription(orderStatusDTO.getDescription());
        return orderStatus;
    }
} 