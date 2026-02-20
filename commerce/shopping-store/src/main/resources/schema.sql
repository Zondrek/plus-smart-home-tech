CREATE TABLE IF NOT EXISTS products (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    product_name VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    image_src VARCHAR(500),
    quantity_state VARCHAR(50),
    product_state VARCHAR(50),
    product_category VARCHAR(50),
    price DOUBLE PRECISION NOT NULL
);
