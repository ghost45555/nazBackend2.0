package com.example.productmanager.controller;

import com.example.productmanager.dto.CustomerAddressDTO;
import com.example.productmanager.service.CustomerAddressService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
import com.example.productmanager.entity.User;
import com.example.productmanager.repository.UserRepository;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/addresses")
@CrossOrigin(origins = {"http://localhost:5500", "http://127.0.0.1:5500"}, maxAge = 3600)
public class CustomerAddressController {

    @Autowired
    private CustomerAddressService customerAddressService;
    
    @Autowired
    private UserRepository userRepository;

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ORDER_MANAGER') or @userRepository.findById(#userId).get().username == authentication.name")
    public ResponseEntity<List<CustomerAddressDTO>> getAddressesByUserId(@PathVariable Long userId) {
        try {
            List<CustomerAddressDTO> addresses = customerAddressService.getAddressesByUserId(userId);
            return ResponseEntity.ok(addresses);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ORDER_MANAGER') or @customerAddressService.getAddressById(#id).userId == @userRepository.findByUsername(authentication.name).get().id")
    public ResponseEntity<CustomerAddressDTO> getAddressById(@PathVariable Long id) {
        CustomerAddressDTO address = customerAddressService.getAddressById(id);
        return address != null
                ? ResponseEntity.ok(address)
                : ResponseEntity.notFound().build();
    }

    @GetMapping("/default/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ORDER_MANAGER') or @userRepository.findById(#userId).get().username == authentication.name")
    public ResponseEntity<CustomerAddressDTO> getDefaultAddressByUserId(@PathVariable Long userId) {
        try {
            CustomerAddressDTO address = customerAddressService.getDefaultAddressByUserId(userId);
            return address != null
                    ? ResponseEntity.ok(address)
                    : ResponseEntity.notFound().build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<?> createAddress(
            @RequestBody CustomerAddressDTO addressDTO,
            Authentication authentication) {
        try {
            // For security, if a non-admin user tries to create an address for someone else, return error
            if (!hasAdminRole(authentication)) {
                User currentUser = userRepository.findByUsername(authentication.getName())
                        .orElseThrow(() -> new UsernameNotFoundException("User not found"));
                
                if (!currentUser.getId().equals(addressDTO.getUserId())) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                            Map.of("message", "You are not authorized to create an address for another user"));
                }
            }
            
            CustomerAddressDTO createdAddress = customerAddressService.createAddress(addressDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdAddress);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ORDER_MANAGER') or @customerAddressService.getAddressById(#id).userId == @userRepository.findByUsername(authentication.name).get().id")
    public ResponseEntity<?> updateAddress(
            @PathVariable Long id,
            @RequestBody CustomerAddressDTO addressDTO) {
        try {
            CustomerAddressDTO updatedAddress = customerAddressService.updateAddress(id, addressDTO);
            return ResponseEntity.ok(updatedAddress);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ORDER_MANAGER') or @customerAddressService.getAddressById(#id).userId == @userRepository.findByUsername(authentication.name).get().id")
    public ResponseEntity<?> deleteAddress(@PathVariable Long id) {
        try {
            boolean deleted = customerAddressService.deleteAddress(id);
            return deleted
                    ? ResponseEntity.noContent().build()
                    : ResponseEntity.notFound().build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
    
    private boolean hasAdminRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> 
                    authority.getAuthority().equals("ROLE_ADMIN") || 
                    authority.getAuthority().equals("ROLE_ORDER_MANAGER"));
    }
} 