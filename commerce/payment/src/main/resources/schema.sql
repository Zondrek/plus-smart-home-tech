CREATE SCHEMA IF NOT EXISTS payment;

CREATE TABLE IF NOT EXISTS payments (
    payment_id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    total_payment NUMERIC DEFAULT 0,
    delivery_total NUMERIC DEFAULT 0,
    fee_total NUMERIC DEFAULT 0,
    product_total NUMERIC DEFAULT 0,
    status VARCHAR(50) NOT NULL
);
