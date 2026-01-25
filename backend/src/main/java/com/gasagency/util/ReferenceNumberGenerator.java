package com.gasagency.util;

import com.gasagency.entity.Warehouse;
import com.gasagency.repository.SaleRepository;
import com.gasagency.repository.WarehouseTransferRepository;
import com.gasagency.repository.SupplierTransactionRepository;
import com.gasagency.repository.CustomerCylinderLedgerRepository;
import com.gasagency.repository.BankAccountLedgerRepository;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Utility service for generating production-standard reference numbers
 * for all order types in the Gas Inventory Management System.
 * 
 * Reference Format Standards:
 * - Sales: SO-{WH_CODE}-{YYYYMM}-{SEQUENCE} (e.g., SO-WH1-202601-001234)
 * - Transfer: WT-{FROM}-{TO}-{YYYYMM}-{SEQUENCE} (e.g.,
 * WT-WH1-WH2-202601-000456)
 * - Purchase: PO-{SUPPLIER}-{YYYYMM}-{SEQUENCE} (e.g., PO-SUP001-202601-000123)
 * - EmptyRtn: ER-{WH_CODE}-{YYYYMM}-{SEQUENCE} (e.g., ER-WH001-202601-000123)
 */
@Component
public class ReferenceNumberGenerator {

    private static final Logger logger = LoggerFactory.getLogger(ReferenceNumberGenerator.class);
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

    private final SaleRepository saleRepository;
    private final WarehouseTransferRepository warehouseTransferRepository;
    private final SupplierTransactionRepository supplierTransactionRepository;
    private final CustomerCylinderLedgerRepository customerCylinderLedgerRepository;
    private final BankAccountLedgerRepository bankAccountLedgerRepository;

    public ReferenceNumberGenerator(
            SaleRepository saleRepository,
            WarehouseTransferRepository warehouseTransferRepository,
            SupplierTransactionRepository supplierTransactionRepository,
            CustomerCylinderLedgerRepository customerCylinderLedgerRepository,
            BankAccountLedgerRepository bankAccountLedgerRepository) {
        this.saleRepository = saleRepository;
        this.warehouseTransferRepository = warehouseTransferRepository;
        this.supplierTransactionRepository = supplierTransactionRepository;
        this.customerCylinderLedgerRepository = customerCylinderLedgerRepository;
        this.bankAccountLedgerRepository = bankAccountLedgerRepository;
    }

    /**
     * Generates Sale Order Reference: SO-{WH_CODE}-{YYYYMM}-{SEQUENCE}
     * Example: SO-WH1-202601-001234
     * 
     * @param warehouse The warehouse where the sale is created
     * @return Generated reference number with unique sequence
     */
    public String generateSaleReference(Warehouse warehouse) {
        Objects.requireNonNull(warehouse, "Warehouse cannot be null");

        String yearMonth = LocalDate.now().format(MONTH_FORMATTER);
        String warehouseCode = getWarehouseCode(warehouse);

        // Get count for this warehouse and month to generate sequence
        long sequence = saleRepository.countByWarehouseAndCreatedAtMonthYear(
                warehouse,
                LocalDate.parse("01-" + yearMonth, DateTimeFormatter.ofPattern("dd-yyyyMM")));

        String formattedSequence = String.format("%06d", sequence + 1);
        String reference = String.format("SO-%s-%s-%s", warehouseCode, yearMonth, formattedSequence);

        logger.info("Generated Sale reference: {} for warehouse: {}", reference, warehouse.getName());
        return reference;
    }

    /**
     * Generates Warehouse Transfer Reference:
     * WT-{FROM_WH}-{TO_WH}-{YYYYMM}-{SEQUENCE}
     * Example: WT-WH1-WH2-202601-000456
     * 
     * @param fromWarehouse Source warehouse
     * @param toWarehouse   Destination warehouse
     * @return Generated reference number with unique sequence
     */
    public String generateWarehouseTransferReference(Warehouse fromWarehouse, Warehouse toWarehouse) {
        Objects.requireNonNull(fromWarehouse, "From warehouse cannot be null");
        Objects.requireNonNull(toWarehouse, "To warehouse cannot be null");

        if (fromWarehouse.getId().equals(toWarehouse.getId())) {
            throw new IllegalArgumentException("Source and destination warehouses cannot be the same");
        }

        String yearMonth = LocalDate.now().format(MONTH_FORMATTER);
        String fromCode = getWarehouseCode(fromWarehouse);
        String toCode = getWarehouseCode(toWarehouse);

        // Get count for this month to generate sequence
        long sequence = warehouseTransferRepository.countByCreatedAtMonthYear(
                LocalDate.parse("01-" + yearMonth, DateTimeFormatter.ofPattern("dd-yyyyMM")));

        String formattedSequence = String.format("%06d", sequence + 1);
        String reference = String.format("WT-%s-%s-%s-%s", fromCode, toCode, yearMonth, formattedSequence);

        logger.info("Generated Warehouse Transfer reference: {} from {} to {}",
                reference, fromWarehouse.getName(), toWarehouse.getName());
        return reference;
    }

