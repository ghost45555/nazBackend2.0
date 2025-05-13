package com.example.productmanager.controller;

import com.example.productmanager.dto.JwtResponse;
import com.example.productmanager.dto.LoginRequest;
import com.example.productmanager.dto.SignupRequest;
import com.example.productmanager.dto.CustomerLoginRequest;
import com.example.productmanager.dto.CustomerSignupRequest;
import com.example.productmanager.dto.CustomerAddressDTO;
import com.example.productmanager.entity.CustomerAddress;
import com.example.productmanager.entity.Role;
import com.example.productmanager.entity.User;
import com.example.productmanager.entity.Order;
import com.example.productmanager.repository.RoleRepository;
import com.example.productmanager.repository.UserRepository;
import com.example.productmanager.repository.OrderRepository;
import com.example.productmanager.service.CustomerAddressService;
import com.example.productmanager.security.JwtTokenProvider;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.validation.FieldError;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:5500", "http://127.0.0.1:5500", "http://localhost:8080"}, 
    allowedHeaders = {"Authorization", "Content-Type", "Accept"}, 
    methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE},
    maxAge = 3600)
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private CustomerAddressService customerAddressService;

    public AuthController(AuthenticationManager authenticationManager,
                         UserRepository userRepository,
                         RoleRepository roleRepository,
                         PasswordEncoder passwordEncoder,
                         JwtTokenProvider tokenProvider) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);
        
        User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return ResponseEntity.ok(new JwtResponse(jwt, "Bearer", user.getUsername(), user.getEmail()));
    }
    
    @PostMapping("/customer-login")
    public ResponseEntity<?> authenticateCustomer(@Valid @RequestBody CustomerLoginRequest loginRequest) {
        try {
            logger.info("Processing customer login request for email: {}", loginRequest.getEmail());
            
            // Find user by email instead of username
            User user = userRepository.findByEmail(loginRequest.getEmail())
                    .orElseThrow(() -> new RuntimeException("No account found with this email"));
            
            // Create authentication token with username
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getUsername(), loginRequest.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = tokenProvider.generateToken(authentication);
            
            // Include more user details in response
            Map<String, Object> userData = new HashMap<>();
            userData.put("id", user.getId());
            userData.put("username", user.getUsername());
            userData.put("email", user.getEmail());
            userData.put("firstName", user.getFirstName());
            userData.put("lastName", user.getLastName());
            userData.put("phone", user.getPhone());
            
            Map<String, Object> response = new HashMap<>();
            response.put("token", jwt);
            response.put("tokenType", "Bearer");
            response.put("user", userData);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error during customer login: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid email or password"));
        }
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signupRequest) {
        try {
            logger.info("Processing signup request for username: {} with role: {}", 
                signupRequest.getUsername(), signupRequest.getRole());
            
            // Admin role check
            if ("Admin".equalsIgnoreCase(signupRequest.getRole()) && !"admin".equals(signupRequest.getUsername())) {
                logger.warn("Attempt to register non-'admin' user with Admin role denied.");
                return ResponseEntity.badRequest().body(Map.of("message", "Admin role can only be assigned to username 'admin'"));
            }

            if (userRepository.existsByUsername(signupRequest.getUsername())) {
                logger.warn("Username {} is already taken", signupRequest.getUsername());
                // Use Map for consistent error structure
                return ResponseEntity.badRequest().body(Map.of("message", "Username is already taken!"));
            }

            if (userRepository.existsByEmail(signupRequest.getEmail())) {
                logger.warn("Email {} is already in use", signupRequest.getEmail());
                // Use Map for consistent error structure
                return ResponseEntity.badRequest().body(Map.of("message", "Email is already in use!"));
            }

            User user = new User();
            user.setUsername(signupRequest.getUsername());
            user.setEmail(signupRequest.getEmail());
            user.setPassword(passwordEncoder.encode(signupRequest.getPassword()));
            user.setFirstName(signupRequest.getFirstName());
            user.setLastName(signupRequest.getLastName());
            user.setPhone(signupRequest.getPhone());

            // Determine role name based on input
            String roleName;
            switch (signupRequest.getRole().toUpperCase()) {
                case "ADMIN":
                    roleName = "ROLE_ADMIN";
                    break;
                case "PRODUCT MANAGER":
                    roleName = "ROLE_PRODUCT_MANAGER";
                    break;
                case "ORDER MANAGER":
                    roleName = "ROLE_ORDER_MANAGER";
                    break;
                case "CUSTOMER":
                default:
                    // Map Customer to ROLE_USER if ROLE_CUSTOMER doesn't exist or isn't standard
                    roleName = "ROLE_USER"; 
                    break;
            }

            // Find and assign the selected role
            final String finalRoleName = roleName; // Need final variable for lambda
            Role userRole = roleRepository.findByName(finalRoleName)
                    .orElseGet(() -> {
                        logger.warn("Role {} not found, creating it", finalRoleName);
                        Role newRole = new Role();
                        newRole.setName(finalRoleName);
                        // Add description based on role
                        String description = switch (finalRoleName) {
                            case "ROLE_ADMIN" -> "Administrator role with full access";
                            case "ROLE_PRODUCT_MANAGER" -> "Manages product listings";
                            case "ROLE_ORDER_MANAGER" -> "Manages customer orders";
                            case "ROLE_USER" -> "Regular customer role";
                            default -> "Default user role";
                        };
                        newRole.setDescription(description);
                        return roleRepository.save(newRole);
                    });

            Set<Role> roles = new HashSet<>();
            roles.add(userRole);
            user.setRoles(roles);
            
            logger.info("Saving new user: {} with role {}", user.getUsername(), finalRoleName);
            userRepository.save(user);

            // Authenticate the user after registration
            try {
                Authentication authentication = authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(signupRequest.getUsername(), signupRequest.getPassword()));

                SecurityContextHolder.getContext().setAuthentication(authentication);
                String jwt = tokenProvider.generateToken(authentication);

                logger.info("User {} successfully registered and authenticated", user.getUsername());
                return ResponseEntity.ok(new JwtResponse(jwt, "Bearer", user.getUsername(), user.getEmail()));
            } catch (Exception e) {
                logger.error("Error authenticating user after registration: {}", e.getMessage());
                throw e;
            }
        } catch (Exception e) {
            logger.error("Error during user registration: {}", e.getMessage(), e);
            // Return structured error
            return ResponseEntity.badRequest()
                .body(Map.of("message", "Error during registration: " + e.getMessage()));
        }
    }
    
    @PostMapping("/customer-signup")
    public ResponseEntity<?> registerCustomer(@Valid @RequestBody CustomerSignupRequest signupRequest) {
        try {
            logger.info("Processing customer signup request for email: {} with order ID: {}", 
                signupRequest.getEmail(), signupRequest.getOrderId());
            
            User user;
            boolean isNewUser = false;
            
            // Check if user with this email already exists
            Optional<User> existingUser = userRepository.findByEmail(signupRequest.getEmail());
            
            if (existingUser.isPresent()) {
                user = existingUser.get();
                // Check if this is a guest user (with placeholder password)
                if ("GUEST_USER_NO_LOGIN".equals(user.getPassword())) {
                    logger.info("Converting guest user to registered user: {}", user.getUsername());
                    // Update the guest user with real password
                    user.setPassword(passwordEncoder.encode(signupRequest.getPassword()));
                    user.setFirstName(signupRequest.getFirstName());
                    user.setLastName(signupRequest.getLastName());
                    user.setPhone(signupRequest.getPhone());
                } else {
                    // Real user with password already exists
                    logger.warn("Email {} is already in use by a registered user", signupRequest.getEmail());
                    return ResponseEntity.badRequest().body(Map.of("message", "Email is already in use by a registered account!"));
                }
            } else {
                // Create new user
                isNewUser = true;
                
                // Generate a unique username based on email
                String baseUsername = signupRequest.getEmail().split("@")[0];
                String username = baseUsername;
                int counter = 1;
                
                // Ensure username is unique
                while (userRepository.existsByUsername(username)) {
                    username = baseUsername + counter++;
                }
                logger.debug("Generated unique username: {}", username);
                
                user = new User();
                user.setUsername(username);
                user.setEmail(signupRequest.getEmail());
                user.setPassword(passwordEncoder.encode(signupRequest.getPassword()));
                user.setFirstName(signupRequest.getFirstName());
                user.setLastName(signupRequest.getLastName());
                user.setPhone(signupRequest.getPhone());
                
                // Assign customer role
                Role userRole = roleRepository.findByName("ROLE_USER")
                        .orElseGet(() -> {
                            logger.warn("Role ROLE_USER not found, creating it");
                            Role newRole = new Role();
                            newRole.setName("ROLE_USER");
                            newRole.setDescription("Regular customer role");
                            return roleRepository.save(newRole);
                        });
                
                Set<Role> roles = new HashSet<>();
                roles.add(userRole);
                user.setRoles(roles);
                logger.debug("Assigned role: {}", userRole.getName());
            }
            
            // Save the user
            user = userRepository.save(user);
            logger.info("Successfully saved user with ID: {}", user.getId());
            
            // If there's an order to link and it's not already linked to this user
            if (signupRequest.getOrderId() != null) {
                Optional<Order> orderOpt = orderRepository.findById(signupRequest.getOrderId());
                if (orderOpt.isPresent()) {
                    Order order = orderOpt.get();
                    
                    // Verify email matches
                    if (order.getEmail().equals(signupRequest.getEmail())) {
                        // Link order to user if not already linked
                        if (order.getUser() == null || !order.getUser().getId().equals(user.getId())) {
                            order.setUser(user);
                            orderRepository.save(order);
                            logger.info("Linked order #{} to user {}", order.getId(), user.getUsername());
                        }
                        
                        // Save address if requested
                        if (signupRequest.getSaveAddress() != null && signupRequest.getSaveAddress()) {
                            CustomerAddressDTO addressDTO = new CustomerAddressDTO();
                            addressDTO.setUserId(user.getId());
                            addressDTO.setAddress(order.getAddress());
                            addressDTO.setApartment(order.getApartment());
                            addressDTO.setCity(order.getCity());
                            addressDTO.setPostalCode(order.getPostalCode());
                            addressDTO.setIsDefault(true);
                            
                            customerAddressService.createAddress(addressDTO);
                            logger.info("Saved shipping address for user {}", user.getUsername());
                        }
                    } else {
                        logger.warn("Email mismatch when linking order #{} to user {}", order.getId(), user.getUsername());
                    }
                }
            }
            
            // Authenticate the user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getUsername(), signupRequest.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = tokenProvider.generateToken(authentication);
            
            // Include more user details in response
            Map<String, Object> userData = new HashMap<>();
            userData.put("id", user.getId());
            userData.put("username", user.getUsername());
            userData.put("email", user.getEmail());
            userData.put("firstName", user.getFirstName());
            userData.put("lastName", user.getLastName());
            userData.put("phone", user.getPhone());
            
            Map<String, Object> response = new HashMap<>();
            response.put("token", jwt);
            response.put("tokenType", "Bearer");
            response.put("user", userData);
            
            String actionType = isNewUser ? "registered" : "converted from guest to registered user";
            logger.info("Customer {} successfully {} and authenticated", user.getUsername(), actionType);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error during customer registration: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(Map.of("message", "Error during registration: " + e.getMessage()));
        }
    }

    @GetMapping("/user")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        return ResponseEntity.ok(new JwtResponse(null, null, user.getUsername(), user.getEmail()));
    }

    /**
     * GET /api/auth/check-admin : Check if the current user has ADMIN role
     * 
     * @param authentication the current authentication
     * @return the ResponseEntity with status 200 (OK) and with body containing the admin status
     */
    @GetMapping("/check-admin")
    public ResponseEntity<?> checkAdminStatus(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            // Not authenticated
            return ResponseEntity.ok(Map.of("isAuthenticated", false, "isAdmin", false));
        }
        
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        
        // Log the check
        logger.info("Admin check for user {}: {}", authentication.getName(), isAdmin);
        
        return ResponseEntity.ok(Map.of(
            "isAuthenticated", true, 
            "isAdmin", isAdmin,
            "username", authentication.getName()
        ));
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Map<String, Object> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });
        logger.warn("Validation errors: {}", fieldErrors);
        // Return structured error including field errors
        return Map.of("message", "Validation failed", "errors", fieldErrors); 
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(RuntimeException.class)
    public Map<String, String> handleRuntimeExceptions(RuntimeException ex) {
        logger.error("Runtime exception: {}", ex.getMessage(), ex);
        return Map.of("message", ex.getMessage());
    }
} 