CREATE TABLE IF NOT EXISTS orders (
    order_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shopping_cart_id UUID,
    username VARCHAR(100) NOT NULL,
    order_state VARCHAR(20) NOT NULL CHECK (order_state IN (
        'NEW', 'ON_PAYMENT', 'ON_DELIVERY', 'DONE', 'DELIVERED',
        'ASSEMBLED', 'PAID', 'COMPLETED', 'DELIVERY_FAILED',
        'ASSEMBLY_FAILED', 'PAYMENT_FAILED', 'PRODUCT_RETURNED', 'CANCELED'
    )),
    payment_id UUID,
    delivery_id UUID,
    delivery_weight DOUBLE PRECISION,
    delivery_volume DOUBLE PRECISION,
    fragile BOOLEAN DEFAULT FALSE,
    total_price NUMERIC(10,2),
    delivery_price NUMERIC(10,2),
    product_price NUMERIC(10,2),
    country VARCHAR(100),
    city VARCHAR(100),
    street VARCHAR(200),
    house VARCHAR(20),
    flat VARCHAR(20),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS order_items (
    order_item_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(order_id) ON DELETE CASCADE,
    product_id UUID NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    version BIGINT DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_orders_username
ON orders(username);

CREATE INDEX IF NOT EXISTS idx_orders_state
ON orders(order_state);

CREATE INDEX IF NOT EXISTS idx_orders_created_at
ON orders(created_at);

CREATE INDEX IF NOT EXISTS idx_order_items_order_id
ON order_items(order_id);

CREATE UNIQUE INDEX IF NOT EXISTS idx_order_items_order_product
ON order_items(order_id, product_id);

