package com.example.productmanager.service;

import com.example.productmanager.model.*;
import com.example.productmanager.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.hibernate.Hibernate;
import com.example.productmanager.dto.ProductDTO;
import com.example.productmanager.dto.SpecificationDTO;
import com.example.productmanager.dto.NutritionalInfoDTO;
import com.example.productmanager.dto.WeightOptionDTO;
import com.example.productmanager.model.ProductFeature;
import com.example.productmanager.model.ProductSpecification;
import com.example.productmanager.model.ProductNutritionalInfo;
import com.example.productmanager.model.ProductWeightOption;
import com.example.productmanager.model.Certification;
import com.example.productmanager.repository.ProductFeatureRepository;
import com.example.productmanager.repository.ProductSpecificationRepository;
import com.example.productmanager.repository.ProductNutritionalInfoRepository;
import com.example.productmanager.repository.ProductWeightOptionRepository;
import com.example.productmanager.repository.CertificationRepository;
import jakarta.persistence.EntityNotFoundException;
import com.example.productmanager.model.Category;
import com.example.productmanager.repository.CategoryRepository;
import java.util.stream.Collectors;
import java.util.HashSet;
import java.math.BigDecimal;
import java.util.ArrayList;

@Service
public class ProductService {
    private final Logger logger = LoggerFactory.getLogger(ProductService.class);

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CertificationRepository certificationRepository;

    @Autowired
    private ProductFeatureRepository featureRepository;

    @Autowired
    private ProductSpecificationRepository specificationRepository;

    @Autowired
    private ProductNutritionalInfoRepository nutritionalInfoRepository;

    @Autowired
    private ProductWeightOptionRepository weightOptionRepository;

    @Transactional(readOnly = true)
    public List<Product> getAllProducts() {
        try {
            logger.info("Retrieving all products from repository");
            List<Product> products = productRepository.findAll();
            
            // Initialize all lazy-loaded collections to prevent LazyInitializationException
            for (Product product : products) {
                try {
                    // Initialize category
                    if (product.getCategory() != null) {
                        Hibernate.initialize(product.getCategory());
                    }
                    
                    // Initialize nutritional info
                    if (product.getNutritionalInfo() != null) {
                        Hibernate.initialize(product.getNutritionalInfo());
                    }
                    
                    // Initialize collections
                    Hibernate.initialize(product.getFeatures());
                    Hibernate.initialize(product.getSpecifications());
                    Hibernate.initialize(product.getWeightOptions());
                    Hibernate.initialize(product.getCertifications());
                    
                } catch (Exception e) {
                    logger.warn("Error initializing product {}: {}", product.getId(), e.getMessage());
                }
            }
            
            logger.info("Successfully retrieved {} products", products.size());
            return products;
        } catch (Exception e) {
            logger.error("Error retrieving products from repository: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve products: " + e.getMessage(), e);
        }
    }

    public Product saveProduct(Product product, MultipartFile image) throws IOException {
        try {
            // Set category if categoryId is provided
            if (product.getCategory() != null && product.getCategory().getId() != null) {
                Optional<Category> category = categoryRepository.findById(product.getCategory().getId());
                if (category.isPresent()) {
                    product.setCategory(category.get());
                } else {
                    logger.warn("Category not found with id: {}", product.getCategory().getId());
                }
            }

            // Save first to get the ID
            Product savedProduct = productRepository.save(product);

            if (image != null && !image.isEmpty()) {
                savedProduct.setImageData(image.getBytes());
                savedProduct.setImageUrl("/api/products/" + savedProduct.getId() + "/image");
                savedProduct = productRepository.save(savedProduct); // Save again with image
            }

            return savedProduct;
        } catch (Exception e) {
            logger.error("Error saving product", e);
            throw e;
        }
    }

