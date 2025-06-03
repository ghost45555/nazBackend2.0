-- Drop existing triggers if they exist to avoid conflicts
DROP TRIGGER IF EXISTS before_order_item_insert;
DROP TRIGGER IF EXISTS after_order_item_insert;
DROP TRIGGER IF EXISTS after_order_item_update;
DROP TRIGGER IF EXISTS after_order_item_delete;

-- Trigger to validate inventory before order item insertion
DELIMITER //
CREATE TRIGGER before_order_item_insert
BEFORE INSERT ON order_items
FOR EACH ROW
BEGIN
    DECLARE current_inventory INT;
    
    -- Get current inventory for the product
    SELECT inventory INTO current_inventory
    FROM products
    WHERE id = NEW.product_id;
    
    -- Check if enough inventory is available
    IF current_inventory < NEW.quantity THEN
        SIGNAL SQLSTATE '45000' 
        SET MESSAGE_TEXT = 'Insufficient inventory for product';
    END IF;
END//
DELIMITER ;

-- Trigger to reduce product inventory when an order item is inserted
DELIMITER //
CREATE TRIGGER after_order_item_insert
AFTER INSERT ON order_items
FOR EACH ROW
BEGIN
    -- Update product inventory by reducing the ordered quantity
    UPDATE products
    SET inventory = inventory - NEW.quantity
    WHERE id = NEW.product_id;
END//
DELIMITER ;

-- Trigger to adjust inventory when order item quantity is updated
DELIMITER //
CREATE TRIGGER after_order_item_update
AFTER UPDATE ON order_items
FOR EACH ROW
BEGIN
    -- If quantity changed, adjust inventory accordingly
    IF NEW.quantity != OLD.quantity THEN
        UPDATE products
        SET inventory = inventory + OLD.quantity - NEW.quantity
        WHERE id = NEW.product_id;
    END IF;
END//
DELIMITER ;

-- Trigger to restore inventory when an order item is deleted
DELIMITER //
CREATE TRIGGER after_order_item_delete
AFTER DELETE ON order_items
FOR EACH ROW
BEGIN
    -- Restore inventory when an order item is deleted
    UPDATE products
    SET inventory = inventory + OLD.quantity
    WHERE id = OLD.product_id;
END//
DELIMITER ; 