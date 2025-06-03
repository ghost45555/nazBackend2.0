-- Drop tables/views if they exist
DROP TABLE IF EXISTS product_manager.customer_orders_view;
DROP VIEW IF EXISTS product_manager.customer_orders_view;
DROP TABLE IF EXISTS product_manager.order_items_view;
DROP VIEW IF EXISTS product_manager.order_items_view;

-- Create views
CREATE VIEW product_manager.customer_orders_view AS
SELECT 
    o.id,
    o.user_id,
    u.email,
    u.first_name AS firstName,
    u.last_name AS lastName,
    o.address,
    o.apartment,
    o.city,
    o.postal_code AS postalCode,
    o.phone,
    o.order_notes AS orderNotes,
    o.payment_method AS paymentMethod,
    o.subtotal,
    o.shipping,
    o.total,
    o.status_id AS statusId,
    os.name AS statusName,
    o.created_at AS createdAt,
    o.updated_at AS updatedAt
FROM 
    product_manager.orders o
JOIN 
    product_manager.users u ON o.user_id = u.id
JOIN 
    product_manager.order_statuses os ON o.status_id = os.id;

CREATE VIEW product_manager.order_items_view AS
SELECT
    oi.id,
    oi.order_id,
    oi.product_id,
    oi.product_name AS productName,
    oi.product_image AS productImage,
    oi.packaging_photo AS packagingPhoto,
    oi.weight,
    oi.price,
    oi.quantity
FROM
    product_manager.order_items oi;
 