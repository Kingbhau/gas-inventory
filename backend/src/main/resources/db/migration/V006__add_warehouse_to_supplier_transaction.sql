-- Migration: Add warehouse support to supplier_transaction table
-- This adds warehouse_id as a required field to track which warehouse received the supplier transaction

ALTER TABLE supplier_transaction 
ADD COLUMN warehouse_id BIGINT AFTER id;

-- Set default warehouse_id to 1 for existing records (assumes warehouse with id=1 exists)
UPDATE supplier_transaction SET warehouse_id = 1 WHERE warehouse_id IS NULL;

-- Add NOT NULL constraint after setting values
ALTER TABLE supplier_transaction 
MODIFY COLUMN warehouse_id BIGINT NOT NULL;

-- Add foreign key constraint
ALTER TABLE supplier_transaction
ADD CONSTRAINT fk_supplier_transaction_warehouse 
FOREIGN KEY (warehouse_id) REFERENCES warehouse(id) ON DELETE RESTRICT;

-- Add index for warehouse_id
CREATE INDEX idx_suppliertransaction_warehouse_id ON supplier_transaction(warehouse_id);
