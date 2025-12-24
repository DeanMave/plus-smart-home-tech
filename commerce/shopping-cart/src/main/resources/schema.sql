CREATE TABLE IF NOT EXISTS shopping_carts (
    shopping_cart_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(100) NOT NULL,
    cart_state VARCHAR(20) NOT NULL CHECK (cart_state IN ('ACTIVE', 'DEACTIVATED')),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS shopping_cart_items (
    cart_item_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shopping_cart_id UUID NOT NULL REFERENCES shopping_carts(shopping_cart_id) ON DELETE CASCADE,
    product_id UUID NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    version BIGINT DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_shopping_carts_username_active
ON shopping_carts(username) WHERE cart_state = 'ACTIVE';

CREATE INDEX IF NOT EXISTS idx_shopping_cart_items_cart_id
ON shopping_cart_items(shopping_cart_id);

CREATE UNIQUE INDEX IF NOT EXISTS idx_shopping_cart_items_cart_product
ON shopping_cart_items(shopping_cart_id, product_id);

