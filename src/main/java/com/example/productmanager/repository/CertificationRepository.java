package com.example.productmanager.repository;

import com.example.productmanager.model.Certification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CertificationRepository extends JpaRepository<Certification, Long> {
} 