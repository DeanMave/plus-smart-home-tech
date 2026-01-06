CREATE TABLE IF NOT EXISTS payments (
    payment_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL,
    payment_state VARCHAR(20) NOT NULL CHECK (payment_state IN ('PENDING', 'SUCCESS', 'FAILED')),
    product_cost NUMERIC(10,2),
    delivery_cost NUMERIC(10,2),
    tax_cost NUMERIC(10,2),
    total_cost NUMERIC(10,2),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_payments_order_id
ON payments(order_id);

CREATE INDEX IF NOT EXISTS idx_payments_state
ON payments(payment_state);

CREATE INDEX IF NOT EXISTS idx_payments_created_at
ON payments(created_at);
