-- Test script to verify inventory reduction triggers are working
-- Run this script directly in MySQL client to test the triggers

-- Select a product to test with (adjust the ID as needed)
SELECT id, name, inventory FROM products WHERE inventory > 0 LIMIT 1;
SET @test_product_id = 1; -- Change this to a product ID from your database

-- Record current inventory
SELECT @current_inventory := inventory FROM products WHERE id = @test_product_id;
SELECT CONCAT('Current inventory for product #', @test_product_id, ': ', @current_inventory) AS 'Info';

-- Create a test order
INSERT INTO orders (
    email, 
    first_name, 
    last_name, 
    address, 
    city, 
    postal_code, 
    phone, 
    payment_method, 
    subtotal, 
    shipping, 
    total, 
    status_id
) VALUES (
    'test@example.com', 
    'Test', 
    'User', 
    '123 Test St', 
    'Test City',
    '12345', 
    '123-456-7890', 
    'Credit Card', 
    100.00,
    10.00, 
    110.00, 
    1 -- Assuming 1 is 'Pending' status ID
);

-- Get the new order ID
SET @test_order_id = LAST_INSERT_ID();
SELECT CONCAT('Created test order #', @test_order_id) AS 'Info';

-- Create an order item with quantity = 1
INSERT INTO order_items (
    order_id, 
    product_id, 
    product_name, 
    price, 
    quantity
) 
SELECT 
    @test_order_id, 
    id, 
    name, 
    10.00, 
    1
FROM products 
WHERE id = @test_product_id;

-- Check inventory after order item creation (should be reduced by 1)
SELECT @new_inventory := inventory FROM products WHERE id = @test_product_id;
SELECT 
    CONCAT('Previous inventory: ', @current_inventory) AS 'Before',
    CONCAT('Current inventory: ', @new_inventory) AS 'After',
    CASE 
        WHEN @new_inventory = @current_inventory - 1 THEN 'TRIGGER WORKING CORRECTLY'
        ELSE 'TRIGGER NOT WORKING'
    END AS 'Result';

-- Clean up the test data (comment these out if you want to keep the test data)
DELETE FROM order_items WHERE order_id = @test_order_id;
DELETE FROM orders WHERE id = @test_order_id;

-- Check that inventory has been restored after deletion
SELECT @final_inventory := inventory FROM products WHERE id = @test_product_id;
SELECT 
    CONCAT('Final inventory: ', @final_inventory) AS 'After Cleanup',
    CASE 
        WHEN @final_inventory = @current_inventory THEN 'DELETE TRIGGER WORKING CORRECTLY'
        ELSE 'DELETE TRIGGER NOT WORKING'
    END AS 'Delete Result'; 