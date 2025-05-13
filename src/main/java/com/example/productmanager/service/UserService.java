package com.example.productmanager.service;

import com.example.productmanager.dto.WorkerCreateRequest;
import com.example.productmanager.entity.Role;
import com.example.productmanager.entity.User;
import com.example.productmanager.repository.RoleRepository;
import com.example.productmanager.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Transactional
    public User createWorker(WorkerCreateRequest request) {
        // Check if username or email already exists
        if (userRepository.existsByUsername(request.username)) {
            throw new RuntimeException("Username already exists");
        }
        if (userRepository.existsByEmail(request.email)) {
            throw new RuntimeException("Email already exists");
        }

        // Create new user
        User user = new User();
        user.setFirstName(request.firstName);
        user.setLastName(request.lastName);
        user.setEmail(request.email);
        user.setPhone(request.phone);
        user.setUsername(request.username);
        user.setPassword(passwordEncoder.encode(request.password));

        // Create or update role by name
        Role role = roleRepository.findByName(request.name)
                .orElseGet(() -> {
                    Role newRole = new Role();
                    newRole.setName(request.name); // ROLE_PM or ROLE_OM
                    return newRole;
                });
        role.setDescription(request.description);
        role = roleRepository.save(role);

        // Set role for user
        Set<Role> roles = new HashSet<>();
        roles.add(role);
        user.setRoles(roles);

        return userRepository.save(user);
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
} 