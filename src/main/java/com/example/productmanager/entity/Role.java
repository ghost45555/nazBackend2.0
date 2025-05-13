package com.example.productmanager.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "roles")
public class Role {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String name;  // This will store ROLE_PM or ROLE_OM
    
    @Column
    private String description;  // This will store the role description

    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    
    public String getName() { return name; }
    public String getDescription() { return description; }
} 