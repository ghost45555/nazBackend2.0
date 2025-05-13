package com.example.productmanager.service;

import com.example.productmanager.dto.CustomerAddressDTO;
import com.example.productmanager.entity.CustomerAddress;
import com.example.productmanager.entity.User;
import com.example.productmanager.repository.CustomerAddressRepository;
import com.example.productmanager.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CustomerAddressService {

    @Autowired
    private CustomerAddressRepository customerAddressRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<CustomerAddressDTO> getAddressesByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));
        
        return customerAddressRepository.findByUser(user)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CustomerAddressDTO getAddressById(Long id) {
        return customerAddressRepository.findById(id)
                .map(this::convertToDTO)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public CustomerAddressDTO getDefaultAddressByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));
        
        return customerAddressRepository.findByUserAndIsDefaultTrue(user)
                .map(this::convertToDTO)
                .orElse(null);
    }

    @Transactional
    public CustomerAddressDTO createAddress(CustomerAddressDTO addressDTO) {
        User user = userRepository.findById(addressDTO.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + addressDTO.getUserId()));
        
        CustomerAddress address = convertToEntity(addressDTO);
        address.setUser(user);
        
        // If this is the first address or is set as default, reset other default addresses
        if (addressDTO.getIsDefault() || customerAddressRepository.findByUser(user).isEmpty()) {
            address.setIsDefault(true);
            resetDefaultAddresses(user);
        }
        
        CustomerAddress savedAddress = customerAddressRepository.save(address);
        return convertToDTO(savedAddress);
    }

    @Transactional
    public CustomerAddressDTO updateAddress(Long id, CustomerAddressDTO addressDTO) {
        CustomerAddress existingAddress = customerAddressRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Address not found with id: " + id));
        
        existingAddress.setAddress(addressDTO.getAddress());
        existingAddress.setApartment(addressDTO.getApartment());
        existingAddress.setCity(addressDTO.getCity());
        existingAddress.setPostalCode(addressDTO.getPostalCode());
        
        // Handle default address setting
        if (addressDTO.getIsDefault() && !existingAddress.getIsDefault()) {
            resetDefaultAddresses(existingAddress.getUser());
            existingAddress.setIsDefault(true);
        }
        
        CustomerAddress updatedAddress = customerAddressRepository.save(existingAddress);
        return convertToDTO(updatedAddress);
    }

    @Transactional
    public boolean deleteAddress(Long id) {
        CustomerAddress address = customerAddressRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Address not found with id: " + id));
        
        // Check if this is the only address for the user
        List<CustomerAddress> userAddresses = customerAddressRepository.findByUser(address.getUser());
        if (userAddresses.size() == 1) {
            // This is the only address, just delete it
            customerAddressRepository.delete(address);
        } else {
            // If we're deleting a default address, make another one default
            if (address.getIsDefault() && userAddresses.size() > 1) {
                // Find another address to make default
                Optional<CustomerAddress> anotherAddress = userAddresses.stream()
                        .filter(a -> !a.getId().equals(id))
                        .findFirst();
                
                if (anotherAddress.isPresent()) {
                    CustomerAddress newDefault = anotherAddress.get();
                    newDefault.setIsDefault(true);
                    customerAddressRepository.save(newDefault);
                }
            }
            
            // Delete the address
            customerAddressRepository.delete(address);
        }
        
        return true;
    }

    private void resetDefaultAddresses(User user) {
        List<CustomerAddress> addresses = customerAddressRepository.findByUser(user);
        for (CustomerAddress address : addresses) {
            if (address.getIsDefault()) {
                address.setIsDefault(false);
                customerAddressRepository.save(address);
            }
        }
    }

    private CustomerAddressDTO convertToDTO(CustomerAddress address) {
        return new CustomerAddressDTO(
                address.getId(),
                address.getUser().getId(),
                address.getAddress(),
                address.getApartment(),
                address.getCity(),
                address.getPostalCode(),
                address.getIsDefault()
        );
    }

    private CustomerAddress convertToEntity(CustomerAddressDTO addressDTO) {
        CustomerAddress address = new CustomerAddress();
        if (addressDTO.getId() != null) {
            address.setId(addressDTO.getId());
        }
        address.setAddress(addressDTO.getAddress());
        address.setApartment(addressDTO.getApartment());
        address.setCity(addressDTO.getCity());
        address.setPostalCode(addressDTO.getPostalCode());
        address.setIsDefault(addressDTO.getIsDefault());
        
        return address;
    }
} 