package com.example.productmanager.controller;

import com.example.productmanager.dto.WorkerCreateRequest;
import com.example.productmanager.entity.User;
import com.example.productmanager.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = {"http://localhost:5500", "http://127.0.0.1:5500"}, maxAge = 3600)
public class AdminController {
    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    @Autowired
    private UserService userService;

    @PostMapping("/users")
    public ResponseEntity<?> createWorker(@RequestBody WorkerCreateRequest request) {
        logger.info("Received request to create worker: {}", request);
        try {
            User worker = userService.createWorker(request);
            logger.info("Successfully created worker: {}", worker);
            return ResponseEntity.ok(worker);
        } catch (RuntimeException e) {
            logger.error("Error creating worker: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