    @Transactional
    public void deleteProduct(Long id) {
        try {
            // First, get the product to ensure it exists
            Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

            // Delete all related specifications first
            specificationRepository.deleteByProductId(id);
            
            // Delete all related features
            featureRepository.deleteByProductId(id);
            
            // Delete all weight options
            weightOptionRepository.deleteByProductId(id);
            
            // Delete nutritional info if exists
            nutritionalInfoRepository.deleteByProductId(id);
            
            // Clear certifications (this is a many-to-many relationship)
            product.getCertifications().clear();
            productRepository.save(product);
            
            // Now delete the product
            productRepository.deleteById(id);
        } catch (Exception e) {
            logger.error("Error deleting product with id: " + id, e);
            throw new RuntimeException("Error deleting product: " + e.getMessage());
        }
    }

    public Optional<Product> getProduct(Long id) {
        return productRepository.findById(id);
    }

    public List<Product> getProductsByCategory(Long categoryId) {
        return productRepository.findByCategoryId(categoryId);
    }

    public List<Product> getNewArrivals() {
        return productRepository.findByIsNewArrivalTrue();
    }

    public List<Product> getBestSellers() {
        return productRepository.findByIsBestSellerTrue();
    }

    // New methods for managing product features
    public List<ProductFeature> getProductFeatures(Long productId) {
        return featureRepository.findByProductId(productId);
    }

