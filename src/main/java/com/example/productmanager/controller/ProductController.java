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
import java.math.BigDecimal;
import com.example.productmanager.repository.CategoryRepository;

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

    @Autowired
    private CategoryRepository categoryRepository;

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
            @RequestParam(value = "isFeatured", required = false) Boolean isFeatured,
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
            product.setIsFeatured(isFeatured != null ? isFeatured : false);
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

    @GetMapping("/featured")
    public List<Product> getFeaturedProducts() {
        return productService.getFeaturedProducts();
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

    /**
     * Updates an existing product feature
     *
     * @param featureId the ID of the feature to update
     * @param feature the new feature text
     * @return the ResponseEntity with status 200 (OK) and the updated feature
     */
    @PutMapping("/features/{featureId}")
    public ResponseEntity<?> updateProductFeature(
            @PathVariable Long featureId,
            @RequestParam String feature) {
        try {
            ProductFeature updatedFeature = productService.updateProductFeature(featureId, feature);
            return ResponseEntity.ok(updatedFeature);
        } catch (Exception e) {
            logger.error("Error updating feature with id: " + featureId, e);
            return ResponseEntity.internalServerError()
                .body("Error updating feature: " + e.getMessage());
        }
    }

    /**
     * Alternative endpoint for updating a product feature (for clients that don't support PUT)
     */
    @PostMapping("/features/{featureId}/update")
    public ResponseEntity<?> updateProductFeaturePost(
            @PathVariable Long featureId,
            @RequestParam String feature) {
        return updateProductFeature(featureId, feature);
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

    /**
     * Updates an existing product specification
     *
     * @param specificationId the ID of the specification to update
     * @param specName the new specification name
     * @param specValue the new specification value
     * @return the ResponseEntity with status 200 (OK) and the updated specification
     */
    @PutMapping("/specifications/{specificationId}")
    public ResponseEntity<?> updateProductSpecification(
            @PathVariable Long specificationId,
            @RequestParam String specName,
            @RequestParam String specValue) {
        try {
            ProductSpecification updatedSpec = productService.updateProductSpecification(specificationId, specName, specValue);
            return ResponseEntity.ok(updatedSpec);
        } catch (Exception e) {
            logger.error("Error updating specification with id: " + specificationId, e);
            return ResponseEntity.internalServerError()
                .body("Error updating specification: " + e.getMessage());
        }
    }

    /**
     * Alternative endpoint for updating a product specification (for clients that don't support PUT)
     */
    @PostMapping("/specifications/{specificationId}/update")
    public ResponseEntity<?> updateProductSpecificationPost(
            @PathVariable Long specificationId,
            @RequestParam String specName,
            @RequestParam String specValue) {
        return updateProductSpecification(specificationId, specName, specValue);
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
     * PUT /api/products/{productId}/weight-options/{optionId}/image : Updates the packaging photo for a product weight option.
     *
     * @param productId the ID of the product
     * @param optionId the ID of the weight option to update
     * @param packagingImage the new packaging image
     * @return the ResponseEntity with status 200 (OK) if successful, or error status
     */
    @PutMapping(value = "/{productId}/weight-options/{optionId}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateWeightOptionImage(
            @PathVariable Long productId,
            @PathVariable Long optionId,
            @RequestParam("packagingImage") MultipartFile packagingImage) {
        try {
            logger.info("Updating packaging photo for weight option ID: {} of product ID: {}", optionId, productId);
            
            ProductWeightOption updatedOption = productService.updateWeightOptionImage(productId, optionId, packagingImage);
            return ResponseEntity.ok(updatedOption);
        } catch (EntityNotFoundException e) {
            logger.error("Weight option not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IOException e) {
            logger.error("Error processing image for weight option ID: {}", optionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error processing image: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error updating weight option image for ID: {}", optionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error updating weight option image: " + e.getMessage());
        }
    }

    /**
     * Alternative endpoint that accepts POST method for updating weight option images.
     * This is provided for compatibility with clients that may not support PUT requests.
     */
    @PostMapping(value = "/{productId}/weight-options/{optionId}/update-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateWeightOptionImagePost(
            @PathVariable Long productId,
            @PathVariable Long optionId,
            @RequestParam("packagingImage") MultipartFile packagingImage) {
        try {
            logger.info("Updating packaging photo via POST for weight option ID: {} of product ID: {}", optionId, productId);
            
            ProductWeightOption updatedOption = productService.updateWeightOptionImage(productId, optionId, packagingImage);
            return ResponseEntity.ok(updatedOption);
        } catch (EntityNotFoundException e) {
            logger.error("Weight option not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IOException e) {
            logger.error("Error processing image for weight option ID: {}", optionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error processing image: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error updating weight option image for ID: {}", optionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error updating weight option image: " + e.getMessage());
        }
    }

    /**
     * PUT /api/products/{productId}/weight-options/{optionId} : Updates the weight and price for a product weight option.
     *
     * @param productId the ID of the product
     * @param optionId the ID of the weight option to update
     * @param weightOptionData the new weight option data containing weightValue and price
     * @return the ResponseEntity with status 200 (OK) if successful, or error status
     */
    @PutMapping(value = "/{productId}/weight-options/{optionId}")
    public ResponseEntity<?> updateWeightOption(
            @PathVariable Long productId,
            @PathVariable Long optionId,
            @RequestBody Map<String, Object> weightOptionData) {
        try {
            logger.info("Updating weight option info for ID: {} of product ID: {}", optionId, productId);
            logger.debug("Weight option data: {}", weightOptionData);
            
            // Extract weightValue and price from the request body
            Integer weightValue = null;
            BigDecimal price = null;
            
            if (weightOptionData.containsKey("weightValue")) {
                if (weightOptionData.get("weightValue") instanceof Integer) {
                    weightValue = (Integer) weightOptionData.get("weightValue");
                } else if (weightOptionData.get("weightValue") instanceof String) {
                    weightValue = Integer.parseInt((String) weightOptionData.get("weightValue"));
                } else if (weightOptionData.get("weightValue") instanceof Number) {
                    weightValue = ((Number) weightOptionData.get("weightValue")).intValue();
                }
            }
            
            if (weightOptionData.containsKey("price")) {
                if (weightOptionData.get("price") instanceof BigDecimal) {
                    price = (BigDecimal) weightOptionData.get("price");
                } else if (weightOptionData.get("price") instanceof String) {
                    price = new BigDecimal((String) weightOptionData.get("price"));
                } else if (weightOptionData.get("price") instanceof Number) {
                    price = BigDecimal.valueOf(((Number) weightOptionData.get("price")).doubleValue());
                }
            }
            
            if (weightValue == null || price == null) {
                return ResponseEntity.badRequest().body("Weight value and price are required");
            }
            
            ProductWeightOption updatedOption = productService.updateWeightOptionInfo(productId, optionId, weightValue, price);
            return ResponseEntity.ok(updatedOption);
        } catch (EntityNotFoundException e) {
            logger.error("Weight option not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            logger.error("Error updating weight option info for ID: {}", optionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error updating weight option info: " + e.getMessage());
        }
    }

    /**
     * Alternative endpoint that accepts POST method for updating weight option info.
     * This is provided for compatibility with clients that may not support PUT requests.
     */
    @PostMapping(value = "/{productId}/weight-options/{optionId}/update-info")
    public ResponseEntity<?> updateWeightOptionInfoPost(
            @PathVariable Long productId,
            @PathVariable Long optionId,
            @RequestBody Map<String, Object> weightOptionData) {
        logger.info("Updating weight option info via POST for ID: {} of product ID: {}", optionId, productId);
        return updateWeightOption(productId, optionId, weightOptionData);
    }

    /**
     * POST /api/products/{productId}/update-image : Updates the main product image.
     *
     * @param productId the ID of the product
     * @param image the new product image
     * @return the ResponseEntity with status 200 (OK) if successful, or error status
     */
    @PostMapping(value = "/{productId}/update-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateProductImage(
            @PathVariable Long productId,
            @RequestParam("image") MultipartFile image) {
        try {
            logger.info("Updating main image for product ID: {}", productId);
            
            Product updatedProduct = productService.updateProductImage(productId, image);
            return ResponseEntity.ok(updatedProduct);
        } catch (EntityNotFoundException e) {
            logger.error("Product not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IOException e) {
            logger.error("Error processing image for product ID: {}", productId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error processing image: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error updating product image for ID: {}", productId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error updating product image: " + e.getMessage());
        }
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

    /**
     * POST /api/products/featured : Update featured products (new arrivals and best sellers)
     * This endpoint is called when a product is marked as a new arrival or best seller
     */
    @PostMapping("/featured")
    public ResponseEntity<?> updateFeaturedProducts() {
        try {
            logger.info("Updating featured products");
            // No specific implementation needed - this endpoint is just to confirm the action
            return ResponseEntity.ok(Map.of("message", "Featured products updated successfully"));
        } catch (Exception e) {
            logger.error("Error updating featured products", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error updating featured products: " + e.getMessage());
        }
    }

    /**
     * POST /api/products/{productId}/update-basic-info : Updates the basic information of a product.
     *
     * @param productId the ID of the product
     * @param productData the product data containing name, description, pricePerKg, inventory, etc.
     * @return the ResponseEntity with status 200 (OK) if successful, or error status
     */
    @PostMapping("/{productId}/update-basic-info")
    public ResponseEntity<?> updateProductBasicInfo(
            @PathVariable Long productId,
            @RequestBody Map<String, Object> productData) {
        try {
            logger.info("Updating basic info for product ID: {}", productId);
            logger.debug("Product data: {}", productData);
            
            // Find the product
            Product product = productService.getProduct(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with id: " + productId));
            
            // Update fields if provided
            if (productData.containsKey("name")) {
                product.setName((String) productData.get("name"));
            }
            
            if (productData.containsKey("description")) {
                product.setDescription((String) productData.get("description"));
            }
            
            if (productData.containsKey("pricePerKg")) {
                Object priceObj = productData.get("pricePerKg");
                BigDecimal price;
                
                if (priceObj instanceof BigDecimal) {
                    price = (BigDecimal) priceObj;
                } else if (priceObj instanceof String) {
                    price = new BigDecimal((String) priceObj);
                } else if (priceObj instanceof Number) {
                    price = BigDecimal.valueOf(((Number) priceObj).doubleValue());
                } else {
                    return ResponseEntity.badRequest().body("Invalid price format");
                }
                
                product.setPricePerKg(price);
            }
            
            if (productData.containsKey("inventory")) {
                Object inventoryObj = productData.get("inventory");
                Integer inventory;
                
                if (inventoryObj instanceof Integer) {
                    inventory = (Integer) inventoryObj;
                } else if (inventoryObj instanceof String) {
                    inventory = Integer.parseInt((String) inventoryObj);
                } else if (inventoryObj instanceof Number) {
                    inventory = ((Number) inventoryObj).intValue();
                } else {
                    return ResponseEntity.badRequest().body("Invalid inventory format");
                }
                
                product.setInventory(inventory);
            }
            
            if (productData.containsKey("categoryId")) {
                Object categoryIdObj = productData.get("categoryId");
                Long categoryId;
                
                if (categoryIdObj instanceof Long) {
                    categoryId = (Long) categoryIdObj;
                } else if (categoryIdObj instanceof Integer) {
                    categoryId = ((Integer) categoryIdObj).longValue();
                } else if (categoryIdObj instanceof String) {
                    categoryId = Long.parseLong((String) categoryIdObj);
                } else if (categoryIdObj instanceof Number) {
                    categoryId = ((Number) categoryIdObj).longValue();
                } else {
                    return ResponseEntity.badRequest().body("Invalid category ID format");
                }
                
                Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new EntityNotFoundException("Category not found with id: " + categoryId));
                product.setCategory(category);
            }
            
            if (productData.containsKey("isNewArrival")) {
                product.setIsNewArrival((Boolean) productData.get("isNewArrival"));
            }
            
            if (productData.containsKey("isBestSeller")) {
                product.setIsBestSeller((Boolean) productData.get("isBestSeller"));
            }
            
            if (productData.containsKey("isFeatured")) {
                product.setIsFeatured((Boolean) productData.get("isFeatured"));
            }
            
            if (productData.containsKey("hasDiscount")) {
                product.setHasDiscount((Boolean) productData.get("hasDiscount"));
            }
            
            if (productData.containsKey("discountPercentage")) {
                Object discountObj = productData.get("discountPercentage");
                Integer discount;
                
                if (discountObj instanceof Integer) {
                    discount = (Integer) discountObj;
                } else if (discountObj instanceof String) {
                    discount = Integer.parseInt((String) discountObj);
                } else if (discountObj instanceof Number) {
                    discount = ((Number) discountObj).intValue();
                } else {
                    return ResponseEntity.badRequest().body("Invalid discount percentage format");
                }
                
                product.setDiscountPercentage(discount);
            }
            
            // Save the updated product
            Product updatedProduct = productService.saveProduct(product, null);
            return ResponseEntity.ok(updatedProduct);
        } catch (EntityNotFoundException e) {
            logger.error("Entity not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (NumberFormatException e) {
            logger.error("Invalid number format: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Invalid number format: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error updating product basic info for ID: {}", productId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error updating product info: " + e.getMessage());
        }
    }

    /**
     * PUT /api/products/{productId}/inventory : Updates only the inventory for a product.
     * @param productId the ID of the product
     * @param body JSON with { "inventory": newInventory }
     * @return the updated product or error
     */
    @PutMapping("/{productId}/inventory")
    public ResponseEntity<?> updateProductInventory(@PathVariable Long productId, @RequestBody Map<String, Object> body) {
        try {
            if (!body.containsKey("inventory")) {
                return ResponseEntity.badRequest().body("Missing 'inventory' field");
            }
            Integer newInventory;
            Object invObj = body.get("inventory");
            if (invObj instanceof Integer) {
                newInventory = (Integer) invObj;
            } else if (invObj instanceof String) {
                newInventory = Integer.parseInt((String) invObj);
            } else if (invObj instanceof Number) {
                newInventory = ((Number) invObj).intValue();
            } else {
                return ResponseEntity.badRequest().body("Invalid inventory format");
            }
            if (newInventory < 0) newInventory = 0;
            Product product = productService.getProduct(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with id: " + productId));
            product.setInventory(newInventory);
            Product updated = productService.saveProduct(product, null);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error updating inventory: " + e.getMessage());
        }
    }

    /**
     * Decrement inventory for a product by productId and quantity.
     * POST /api/products/{productId}/decrement-inventory
     * Body: { "quantity": 5 }
     */
    @PostMapping("/{productId}/decrement-inventory")
    public ResponseEntity<?> decrementInventory(@PathVariable Long productId, @RequestBody Map<String, Object> body) {
        try {
            if (!body.containsKey("quantity")) {
                return ResponseEntity.badRequest().body("Missing 'quantity' in request body");
            }
            int quantity = Integer.parseInt(body.get("quantity").toString());
            productService.decrementInventory(productId, quantity);
            return ResponseEntity.ok(Map.of(
                "message", "Inventory decremented successfully",
                "productId", productId,
                "quantity", quantity
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "error", e.getMessage()
            ));
        }
    }
} 