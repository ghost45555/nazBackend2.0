package com.example.productmanager.controller;

import com.example.productmanager.dto.JwtResponse;
import com.example.productmanager.dto.LoginRequest;
import com.example.productmanager.dto.SignupRequest;
import com.example.productmanager.entity.Role;
import com.example.productmanager.entity.User;
import com.example.productmanager.repository.RoleRepository;
import com.example.productmanager.repository.UserRepository;
import com.example.productmanager.security.JwtTokenProvider;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;
import java.util.HashMap;
import java.util.Map;
import org.springframework.validation.FieldError;

import java.util.HashSet;
import java.util.Set;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:5500", "http://127.0.0.1:5500"}, maxAge = 3600)
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

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