    public ProductFeature addProductFeature(Long productId, String feature) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new RuntimeException("Product not found"));
        
        ProductFeature productFeature = new ProductFeature();
        productFeature.setProduct(product);
        productFeature.setFeature(feature);
        
        return featureRepository.save(productFeature);
    }

    public void deleteProductFeature(Long featureId) {
        featureRepository.deleteById(featureId);
    }

    // New methods for managing product specifications
    public List<ProductSpecification> getProductSpecifications(Long productId) {
        return specificationRepository.findByProductId(productId);
    }

    public ProductSpecification addProductSpecification(Long productId, String specName, String specValue) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new RuntimeException("Product not found"));
        
        ProductSpecification specification = new ProductSpecification();
        specification.setProduct(product);
        specification.setSpecName(specName);
        specification.setSpecValue(specValue);
        
        return specificationRepository.save(specification);
    }

    public void deleteProductSpecification(Long specificationId) {
        specificationRepository.deleteById(specificationId);
    }

    // New methods for managing product certifications
    public Set<Certification> getProductCertifications(Long productId) {
        return productRepository.findById(productId)
            .map(Product::getCertifications)
            .orElseThrow(() -> new RuntimeException("Product not found"));
    }

    public void addProductCertification(Long productId, Long certificationId) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new RuntimeException("Product not found"));
        
        Certification certification = certificationRepository.findById(certificationId)
            .orElseThrow(() -> new RuntimeException("Certification not found"));
        
        product.getCertifications().add(certification);
        productRepository.save(product);
    }

    public void removeProductCertification(Long productId, Long certificationId) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new RuntimeException("Product not found"));
        
        product.getCertifications().removeIf(c -> c.getId().equals(certificationId));
        productRepository.save(product);
    }

    // Methods for managing nutritional info
    public Optional<ProductNutritionalInfo> getProductNutritionalInfo(Long productId) {
        return nutritionalInfoRepository.findByProductId(productId);
    }

    @Transactional
    public ProductNutritionalInfo saveProductNutritionalInfo(Long productId, ProductNutritionalInfo nutritionalInfo) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new RuntimeException("Product not found"));
        
        // Check if there's an existing nutritional info for this product
        Optional<ProductNutritionalInfo> existingInfo = nutritionalInfoRepository.findByProductId(productId);
        
        if (existingInfo.isPresent()) {
            // Update existing record
            ProductNutritionalInfo existing = existingInfo.get();
            
            // Copy properties from the incoming nutritionalInfo to the existing one
            existing.setServingSize(nutritionalInfo.getServingSize());
            existing.setServingsPerContainer(nutritionalInfo.getServingsPerContainer());
            existing.setCalories(nutritionalInfo.getCalories());
            existing.setTotalFat(nutritionalInfo.getTotalFat());
            existing.setSaturatedFat(nutritionalInfo.getSaturatedFat());
            existing.setTransFat(nutritionalInfo.getTransFat());
            existing.setCholesterol(nutritionalInfo.getCholesterol());
            existing.setSodium(nutritionalInfo.getSodium());
            existing.setTotalCarbohydrates(nutritionalInfo.getTotalCarbohydrates());
            existing.setDietaryFiber(nutritionalInfo.getDietaryFiber());
            existing.setSugars(nutritionalInfo.getSugars());
            existing.setProtein(nutritionalInfo.getProtein());
            existing.setVitaminA(nutritionalInfo.getVitaminA());
            existing.setVitaminC(nutritionalInfo.getVitaminC());
            existing.setCalcium(nutritionalInfo.getCalcium());
            existing.setIron(nutritionalInfo.getIron());
            
            return nutritionalInfoRepository.save(existing);
        } else {
            // Create new record
            nutritionalInfo.setProduct(product);
            nutritionalInfo.setProductId(productId); // Explicitly set the ID
            return nutritionalInfoRepository.save(nutritionalInfo);
        }
    }

    public void deleteProductNutritionalInfo(Long productId) {
        nutritionalInfoRepository.deleteByProductId(productId);
    }

    // Methods for managing weight options
    public List<ProductWeightOption> getProductWeightOptions(Long productId) {
        return weightOptionRepository.findByProductId(productId);
    }

    public ProductWeightOption addProductWeightOption(Long productId, ProductWeightOption weightOption, MultipartFile packagingImage) throws IOException {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new RuntimeException("Product not found"));
        
        weightOption.setProduct(product);
        
        // First save to get the ID
        ProductWeightOption savedOption = weightOptionRepository.save(weightOption);
        
        if (packagingImage != null && !packagingImage.isEmpty()) {
            savedOption.setPackagingPhotoData(packagingImage.getBytes());
            savedOption.setPackagingPhoto("/api/products/" + productId + "/weight-options/" + savedOption.getId() + "/image");
            // Save again with the image data and URL
            savedOption = weightOptionRepository.save(savedOption);
        }
        
        return savedOption;
    }

    public void deleteProductWeightOption(Long weightOptionId) {
        weightOptionRepository.deleteById(weightOptionId);
    }
    
    public Optional<ProductWeightOption> getProductWeightOption(Long id) {
        return weightOptionRepository.findById(id);
    }

    public ProductDTO getProductDetails(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with id: " + productId));

        // Note: Product entity already loads Certifications, Features, Specifications, WeightOptions via Set
        // and NutritionalInfo via OneToOne. Ensure FetchType is EAGER or they are fetched explicitly if LAZY.
        // Based on Product.java, they are LAZY, so explicit fetching or join fetch query is better.
        // For simplicity here, we rely on LAZY loading being triggered by accessing the getters,
        // but in a real app, consider @EntityGraph or JOIN FETCH for performance.

        return convertToProductDTO(product);
    }

    private ProductDTO convertToProductDTO(Product product) {
        ProductDTO dto = new ProductDTO();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setImageUrl(product.getImageUrl());
        if (product.getCategory() != null) {
            dto.setCategoryId(product.getCategory().getId());
            dto.setCategoryName(product.getCategory().getName());
        }
        dto.setIsNewArrival(product.getIsNewArrival());
        dto.setIsBestSeller(product.getIsBestSeller());
        dto.setPricePerKg(product.getPricePerKg());
        dto.setHasDiscount(product.getHasDiscount());
        dto.setDiscountPercentage(product.getDiscountPercentage());
        dto.setInventory(product.getInventory());

        if (product.getCertifications() != null) {
            dto.setCertificationIds(product.getCertifications().stream()
                    .map(Certification::getId)
                    .collect(Collectors.toList()));
        } else {
            dto.setCertificationIds(new ArrayList<>());
        }

        if (product.getFeatures() != null) {
            dto.setFeatures(product.getFeatures().stream()
                    .map(ProductFeature::getFeature) // Assuming ProductFeature has getFeature()
                    .collect(Collectors.toList()));
        } else {
            dto.setFeatures(new ArrayList<>());
        }
        
        if (product.getSpecifications() != null) {
            dto.setSpecifications(product.getSpecifications().stream()
                    .map(spec -> new SpecificationDTO(spec.getSpecName(), spec.getSpecValue())) // Assuming ProductSpecification has getSpecName() and getSpecValue()
                    .collect(Collectors.toList()));
        } else {
            dto.setSpecifications(new ArrayList<>());
        }

        ProductNutritionalInfo pni = product.getNutritionalInfo();
        if (pni != null) {
            NutritionalInfoDTO nutDto = new NutritionalInfoDTO();
            nutDto.setServingSize(pni.getServingSize());
            nutDto.setCalories(pni.getCalories());
            nutDto.setProtein(pni.getProtein());
            nutDto.setFat(pni.getTotalFat());
            nutDto.setCarbohydrates(pni.getTotalCarbohydrates());
            nutDto.setFiber(pni.getDietaryFiber());
            nutDto.setSugar(pni.getSugars());
            nutDto.setSodium(pni.getSodium());
            nutDto.setIron(pni.getIron());
            dto.setNutritionalInfo(nutDto);
        }

        if (product.getWeightOptions() != null) {
            dto.setWeightOptions(product.getWeightOptions().stream()
                    .map(wo -> new WeightOptionDTO(
                        wo.getId(),
                        String.valueOf(wo.getWeightValue()),
                        wo.getPrice(),
                        wo.getPackagingPhoto()
                    ))
                    .collect(Collectors.toList()));
        } else {
            dto.setWeightOptions(new ArrayList<>());
        }

        return dto;
    }

    @Transactional
    public ProductDTO updateProductDetails(Long productId, ProductDTO productDTO) {
        logger.info("Request to update Product : {}", productDTO);

        // 1. Fetch the existing product
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with id: " + productId));

        // --- Update Product Core Fields --- 
        product.setName(productDTO.getName());
        product.setDescription(productDTO.getDescription());
        product.setImageUrl(productDTO.getImageUrl()); // Handle image data update separately if needed
        product.setIsNewArrival(productDTO.getIsNewArrival());
        product.setIsBestSeller(productDTO.getIsBestSeller());
        product.setPricePerKg(productDTO.getPricePerKg());
        product.setHasDiscount(productDTO.getHasDiscount());
        product.setDiscountPercentage(productDTO.getDiscountPercentage());
        product.setInventory(productDTO.getInventory());

        // --- Update Category --- 
        if (productDTO.getCategoryId() != null) {
            Category category = categoryRepository.findById(productDTO.getCategoryId())
                    .orElseThrow(() -> new EntityNotFoundException("Category not found with id: " + productDTO.getCategoryId()));
            product.setCategory(category);
        } else {
            product.setCategory(null);
        }

        // --- Update Certifications (Fetch, Clear, Add) --- 
        if (productDTO.getCertificationIds() != null) {
            Set<Certification> newCertifications = new HashSet<>();
            if (!productDTO.getCertificationIds().isEmpty()) {
                List<Certification> foundCertifications = certificationRepository.findAllById(productDTO.getCertificationIds());
                 if(foundCertifications.size() != productDTO.getCertificationIds().size()){
                     // Handle case where some certification IDs were not found
                     throw new EntityNotFoundException("One or more Certifications not found");
                 }
                newCertifications.addAll(foundCertifications);
            }
            product.getCertifications().clear(); // Important for @ManyToMany
            product.getCertifications().addAll(newCertifications);
        }

        // --- Update Features (Rely on CascadeType.ALL and orphanRemoval=true) --- 
        product.getFeatures().clear(); // Clear existing features managed by JPA
        if (productDTO.getFeatures() != null) {
            productDTO.getFeatures().forEach(featureText -> {
                ProductFeature feature = new ProductFeature();
                feature.setFeature(featureText);
                feature.setProduct(product);
                product.getFeatures().add(feature);
            });
        }

        // --- Update Specifications (Rely on CascadeType.ALL and orphanRemoval=true) --- 
        product.getSpecifications().clear(); // Clear existing specifications managed by JPA
        if (productDTO.getSpecifications() != null) {
            productDTO.getSpecifications().forEach(specDTO -> {
                ProductSpecification spec = new ProductSpecification();
                spec.setSpecName(specDTO.getSpecName());
                spec.setSpecValue(specDTO.getSpecValue());
                spec.setProduct(product);
                product.getSpecifications().add(spec);
            });
        }
        
        // --- Update Nutritional Info (Fetch/Create, Update) --- 
        if (productDTO.getNutritionalInfo() != null) {
            ProductNutritionalInfo pni = product.getNutritionalInfo();
            if (pni == null) {
                pni = new ProductNutritionalInfo();
                pni.setProduct(product); // Link to product
                // pni.setProductId(product.getId()); // Not needed if using @MapsId
                product.setNutritionalInfo(pni); // Set on product side
            }
            NutritionalInfoDTO nutDto = productDTO.getNutritionalInfo();
            pni.setServingSize(nutDto.getServingSize());
            pni.setCalories(nutDto.getCalories());
            pni.setTotalFat(nutDto.getFat());
            pni.setSodium(nutDto.getSodium());
            pni.setTotalCarbohydrates(nutDto.getCarbohydrates());
            pni.setDietaryFiber(nutDto.getFiber());
            pni.setSugars(nutDto.getSugar());
            pni.setProtein(nutDto.getProtein());
            pni.setIron(nutDto.getIron());
            // Update other fields as needed
            // No explicit save needed for pni if Product cascades save/update
        } else {
             // If DTO nutritional info is null, should we delete existing?
             // Depends on requirements. If using @OneToOne(orphanRemoval=true), setting product.setNutritionalInfo(null) would delete it.
             if (product.getNutritionalInfo() != null) {
                 nutritionalInfoRepository.delete(product.getNutritionalInfo()); // Explicit delete if orphanRemoval=false or not relying on it
                 product.setNutritionalInfo(null);
             }
        }

        // --- Update Weight Options (Rely on CascadeType.ALL and orphanRemoval=true) --- 
        product.getWeightOptions().clear(); // Clear existing weight options managed by JPA
        if (productDTO.getWeightOptions() != null) {
            productDTO.getWeightOptions().forEach(woDTO -> {
                ProductWeightOption wo = new ProductWeightOption();
                // wo.setId(woDTO.getId()); // Let DB generate ID for new options
                try {
                     // Assuming weightValue in DTO is like "500g", "1kg" - needs parsing
                     // For now, assuming it's just the numeric part as String, convert to Integer
                     wo.setWeightValue(Integer.parseInt(woDTO.getWeightValue())); 
                 } catch (NumberFormatException e) {
                     logger.error("Invalid weight format: {}", woDTO.getWeightValue());
                     // Handle error - maybe throw exception or skip this option
                     throw new IllegalArgumentException("Invalid weight format: " + woDTO.getWeightValue());
                 }
                wo.setPrice(woDTO.getPrice());
                wo.setPackagingPhoto(woDTO.getPackagingPhoto());
                wo.setProduct(product);
                product.getWeightOptions().add(wo);
            });
        }

        // 5. Save the updated product (cascades updates to related entities)
        Product updatedProduct = productRepository.save(product);
        
        // 6. Convert back to DTO to return the updated state
        // Use the existing convertToProductDTO, but it might cause N+1 if entities were lazily loaded and modified
        // Re-fetching or ensuring eager loading/join fetch during the update might be better
        // For simplicity, we'll call the existing converter.
        return convertToProductDTO(updatedProduct); 
    }
} 