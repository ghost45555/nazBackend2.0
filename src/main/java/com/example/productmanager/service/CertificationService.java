package com.example.productmanager.service;

import com.example.productmanager.model.Certification;
import com.example.productmanager.repository.CertificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CertificationService {

    @Autowired
    private CertificationRepository certificationRepository;

    public List<Certification> getAllCertifications() {
        return certificationRepository.findAll();
    }

    public Certification saveCertification(Certification certification) {
        return certificationRepository.save(certification);
    }

    public void deleteCertification(Long id) {
        certificationRepository.deleteById(id);
    }
} 