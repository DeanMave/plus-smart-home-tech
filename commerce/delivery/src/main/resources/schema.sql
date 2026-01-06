CREATE TABLE IF NOT EXISTS deliveries (
    delivery_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL,
    delivery_state VARCHAR(20) NOT NULL CHECK (delivery_state IN ('CREATED', 'IN_PROGRESS', 'DELIVERED', 'FAILED', 'CANCELLED')),

    from_country VARCHAR(100),
    from_city VARCHAR(100),
    from_street VARCHAR(200),
    from_house VARCHAR(20),
    from_flat VARCHAR(20),

    to_country VARCHAR(100) NOT NULL,
    to_city VARCHAR(100) NOT NULL,
    to_street VARCHAR(200) NOT NULL,
    to_house VARCHAR(20) NOT NULL,
    to_flat VARCHAR(20),

    delivery_weight DOUBLE PRECISION,
    delivery_volume DOUBLE PRECISION,
    fragile BOOLEAN DEFAULT FALSE,
    delivery_cost NUMERIC(10,2),

    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_deliveries_order_id
ON deliveries(order_id);

CREATE INDEX IF NOT EXISTS idx_deliveries_state
ON deliveries(delivery_state);

CREATE INDEX IF NOT EXISTS idx_deliveries_created_at
ON deliveries(created_at);

CREATE INDEX IF NOT EXISTS idx_deliveries_to_city
ON deliveries(to_city);

CREATE INDEX IF NOT EXISTS idx_deliveries_to_street
ON deliveries(to_street);
