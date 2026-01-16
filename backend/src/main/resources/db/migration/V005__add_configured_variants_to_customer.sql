-- Add configuredVariants column to customer table
-- This stores a JSON array of variant IDs configured for each customer
-- Example: [1, 3, 5]

ALTER TABLE customer ADD COLUMN configured_variants TEXT DEFAULT NULL COMMENT 'JSON array of variant IDs configured for this customer';

-- Create index for potential future queries
CREATE INDEX idx_customer_variants ON customer(active);
