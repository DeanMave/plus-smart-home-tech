CREATE TABLE IF NOT EXISTS warehouse_products (
    warehouse_product_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL UNIQUE,
    quantity BIGINT NOT NULL CHECK (quantity >= 0),
    fragile BOOLEAN,
    width DOUBLE PRECISION,
    height DOUBLE PRECISION,
    depth DOUBLE PRECISION,
    weight DOUBLE PRECISION,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_warehouse_products_product_id
ON warehouse_products(product_id);

CREATE INDEX IF NOT EXISTS idx_warehouse_products_quantity
ON warehouse_products(quantity);
