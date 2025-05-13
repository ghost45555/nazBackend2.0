package com.example.productmanager.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "workers")
public class Worker {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String firstName;
    private String lastName;
    private String phone;
    private String username;
    private String password;
    private String role;
} 