package com.example.productmanager.controller;

import com.example.productmanager.model.Certification;
import com.example.productmanager.repository.CertificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@RestController
@RequestMapping("/api/certifications")
public class CertificationController {
    private static final Logger logger = LoggerFactory.getLogger(CertificationController.class);

    @Autowired
    private CertificationRepository certificationRepository;

    /**
     * GET /api/certifications : Get all certifications
     * 
     * @return the ResponseEntity with status 200 (OK) and the list of certifications in body
     */
    @GetMapping
    public List<Certification> getAllCertifications() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        System.out.println("Current user: " + auth.getName());
        System.out.println("Authorities: " + auth.getAuthorities());
        return certificationRepository.findAll();
    }
    
    /**
     * POST /api/certifications : Create a new certification
     * 
     * @param certification the certification to create
     * @return the ResponseEntity with status 201 (Created) and with body the new certification
     */
    @PostMapping
    public ResponseEntity<Certification> createCertification(@RequestBody Certification certification) {
        logger.info("REST request to save Certification : {}", certification);
        Certification result = certificationRepository.save(certification);
        return ResponseEntity.ok(result);
    }
    
    /**
     * GET /api/certifications/{id} : Get the "id" certification
     * 
     * @param id the id of the certification to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the certification, or with status 404 (Not Found)
     */
    @GetMapping("/{id}")
    public ResponseEntity<Certification> getCertification(@PathVariable Long id) {
        logger.info("REST request to get Certification : {}", id);
        return certificationRepository.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * PUT /api/certifications/{id} : Updates an existing certification
     * 
     * @param id the id of the certification to update
     * @param certification the certification to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated certification,
     *         or with status 400 (Bad Request) if the id in the certification doesn't match the path variable
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateCertification(
            @PathVariable Long id, 
            @RequestBody Certification certification) {
        logger.info("REST request to update Certification : {}", certification);
        
        if (certification.getId() == null || !certification.getId().equals(id)) {
            return ResponseEntity.badRequest().body("Invalid ID");
        }
        
        try {
            // Ensure the certification exists
            certificationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Certification not found with id: " + id));
            
            Certification result = certificationRepository.save(certification);
            return ResponseEntity.ok(result);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            logger.error("Error updating certification: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error updating certification: " + e.getMessage());
        }
    }
    
    /**
     * POST /api/certifications/{id}/update : Updates an existing certification (alternative for clients that don't support PUT)
     */
    @PostMapping("/{id}/update")
    public ResponseEntity<?> updateCertificationPost(
            @PathVariable Long id, 
            @RequestBody Certification certification) {
        return updateCertification(id, certification);
    }
    
    /**
     * DELETE /api/certifications/{id} : Delete the "id" certification
     * 
     * @param id the id of the certification to delete
     * @return the ResponseEntity with status 204 (NO_CONTENT)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCertification(@PathVariable Long id) {
        logger.info("REST request to delete Certification : {}", id);
        certificationRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
} 