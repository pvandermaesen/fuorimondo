CREATE TABLE orders (
    id                  UUID PRIMARY KEY,
    user_id             UUID NOT NULL REFERENCES users(id),
    product_id          UUID NOT NULL REFERENCES products(id),
    product_snapshot    TEXT NOT NULL,
    unit_price_eur      NUMERIC(10,2) NOT NULL CHECK (unit_price_eur >= 0),
    total_eur           NUMERIC(10,2) NOT NULL CHECK (total_eur >= 0),
    shipping_address_id UUID REFERENCES addresses(id),
    shipping_snapshot   TEXT,
    status              VARCHAR(32) NOT NULL CHECK (status IN ('PENDING_PAYMENT','PAID','FAILED','CANCELLED','EXPIRED')),
    mollie_payment_id   VARCHAR(64),
    mollie_checkout_url VARCHAR(512),
    expires_at          TIMESTAMP,
    paid_at             TIMESTAMP,
    created_at          TIMESTAMP NOT NULL,
    updated_at          TIMESTAMP NOT NULL
);

CREATE INDEX idx_orders_user ON orders(user_id);
CREATE INDEX idx_orders_product_status ON orders(product_id, status);
CREATE INDEX idx_orders_mollie_payment ON orders(mollie_payment_id);
