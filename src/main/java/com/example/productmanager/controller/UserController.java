package com.example.productmanager.controller;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = {"http://localhost:5500", "http://127.0.0.1:5500"}, maxAge = 3600)
public class UserController {
    // Other user-related endpoints can go here
} 