    /**
     * Generates Supplier Purchase Order Reference:
     * PO-{SUPPLIER_CODE}-{YYYYMM}-{SEQUENCE}
     * Example: PO-SUP001-202601-000123
     * 
     * @param supplierCode The code/ID of the supplier
     * @return Generated reference number with unique sequence
     */
    public String generateSupplierPurchaseOrderReference(String supplierCode) {
        Objects.requireNonNull(supplierCode, "Supplier code cannot be null");
        if (supplierCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Supplier code cannot be empty");
        }

        String yearMonth = LocalDate.now().format(MONTH_FORMATTER);

        // Get count for this month to generate sequence
        long sequence = supplierTransactionRepository.countByCreatedAtMonthYear(
                LocalDate.parse("01-" + yearMonth, DateTimeFormatter.ofPattern("dd-yyyyMM")));

        String formattedSequence = String.format("%06d", sequence + 1);
        String reference = String.format("PO-%s-%s-%s", supplierCode, yearMonth, formattedSequence);

        logger.info("Generated Purchase Order reference: {} for supplier: {}", reference, supplierCode);
        return reference;
    }

    /**
     * Generates Empty Return Reference: ER-{WH_CODE}-{YYYYMM}-{SEQUENCE}
     * Example: ER-WH001-202601-000001
     * 
     * @param warehouse The warehouse where empty cylinders are returned
     * @return Generated reference number with unique sequence
     */
    public String generateEmptyReturnReference(Warehouse warehouse) {
        Objects.requireNonNull(warehouse, "Warehouse cannot be null");

        String yearMonth = LocalDate.now().format(MONTH_FORMATTER);
        String warehouseCode = getWarehouseCode(warehouse);

        // Query to count EMPTY_RETURN entries for this warehouse in this month
        // Using raw JPQL count for empty returns with warehouse
        long sequence = customerCylinderLedgerRepository.countEmptyReturnsByWarehouseAndMonth(
                warehouse,
                LocalDate.parse("01-" + yearMonth, DateTimeFormatter.ofPattern("dd-yyyyMM")));

        String formattedSequence = String.format("%06d", sequence + 1);
        String reference = String.format("ER-%s-%s-%s", warehouseCode, yearMonth, formattedSequence);

        logger.info("Generated Empty Return reference: {} for warehouse: {}", reference, warehouse.getName());
        return reference;
    }

    /**
     * Generates Bank Account Transaction Reference:
     * {TYPE}-{BANK_CODE}-{YYYYMM}-{SEQUENCE}
     * Types: DEP (Deposit), WIT (Withdrawal), ADJ (Adjustment)
     * Example: DEP-HDFC-202601-000001
     * 
     * @param bankCode        The bank/account code (e.g., HDFC, ICICI, AXIS)
     * @param transactionType One of: DEP (Deposit), WIT (Withdrawal), ADJ
     *                        (Adjustment)
     * @return Generated reference number with unique sequence
     */
    public String generateBankTransactionReference(String bankCode, String transactionType) {
        Objects.requireNonNull(bankCode, "Bank code cannot be null");
        Objects.requireNonNull(transactionType, "Transaction type cannot be null");

        if (bankCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Bank code cannot be empty");
        }

        // Validate transaction type
        if (!transactionType.matches("^(DEP|WIT|ADJ)$")) {
            throw new IllegalArgumentException("Transaction type must be DEP, WIT, or ADJ");
        }

        String yearMonth = LocalDate.now().format(MONTH_FORMATTER);
        // Remove spaces and convert to uppercase for reference number
        String upperBankCode = bankCode.toUpperCase().replaceAll("\\s+", "");

        // Get count for this month to generate sequence
        long sequence = bankAccountLedgerRepository.countByCreatedAtMonthYear(
                LocalDate.parse("01-" + yearMonth, DateTimeFormatter.ofPattern("dd-yyyyMM")));

        String formattedSequence = String.format("%06d", sequence + 1);
        String reference = String.format("%s-%s-%s-%s", transactionType, upperBankCode, yearMonth, formattedSequence);

        logger.info("Generated Bank transaction reference: {} for bank: {} type: {}",
                reference, upperBankCode, transactionType);
        return reference;
    }

    /**
     * Helper method to extract warehouse code
     * 
     * @param warehouse The warehouse entity
     * @return Warehouse code (stored in database, e.g., "WH001")
     */
    private String getWarehouseCode(Warehouse warehouse) {
        if (warehouse.getCode() != null && !warehouse.getCode().trim().isEmpty()) {
            return warehouse.getCode().toUpperCase();
        }
        // Fallback should not happen if code is always set
        return "WH" + warehouse.getId();
    }
}
