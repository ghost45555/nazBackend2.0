# MySQL Triggers for Inventory Management

This document explains the MySQL triggers implemented for automatic inventory management when orders are placed.

## Overview

When a customer places an order at `http://localhost:8080/orders`, the following triggers are activated:

1. **before_order_item_insert** - Validates inventory before allowing an order item to be created
2. **after_order_item_insert** - Reduces inventory after an order item is successfully created
3. **after_order_item_update** - Adjusts inventory when an order item's quantity is changed
4. **after_order_item_delete** - Restores inventory when an order item is deleted

## Implementation Details

### Validation Trigger (before_order_item_insert)

This trigger runs BEFORE an order item is inserted and checks if there's enough inventory available. If not, it prevents the insertion with an error message.

```sql
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
```

### Inventory Reduction Trigger (after_order_item_insert)

This trigger runs AFTER an order item is successfully inserted and reduces the inventory by the ordered quantity.

```sql
CREATE TRIGGER after_order_item_insert
AFTER INSERT ON order_items
FOR EACH ROW
BEGIN
    -- Update product inventory by reducing the ordered quantity
    UPDATE products
    SET inventory = inventory - NEW.quantity
    WHERE id = NEW.product_id;
END;
```

### Quantity Change Trigger (after_order_item_update)

This trigger adjusts inventory when an order item's quantity is changed:

```sql
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
```

### Order Cancellation Trigger (after_order_item_delete)

This trigger restores inventory when an order item is deleted:

```sql
CREATE TRIGGER after_order_item_delete
AFTER DELETE ON order_items
FOR EACH ROW
BEGIN
    -- Restore inventory when an order item is deleted
    UPDATE products
    SET inventory = inventory + OLD.quantity
    WHERE id = OLD.product_id;
END;
```

## Testing

A test script is available at `src/main/resources/db/test_triggers.sql` that can be used to verify that the triggers are working correctly.

## Notes

- These triggers handle the inventory management automatically at the database level, eliminating the need for application code to manage inventory.
- The validation trigger ensures that orders cannot be placed for products with insufficient inventory.
- The update and delete triggers ensure inventory is properly managed throughout the order lifecycle. 