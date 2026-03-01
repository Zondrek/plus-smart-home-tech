CREATE SCHEMA IF NOT EXISTS orders;

CREATE TABLE IF NOT EXISTS orders (
    order_id UUID PRIMARY KEY,
    username VARCHAR(255),
    shopping_cart_id UUID,
    payment_id UUID,
    delivery_id UUID,
    state VARCHAR(50) NOT NULL,
    delivery_weight DOUBLE PRECISION DEFAULT 0,
    delivery_volume DOUBLE PRECISION DEFAULT 0,
    fragile BOOLEAN DEFAULT FALSE,
    total_price NUMERIC DEFAULT 0,
    delivery_price NUMERIC DEFAULT 0,
    product_price NUMERIC DEFAULT 0
);

CREATE TABLE IF NOT EXISTS order_products (
    order_id UUID NOT NULL REFERENCES orders(order_id),
    product_id UUID NOT NULL,
    quantity BIGINT NOT NULL,
    PRIMARY KEY (order_id, product_id)
);
