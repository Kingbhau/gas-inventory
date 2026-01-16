-- Database Migration: Add Customer-Specific Pricing
-- Date: 2026-01-15
-- Description: Add salePrice and discountPrice columns to customer table

-- Add columns to customer table
ALTER TABLE customer ADD COLUMN sale_price NUMERIC(19,2) DEFAULT NULL;
ALTER TABLE customer ADD COLUMN discount_price NUMERIC(19,2) DEFAULT NULL;

-- Add comments/descriptions (optional, database-specific)
-- For MySQL:
-- ALTER TABLE customer MODIFY COLUMN sale_price DECIMAL(19,2) COMMENT 'Sale price per unit for this customer';
-- ALTER TABLE customer MODIFY COLUMN discount_price DECIMAL(19,2) COMMENT 'Fixed discount amount for this customer';

-- For PostgreSQL:
-- COMMENT ON COLUMN customer.sale_price IS 'Sale price per unit for this customer';
-- COMMENT ON COLUMN customer.discount_price IS 'Fixed discount amount for this customer';

-- Verify the changes
SELECT column_name, data_type, is_nullable 
FROM information_schema.columns 
WHERE table_name = 'customer' 
ORDER BY ordinal_position;
