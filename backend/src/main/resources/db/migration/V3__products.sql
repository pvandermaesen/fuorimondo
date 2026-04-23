CREATE TABLE products (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(4000),
    price_eur NUMERIC(10,2) NOT NULL CHECK (price_eur >= 0),
    photo_filename VARCHAR(255),
    delivery BOOLEAN NOT NULL,
    weight_kg NUMERIC(6,3),
    sale_start_at TIMESTAMP NOT NULL,
    sale_end_at TIMESTAMP,
    stock INTEGER CHECK (stock IS NULL OR stock >= 0),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_products_sale_start_at ON products(sale_start_at);

CREATE TABLE product_tiers (
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    tier_code VARCHAR(16) NOT NULL CHECK (tier_code IN ('TIER_1', 'TIER_2', 'TIER_3')),
    PRIMARY KEY (product_id, tier_code)
);
