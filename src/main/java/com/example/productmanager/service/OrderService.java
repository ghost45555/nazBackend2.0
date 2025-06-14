package com.example.productmanager.service;

import com.example.productmanager.dto.CreateOrderDTO;
import com.example.productmanager.dto.OrderDTO;
import com.example.productmanager.dto.OrderItemDTO;
import com.example.productmanager.entity.Order;
import com.example.productmanager.entity.OrderItem;
import com.example.productmanager.entity.OrderStatus;
import com.example.productmanager.entity.User;
import com.example.productmanager.entity.Role;
import com.example.productmanager.model.Product;
import com.example.productmanager.repository.OrderItemRepository;
import com.example.productmanager.repository.OrderRepository;
import com.example.productmanager.repository.OrderStatusRepository;
import com.example.productmanager.repository.UserRepository;
import com.example.productmanager.repository.RoleRepository;
import com.example.productmanager.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
@Service
public class OrderService {
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private OrderStatusRepository orderStatusRepository;

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<OrderDTO> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrderDTO getOrderById(Long id) {
        return orderRepository.findById(id)
                .map(this::convertToDTO)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<OrderDTO> getOrdersByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));
        
        return orderRepository.findByUser(user)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderDTO> getOrdersByEmail(String email) {
        return orderRepository.findByEmail(email)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderDTO> getOrdersByStatus(String statusName) {
        OrderStatus status = orderStatusRepository.findByName(statusName)
                .orElseThrow(() -> new EntityNotFoundException("Order status not found with name: " + statusName));
        
        return orderRepository.findByStatus(status)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public OrderDTO createOrder(CreateOrderDTO createOrderDTO) {
        try {
            // Find user or create a new one for guest checkout
            User user = null;
            if (createOrderDTO.getUserId() != null) {
                // Existing user
                user = userRepository.findById(createOrderDTO.getUserId())
                        .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + createOrderDTO.getUserId()));
            } else {
                // Guest checkout - create a new user with email only
                Optional<User> existingUser = userRepository.findByEmail(createOrderDTO.getEmail());
                
                if (existingUser.isPresent()) {
                    // Use existing user if email already exists
                    user = existingUser.get();
                    logger.info("Using existing user with email: {}", createOrderDTO.getEmail());
                } else {
                    // Create a new user with generated username and placeholder password
                    user = new User();
                    user.setEmail(createOrderDTO.getEmail());
                    user.setFirstName(createOrderDTO.getFirstName());
                    user.setLastName(createOrderDTO.getLastName());
                    user.setPhone(createOrderDTO.getPhone());
                    
                    // Generate a unique username based on email
                    String emailUsername = createOrderDTO.getEmail().split("@")[0];
                    String uniqueUsername = emailUsername + "_guest_" + System.currentTimeMillis();
                    user.setUsername(uniqueUsername);
                    
                    // Set a placeholder password (not usable for login)
                    user.setPassword("GUEST_USER_NO_LOGIN");
                    
                    // Assign ROLE_USER to the new user
                    Role userRole = roleRepository.findByName("ROLE_USER")
                            .orElseThrow(() -> new EntityNotFoundException("ROLE_USER not found"));
                    Set<Role> roles = new HashSet<>();
                    roles.add(userRole);
                    user.setRoles(roles);
                    
                    user = userRepository.save(user);
                    logger.info("Created new user with email: {}", createOrderDTO.getEmail());
                }
            }
            
            // Find default order status (Pending)
            OrderStatus pendingStatus = orderStatusRepository.findByName("Pending")
                    .orElseThrow(() -> new EntityNotFoundException("Pending order status not found"));
            
            // Create the order
            Order order = new Order();
            order.setUser(user);
            order.setEmail(createOrderDTO.getEmail());
            order.setFirstName(createOrderDTO.getFirstName());
            order.setLastName(createOrderDTO.getLastName());
            order.setAddress(createOrderDTO.getAddress());
            order.setApartment(createOrderDTO.getApartment());
            order.setCity(createOrderDTO.getCity());
            order.setPostalCode(createOrderDTO.getPostalCode());
            order.setPhone(createOrderDTO.getPhone());
            order.setOrderNotes(createOrderDTO.getOrderNotes());
            order.setPaymentMethod(createOrderDTO.getPaymentMethod());
            order.setSubtotal(createOrderDTO.getSubtotal());
            order.setShipping(createOrderDTO.getShipping());
            order.setTotal(createOrderDTO.getTotal());
            order.setStatus(pendingStatus);
            order.setCreatedAt(LocalDateTime.now());
            order.setUpdatedAt(LocalDateTime.now());
            
            // Inventory validation: check all items before saving order
            for (OrderItemDTO itemDTO : createOrderDTO.getItems()) {
                if (itemDTO.getProductId() != null) {
                    Product product = productRepository.findById(itemDTO.getProductId()).orElse(null);
                    if (product != null) {
                        int currentInventory = product.getInventory() != null ? product.getInventory() : 0;
                        int requestedQty = itemDTO.getQuantity() != null ? itemDTO.getQuantity() : 0;
                        if (requestedQty > currentInventory) {
                            throw new RuntimeException("Insufficient inventory for product: " + product.getName() + ". Requested: " + requestedQty + ", Available: " + currentInventory);
                        }
                    }
                }
            }
            
            // Save the order to get an ID
            Order savedOrder = orderRepository.save(order);
            
            // Create and save order items
            List<OrderItem> orderItems = new ArrayList<>();
            Map<Long, Product> productsToUpdate = new HashMap<>(); // Track products that need inventory updates
            
            for (OrderItemDTO itemDTO : createOrderDTO.getItems()) {
                OrderItem item = new OrderItem();
                item.setOrder(savedOrder);
                // Enforce productId is present and valid
                if (itemDTO.getProductId() == null) {
                    throw new RuntimeException("Order item is missing productId");
                }
                Product product = productRepository.findById(itemDTO.getProductId())
                        .orElseThrow(() -> new EntityNotFoundException("Product not found with id: " + itemDTO.getProductId()));
                item.setProduct(product);
                item.setProductName(itemDTO.getProductName());
                item.setProductImage(itemDTO.getProductImage());
                item.setPackagingPhoto(itemDTO.getPackagingPhoto());
                item.setWeight(itemDTO.getWeight());
                item.setPrice(itemDTO.getPrice());
                item.setQuantity(itemDTO.getQuantity());
                orderItems.add(item);
                
                // Store product for inventory update (will be used as fallback if trigger doesn't work)
                productsToUpdate.put(product.getId(), product);
            }
            
            // Save all order items
            List<OrderItem> savedOrderItems = orderItemRepository.saveAll(orderItems);
            logger.info("Saved {} order items for order #{}", savedOrderItems.size(), savedOrder.getId());

            // Check if inventory was updated by the trigger by comparing with initial values
            // If not, manually update inventory as fallback
            boolean manualUpdateNeeded = true;
            
            try {
                // Wait briefly to allow triggers to execute
                Thread.sleep(100);
                
                // Refresh products from database to see if triggers updated inventory
                for (Map.Entry<Long, Product> entry : productsToUpdate.entrySet()) {
                    Product updatedProduct = productRepository.findById(entry.getKey()).orElse(null);
                    if (updatedProduct != null) {
                        Product originalProduct = entry.getValue();
                        if (updatedProduct.getInventory().equals(originalProduct.getInventory())) {
                            // Inventory wasn't changed by trigger, we need to manually update
                            logger.warn("Trigger did not update inventory for product {}. Will update manually.", updatedProduct.getId());
                        } else {
                            // Inventory was updated by trigger, no manual update needed
                            manualUpdateNeeded = false;
                            logger.info("Trigger successfully updated inventory for product {}", updatedProduct.getId());
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("Error checking trigger inventory updates: {}", e.getMessage());
            }
            
            // If needed, manually update inventory
            if (manualUpdateNeeded) {
                logger.warn("Performing manual inventory update as fallback since triggers aren't working");
                for (OrderItem item : savedOrderItems) {
                    if (item.getProduct() != null && item.getQuantity() != null) {
                        Product product = item.getProduct();
                        int currentInventory = product.getInventory() != null ? product.getInventory() : 0;
                        product.setInventory(currentInventory - item.getQuantity());
                        productRepository.save(product);
                        logger.info("Manually updated inventory for product {} from {} to {}", 
                            product.getId(), currentInventory, product.getInventory());
                    }
                }
            }
            
            // Fetch the complete order with items
            return getOrderById(savedOrder.getId());
        } catch (Exception e) {
            logger.error("Error creating order: {}", e.getMessage());
            throw new RuntimeException("Failed to create order: " + e.getMessage());
        }
    }

    @Transactional
    public OrderDTO updateOrderStatus(Long orderId, String statusName) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with id: " + orderId));
        
        OrderStatus status = orderStatusRepository.findByName(statusName)
                .orElseThrow(() -> new EntityNotFoundException("Order status not found with name: " + statusName));
        
        // No need to manually update inventory for cancelled/refunded orders
        // The inventory will be handled by the database triggers when order items are modified or deleted
        
        order.setStatus(status);
        order.setUpdatedAt(LocalDateTime.now());
        
        Order updatedOrder = orderRepository.save(order);
        return convertToDTO(updatedOrder);
    }

    @Transactional
    public boolean deleteOrder(Long id) {
        if (orderRepository.existsById(id)) {
            orderRepository.deleteById(id);
            return true;
        }
        return false;
    }

    @Transactional(readOnly = true)
    public OrderDTO getOrderByIdAndUserId(Long id, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));
        
        return orderRepository.findByIdAndUser(id, user)
                .map(this::convertToDTO)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with id: " + id + " for user id: " + userId));
    }
    
    @Transactional(readOnly = true)
    public OrderDTO getOrderByIdAndEmail(Long id, String email) {
        return orderRepository.findByIdAndEmail(id, email)
                .map(this::convertToDTO)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with id: " + id + " for email: " + email));
    }

    // Convert Order entity to DTO including items
    private OrderDTO convertToDTO(Order order) {
        List<OrderItem> items = orderItemRepository.findByOrder(order);
        logger.info("Order #{}: found {} items in DB", order.getId(), items.size());
        
        List<OrderItemDTO> itemDTOs = items.stream()
                .map(item -> new OrderItemDTO(
                        item.getId(),
                        item.getOrder() != null ? item.getOrder().getId() : null,
                        item.getProduct() != null ? item.getProduct().getId() : null,
                        item.getProductName(),
                        item.getProductImage(),
                        item.getPackagingPhoto(),
                        item.getWeight(),
                        item.getPrice(),
                        item.getQuantity()
                ))
                .collect(Collectors.toList());
        
        OrderDTO dto = new OrderDTO(
                order.getId(),
                order.getUser() != null ? order.getUser().getId() : null,
                order.getEmail(),
                order.getFirstName(),
                order.getLastName(),
                order.getAddress(),
                order.getApartment(),
                order.getCity(),
                order.getPostalCode(),
                order.getPhone(),
                order.getOrderNotes(),
                order.getPaymentMethod(),
                order.getSubtotal(),
                order.getShipping(),
                order.getTotal(),
                order.getStatus() != null ? order.getStatus().getId() : null,
                order.getStatus() != null ? order.getStatus().getName() : null,
                order.getCreatedAt(),
                order.getUpdatedAt(),
                itemDTOs
        );
        return dto;
    }
} 