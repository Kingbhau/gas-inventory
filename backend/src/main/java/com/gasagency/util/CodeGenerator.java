package com.gasagency.util;

import com.gasagency.repository.WarehouseRepository;
import com.gasagency.repository.SupplierRepository;
import com.gasagency.repository.BankAccountRepository;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility for auto-generating unique codes for Warehouses, Suppliers, and Bank
 * Accounts.
 * 
 * Code Format Standards:
 * - Warehouse: WH{PADDED_SEQUENCE} (e.g., WH001, WH002, WH100)
 * - Supplier: SUP{PADDED_SEQUENCE} (e.g., SUP001, SUP002, SUP050)
 * - Bank Account: BANK{PADDED_SEQUENCE} (e.g., BANK001, BANK002, BANK050)
 * 
 * Codes are immutable once assigned and must be unique in the database.
 */
@Component
public class CodeGenerator {

    private static final Logger logger = LoggerFactory.getLogger(CodeGenerator.class);
    private static final String WAREHOUSE_PREFIX = "WH";
    private static final String SUPPLIER_PREFIX = "SUP";
    private static final String BANK_PREFIX = "BANK";
    private static final int CODE_PADDING = 3; // Total 5-6 chars: WH001, SUP001

    private final WarehouseRepository warehouseRepository;
    private final SupplierRepository supplierRepository;
    private final BankAccountRepository bankAccountRepository;

    public CodeGenerator(WarehouseRepository warehouseRepository,
            SupplierRepository supplierRepository,
            BankAccountRepository bankAccountRepository) {
        this.warehouseRepository = warehouseRepository;
        this.supplierRepository = supplierRepository;
        this.bankAccountRepository = bankAccountRepository;
    }

    /**
     * Generate a unique warehouse code.
     * Format: WH001, WH002, ..., WH999, etc.
     * 
     * @return Generated warehouse code
     */
    public String generateWarehouseCode() {
        // Get count of existing warehouses
        long count = warehouseRepository.count();
        long nextSequence = count + 1;

        String code = String.format("%s%0" + CODE_PADDING + "d", WAREHOUSE_PREFIX, nextSequence);
        logger.info("Generated warehouse code: {}", code);

        return code;
    }

    /**
     * Generate a unique supplier code.
     * Format: SUP001, SUP002, ..., SUP999, etc.
     * 
     * @return Generated supplier code
     */
    public String generateSupplierCode() {
        // Get count of existing suppliers
        long count = supplierRepository.count();
        long nextSequence = count + 1;

        String code = String.format("%s%0" + CODE_PADDING + "d", SUPPLIER_PREFIX, nextSequence);
        logger.info("Generated supplier code: {}", code);

        return code;
    }

    /**
     * Generate a unique bank account code.
     * Format: BANK001, BANK002, ..., BANK999, etc.
     * 
     * @return Generated bank account code
     */
    public String generateBankCode() {
        // Get count of existing bank accounts
        long count = bankAccountRepository.count();
        long nextSequence = count + 1;

        String code = String.format("%s%0" + CODE_PADDING + "d", BANK_PREFIX, nextSequence);
        logger.info("Generated bank account code: {}", code);

        return code;
    }
}

