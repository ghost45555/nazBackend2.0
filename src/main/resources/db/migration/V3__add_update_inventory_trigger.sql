-- Add trigger to prevent orders with insufficient inventory
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
END;

-- Add trigger to reduce product inventory when an order is placed
CREATE TRIGGER after_order_item_insert
AFTER INSERT ON order_items
FOR EACH ROW
BEGIN
    -- Update product inventory by reducing the ordered quantity
    UPDATE products
    SET inventory = inventory - NEW.quantity
    WHERE id = NEW.product_id;
END;

-- Add trigger to handle order item updates (if quantity changes)
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
END;

-- Add trigger to handle order item deletion
CREATE TRIGGER after_order_item_delete
AFTER DELETE ON order_items
FOR EACH ROW
BEGIN
    -- Restore inventory when an order item is deleted
    UPDATE products
    SET inventory = inventory + OLD.quantity
    WHERE id = OLD.product_id;
END; 