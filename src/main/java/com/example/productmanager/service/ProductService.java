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

@Service
public class ProductService {
    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);

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

    public List<Product> getAllProducts() {
        try {
            logger.info("Retrieving all products from repository");
            List<Product> products = productRepository.findAll();
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
        
        // Check if nutritional info already exists for this product
        ProductNutritionalInfo existingInfo = nutritionalInfoRepository.findByProductId(productId).orElse(null);
        if (existingInfo != null) {
            // Update existing record
            existingInfo.setServingSize(nutritionalInfo.getServingSize());
            existingInfo.setServingsPerContainer(nutritionalInfo.getServingsPerContainer());
            existingInfo.setCalories(nutritionalInfo.getCalories());
            existingInfo.setTotalFat(nutritionalInfo.getTotalFat());
            existingInfo.setSaturatedFat(nutritionalInfo.getSaturatedFat());
            existingInfo.setTransFat(nutritionalInfo.getTransFat());
            existingInfo.setCholesterol(nutritionalInfo.getCholesterol());
            existingInfo.setSodium(nutritionalInfo.getSodium());
            existingInfo.setTotalCarbohydrates(nutritionalInfo.getTotalCarbohydrates());
            existingInfo.setDietaryFiber(nutritionalInfo.getDietaryFiber());
            existingInfo.setSugars(nutritionalInfo.getSugars());
            existingInfo.setProtein(nutritionalInfo.getProtein());
            existingInfo.setVitaminA(nutritionalInfo.getVitaminA());
            existingInfo.setVitaminC(nutritionalInfo.getVitaminC());
            existingInfo.setCalcium(nutritionalInfo.getCalcium());
            existingInfo.setIron(nutritionalInfo.getIron());
            return nutritionalInfoRepository.save(existingInfo);
        } else {
            // Create new record
            // Set the product association on the nutritionalInfo side
            nutritionalInfo.setProduct(product);
            // Set the nutritionalInfo association on the product side
            product.setNutritionalInfo(nutritionalInfo);
            // Save the product, which should cascade to save the nutritionalInfo
            // due to @OneToOne(cascade = CascadeType.ALL) on Product.nutritionalInfo
            // and @MapsId on ProductNutritionalInfo.product
            Product savedProduct = productRepository.save(product);
            return savedProduct.getNutritionalInfo();
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
} 