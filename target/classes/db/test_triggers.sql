-- Test script for inventory update triggers
-- This script is meant to be run manually for testing

-- 1. View current product inventory
SELECT id, name, inventory FROM products WHERE id = 1;

-- 2. Insert a test order with a known product (adjust product_id if needed)
-- First create the order
INSERT INTO orders (
    email, first_name, last_name, address, city, 
    postal_code, phone, payment_method, subtotal, 
    shipping, total, status_id
) VALUES (
    'test@example.com', 'Test', 'User', '123 Test St', 'Test City',
    '12345', '123-456-7890', 'Credit Card', 100.00,
    10.00, 110.00, 1
);

-- Store the last inserted order ID in a variable
-- Note: This is a MySQL feature and may not work directly in Spring Boot scripts
-- You may need to execute parts of this script separately in a MySQL client
SELECT @last_order_id := LAST_INSERT_ID();

-- 3. Try to add an order item with more quantity than available inventory
-- This should fail due to the before_order_item_insert trigger
/*
INSERT INTO order_items (
    order_id, product_id, product_name, product_image, 
    weight, price, quantity
) VALUES (
    @last_order_id, 1, 'Test Product', 'test.jpg',
    500, 50.00, 999999
);
*/

-- 4. Add a valid order item
INSERT INTO order_items (
    order_id, product_id, product_name, product_image, 
    weight, price, quantity
) VALUES (
    @last_order_id, 1, 'Test Product', 'test.jpg',
    500, 50.00, 2
);

-- 5. Check inventory after order is placed (should be reduced by 2)
SELECT id, name, inventory FROM products WHERE id = 1;

-- 6. Update the order item quantity and check inventory again
UPDATE order_items SET quantity = 3 WHERE order_id = @last_order_id;

-- 7. Check inventory after update (should be reduced by 1 more)
SELECT id, name, inventory FROM products WHERE id = 1;

-- 8. Delete the order item and check inventory
DELETE FROM order_items WHERE order_id = @last_order_id;

-- 9. Check inventory after deletion (should be restored)
SELECT id, name, inventory FROM products WHERE id = 1;

-- 10. Clean up test data
DELETE FROM orders WHERE id = @last_order_id; 