package com.example.productmanager.repository;

import com.example.productmanager.entity.CustomerAddress;
import com.example.productmanager.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerAddressRepository extends JpaRepository<CustomerAddress, Long> {
    List<CustomerAddress> findByUser(User user);
    Optional<CustomerAddress> findByUserAndIsDefaultTrue(User user);
} 