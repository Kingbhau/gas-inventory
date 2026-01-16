-- Migration: Create customer_variant_price table for variant-specific pricing
-- This table stores distinct pricing for each customer-variant combination

CREATE TABLE IF NOT EXISTS customer_variant_price (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    customer_id BIGINT NOT NULL,
    variant_id BIGINT NOT NULL,
    sale_price NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    discount_price NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    
    -- Unique constraint to ensure one price per customer-variant combination
    UNIQUE KEY uk_customer_variant (customer_id, variant_id),
    
    -- Foreign keys for referential integrity
    CONSTRAINT fk_cvp_customer FOREIGN KEY (customer_id) 
        REFERENCES customer(id) ON DELETE CASCADE,
    CONSTRAINT fk_cvp_variant FOREIGN KEY (variant_id) 
        REFERENCES cylinder_variant(id) ON DELETE CASCADE
);

-- Create indexes for query optimization
CREATE INDEX idx_cvp_customer_id ON customer_variant_price(customer_id);
CREATE INDEX idx_cvp_variant_id ON customer_variant_price(variant_id);
CREATE INDEX idx_cvp_created_at ON customer_variant_price(created_at);
