package com.example.productmanager.controller;

import com.example.productmanager.model.Certification;
import com.example.productmanager.service.CertificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/certifications")
public class CertificationController {
    private static final Logger logger = LoggerFactory.getLogger(CertificationController.class);

    @Autowired
    private CertificationService certificationService;

    @GetMapping
    public ResponseEntity<List<Certification>> getAllCertifications() {
        try {
            List<Certification> certifications = certificationService.getAllCertifications();
            return ResponseEntity.ok(certifications);
        } catch (Exception e) {
            logger.error("Error getting certifications", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping
    public ResponseEntity<Certification> createCertification(@RequestBody Certification certification) {
        try {
            Certification savedCertification = certificationService.saveCertification(certification);
            return ResponseEntity.ok(savedCertification);
        } catch (Exception e) {
            logger.error("Error creating certification", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCertification(@PathVariable Long id) {
        try {
            certificationService.deleteCertification(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error deleting certification", e);
            return ResponseEntity.internalServerError().build();
        }
    }
} 