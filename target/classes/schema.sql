-- Product Nutritional Info
CREATE TABLE IF NOT EXISTS product_nutritional_info (
    product_id BIGINT PRIMARY KEY,
    serving_size VARCHAR(50),
    servings_per_container VARCHAR(50),
    calories VARCHAR(20),
    total_fat VARCHAR(20),
    saturated_fat VARCHAR(20),
    trans_fat VARCHAR(20),
    cholesterol VARCHAR(20),
    sodium VARCHAR(20),
    total_carbohydrates VARCHAR(20),
    dietary_fiber VARCHAR(20),
    sugars VARCHAR(20),
    protein VARCHAR(20),
    vitamin_a VARCHAR(20),
    vitamin_c VARCHAR(20),
    calcium VARCHAR(20),
    iron VARCHAR(20),
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

-- Product Weight Options
CREATE TABLE IF NOT EXISTS product_weight_options (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    weight_value INT NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    packaging_photo VARCHAR(255),
    packaging_photo_data LONGBLOB,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
); 