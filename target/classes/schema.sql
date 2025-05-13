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

-- Customer Addresses
CREATE TABLE IF NOT EXISTS customer_addresses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    address TEXT NOT NULL,
    apartment TEXT,
    city VARCHAR(100) NOT NULL,
    postal_code VARCHAR(20) NOT NULL,
    is_default BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Order Status Enum
CREATE TABLE IF NOT EXISTS order_statuses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255)
);

-- Orders Table
CREATE TABLE IF NOT EXISTS orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    email VARCHAR(100) NOT NULL,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    address TEXT NOT NULL,
    apartment TEXT,
    city VARCHAR(100) NOT NULL,
    postal_code VARCHAR(20) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    order_notes TEXT,
    payment_method VARCHAR(50) NOT NULL,
    subtotal DECIMAL(10, 2) NOT NULL,
    shipping DECIMAL(10, 2) NOT NULL,
    total DECIMAL(10, 2) NOT NULL,
    status_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (status_id) REFERENCES order_statuses(id)
);

-- Order Items Table
CREATE TABLE IF NOT EXISTS order_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT,
    product_name VARCHAR(255) NOT NULL,
    product_image VARCHAR(255),
    packaging_photo VARCHAR(255),
    weight INT,
    price DECIMAL(10, 2) NOT NULL,
    quantity INT NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE SET NULL
); 