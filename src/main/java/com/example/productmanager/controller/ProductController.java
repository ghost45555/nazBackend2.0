package com.example.productmanager.controller;

import com.example.productmanager.model.Product;
import com.example.productmanager.model.Category;
import com.example.productmanager.model.ProductFeature;
import com.example.productmanager.model.ProductSpecification;
import com.example.productmanager.model.Certification;
import com.example.productmanager.model.ProductNutritionalInfo;
import com.example.productmanager.model.ProductWeightOption;
import com.example.productmanager.service.ProductService;
import com.example.productmanager.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.productmanager.dto.ProductDTO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);

    @Autowired
    private ProductService productService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ObjectMapper objectMapper;

    @GetMapping
    public ResponseEntity<?> getAllProducts() {
        try {
            logger.info("Fetching all products");
            List<Product> products = productService.getAllProducts();
            logger.info("Found {} products", products.size());
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            logger.error("Error getting products: {}", e.getMessage(), e);
            String errorMessage = e.getMessage();
            if (e.getCause() != null) {
                errorMessage += " - Cause: " + e.getCause().getMessage();
            }
            return ResponseEntity.status(500)
                .body(Map.of(
                    "error", "Error retrieving products",
                    "message", errorMessage,
                    "type", e.getClass().getName(),
                    "timestamp", LocalDateTime.now().toString()
                ));
        }
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createProduct(
            @RequestParam("name") String name,
            @RequestParam("description") String description,
            @RequestParam("categoryId") Long categoryId,
            @RequestParam("pricePerKg") Double pricePerKg,
            @RequestParam("inventory") Integer inventory,
            @RequestParam(value = "isNewArrival", required = false) Boolean isNewArrival,
            @RequestParam(value = "isBestSeller", required = false) Boolean isBestSeller,
            @RequestParam(value = "hasDiscount", required = false) Boolean hasDiscount,
            @RequestParam(value = "discountPercentage", required = false) Integer discountPercentage,
            @RequestParam(value = "image", required = false) MultipartFile image) {
        try {
            logger.info("Creating product: name={}, categoryId={}, pricePerKg={}", name, categoryId, pricePerKg);
            
            Product product = new Product();
            product.setName(name);
            product.setDescription(description);
            product.setPricePerKg(java.math.BigDecimal.valueOf(pricePerKg));
            product.setInventory(inventory);
            product.setIsNewArrival(isNewArrival != null ? isNewArrival : false);
            product.setIsBestSeller(isBestSeller != null ? isBestSeller : false);
            product.setHasDiscount(hasDiscount != null ? hasDiscount : false);
            product.setDiscountPercentage(discountPercentage);

            // Set category
            Category category = new Category();
            category.setId(categoryId);
            product.setCategory(category);
            
            Product savedProduct = productService.saveProduct(product, image);
            return ResponseEntity.ok(savedProduct);
        } catch (Exception e) {
            logger.error("Error creating product", e);
            return ResponseEntity.internalServerError().body("Error creating product: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id) {
        try {
            productService.deleteProduct(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error deleting product with id: " + id, e);
            return ResponseEntity.internalServerError()
                .body("Error deleting product: " + e.getMessage());
        }
    }

    @GetMapping("/category/{categoryId}")
    public List<Product> getProductsByCategory(@PathVariable Long categoryId) {
        return productService.getProductsByCategory(categoryId);
    }

    @GetMapping("/new-arrivals")
    public List<Product> getNewArrivals() {
        return productService.getNewArrivals();
    }

    @GetMapping("/best-sellers")
    public List<Product> getBestSellers() {
        return productService.getBestSellers();
    }

    @GetMapping("/{id}/image")
    public ResponseEntity<byte[]> getProductImage(@PathVariable Long id) {
        return productService.getProduct(id)
                .<ResponseEntity<byte[]>>map(product -> ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .body(product.getImageData()))
                .orElse(ResponseEntity.notFound().build());
    }

    // Product Features endpoints
    @GetMapping("/{productId}/features")
    public List<ProductFeature> getProductFeatures(@PathVariable Long productId) {
        return productService.getProductFeatures(productId);
    }

    @PostMapping("/{productId}/features")
    public ProductFeature addProductFeature(
            @PathVariable Long productId,
            @RequestParam String feature) {
        return productService.addProductFeature(productId, feature);
    }

    @DeleteMapping("/features/{featureId}")
    public ResponseEntity<?> deleteProductFeature(@PathVariable Long featureId) {
        productService.deleteProductFeature(featureId);
        return ResponseEntity.ok().build();
    }

    // Product Specifications endpoints
    @GetMapping("/{productId}/specifications")
    public List<ProductSpecification> getProductSpecifications(@PathVariable Long productId) {
        return productService.getProductSpecifications(productId);
    }

    @PostMapping("/{productId}/specifications")
    public ProductSpecification addProductSpecification(
            @PathVariable Long productId,
            @RequestParam String specName,
            @RequestParam String specValue) {
        return productService.addProductSpecification(productId, specName, specValue);
    }

    @DeleteMapping("/specifications/{specificationId}")
    public ResponseEntity<?> deleteProductSpecification(@PathVariable Long specificationId) {
        productService.deleteProductSpecification(specificationId);
        return ResponseEntity.ok().build();
    }

    // Product Certifications endpoints
    @GetMapping("/{productId}/certifications")
    public Set<Certification> getProductCertifications(@PathVariable Long productId) {
        return productService.getProductCertifications(productId);
    }

    @PostMapping("/{productId}/certifications/{certificationId}")
    public ResponseEntity<?> addProductCertification(
            @PathVariable Long productId,
            @PathVariable Long certificationId) {
        productService.addProductCertification(productId, certificationId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{productId}/certifications/{certificationId}")
    public ResponseEntity<?> removeProductCertification(
            @PathVariable Long productId,
            @PathVariable Long certificationId) {
        productService.removeProductCertification(productId, certificationId);
        return ResponseEntity.ok().build();
    }

    // Product Nutritional Info endpoints
    @GetMapping("/{productId}/nutritional-info")
    public ResponseEntity<?> getProductNutritionalInfo(@PathVariable Long productId) {
        return productService.getProductNutritionalInfo(productId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{productId}/nutritional-info")
    public ResponseEntity<?> saveProductNutritionalInfo(
            @PathVariable Long productId,
            @RequestBody ProductNutritionalInfo nutritionalInfo) {
        try {
            logger.info("Saving nutritional info for product ID: {}", productId);
            logger.debug("Nutritional info data: {}", nutritionalInfo);
            
            ProductNutritionalInfo savedInfo = productService.saveProductNutritionalInfo(productId, nutritionalInfo);
            return ResponseEntity.ok(savedInfo);
        } catch (Exception e) {
            logger.error("Error saving nutritional info for product: " + productId, e);
            
            String detailedMessage = e.getMessage();
            if (e.getCause() != null) {
                detailedMessage += " - Cause: " + e.getCause().getMessage();
            }
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "error", "Error saving nutritional info: " + detailedMessage,
                    "timestamp", LocalDateTime.now().toString()
                ));
        }
    }

    @DeleteMapping("/{productId}/nutritional-info")
    public ResponseEntity<?> deleteProductNutritionalInfo(@PathVariable Long productId) {
        try {
            productService.deleteProductNutritionalInfo(productId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error deleting nutritional info for product: " + productId, e);
            return ResponseEntity.internalServerError()
                .body("Error deleting nutritional info: " + e.getMessage());
        }
    }

    // Product Weight Options endpoints
    @GetMapping("/{productId}/weight-options")
    public List<ProductWeightOption> getProductWeightOptions(@PathVariable Long productId) {
        return productService.getProductWeightOptions(productId);
    }

    @PostMapping(value="/{productId}/weight-options", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> addProductWeightOption(
            @PathVariable Long productId,
            @RequestParam("weightValue") Integer weightValue,
            @RequestParam("price") Double price,
            @RequestParam(value = "packagingImage", required = false) MultipartFile packagingImage) {
        try {
            ProductWeightOption weightOption = new ProductWeightOption();
            weightOption.setWeightValue(weightValue);
            weightOption.setPrice(java.math.BigDecimal.valueOf(price));
            
            ProductWeightOption savedOption = productService.addProductWeightOption(productId, weightOption, packagingImage);
            logger.info("Saved weight option: {}", savedOption);
            return ResponseEntity.ok(savedOption);
        } catch (Exception e) {
            logger.error("Error adding weight option for product: " + productId, e);
            return ResponseEntity.internalServerError()
                .body("Error adding weight option: " + e.getMessage());
        }
    }

    @DeleteMapping("/weight-options/{weightOptionId}")
    public ResponseEntity<?> deleteProductWeightOption(@PathVariable Long weightOptionId) {
        try {
            productService.deleteProductWeightOption(weightOptionId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error deleting weight option: " + weightOptionId, e);
            return ResponseEntity.internalServerError()
                .body("Error deleting weight option: " + e.getMessage());
        }
    }

    @GetMapping("/{productId}/weight-options/{optionId}/image")
    public ResponseEntity<byte[]> getWeightOptionImage(
            @PathVariable Long productId,
            @PathVariable Long optionId) {
        return productService.getProductWeightOption(optionId)
                .<ResponseEntity<byte[]>>map(option -> ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .body(option.getPackagingPhotoData()))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/products/admin/{productId} : Get full product details for editing by admin.
     *
     * @param productId the ID of the product to retrieve.
     * @return the ResponseEntity with status 200 (OK) and with body the productDTO, or with status 404 (Not Found).
     */
    @GetMapping("/admin/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductDTO> getProductDetailsForAdmin(@PathVariable Long productId) {
        logger.info("REST request to get Product details for admin : {}", productId);
        try {
            ProductDTO productDTO = productService.getProductDetails(productId);
            return ResponseEntity.ok(productDTO);
        } catch (EntityNotFoundException e) {
             logger.error("Product not found for id: {}", productId, e);
             return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error getting product details for id: {}", productId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); 
        }
    }

    /**
     * PUT /api/products/admin/{productId} : Updates an existing product.
     *
     * @param productId the ID of the product to update.
     * @param productDTO the productDTO to update.
     * @return the ResponseEntity with status 200 (OK) and with body the updated productDTO,
     * or with status 400 (Bad Request) if the productDTO is not valid,
     * or with status 404 (Not Found) if the product is not found,
     * or with status 500 (Internal Server Error) if the product couldn't be updated.
     */
    @PutMapping("/admin/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateProduct(@PathVariable Long productId, @Valid @RequestBody ProductDTO productDTO) {
        logger.info("REST request to update Product : {} with data : {}", productId, productDTO);

        // Basic validation: Check if ID in path matches ID in body (if present and not null)
        if (productDTO.getId() != null && !productDTO.getId().equals(productId)) {
            // Consider a more specific error response DTO
            return ResponseEntity.badRequest().body("ID in path does not match ID in request body.");
        }
         // Ensure the DTO ID is set for the service layer if not already
        if (productDTO.getId() == null) {
            productDTO.setId(productId);
        }

        try {
            ProductDTO updatedProduct = productService.updateProductDetails(productId, productDTO);
            return ResponseEntity.ok(updatedProduct);
        } catch (EntityNotFoundException e) {
            logger.error("Cannot update product. Entity not found: {}", e.getMessage());
            // Return 404 or a custom error response
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage()); 
        } catch (IllegalArgumentException e) {
             logger.error("Cannot update product. Invalid argument: {}", e.getMessage());
             return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("Error updating product with id {}: {}", productId, e.getMessage(), e);
            // Consider a more specific error response DTO
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal error updating product.");
        }
    }
} 