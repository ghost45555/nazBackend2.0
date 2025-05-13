package com.example.productmanager.repository;

import com.example.productmanager.entity.Worker;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkerRepository extends JpaRepository<Worker, Long> {
    boolean existsByUsername(String username);
} 