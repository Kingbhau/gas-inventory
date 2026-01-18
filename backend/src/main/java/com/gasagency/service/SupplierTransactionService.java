
package com.gasagency.service;

import com.gasagency.dto.CreateSupplierTransactionRequestDTO;
import com.gasagency.dto.SupplierTransactionDTO;
import com.gasagency.entity.*;
import com.gasagency.repository.*;
import com.gasagency.exception.ResourceNotFoundException;
import com.gasagency.util.LoggerUtil;
import com.gasagency.util.ReferenceNumberGenerator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SupplierTransactionService {
        private static final Logger logger = LoggerFactory.getLogger(SupplierTransactionService.class);
        private final SupplierTransactionRepository repository;
        private final SupplierRepository supplierRepository;
        private final CylinderVariantRepository variantRepository;
        private final WarehouseRepository warehouseRepository;
        private final InventoryStockService inventoryStockService;
        private final ReferenceNumberGenerator referenceNumberGenerator;

        public SupplierTransactionService(SupplierTransactionRepository repository,
                        SupplierRepository supplierRepository,
                        CylinderVariantRepository variantRepository,
                        WarehouseRepository warehouseRepository,
                        InventoryStockService inventoryStockService,
                        ReferenceNumberGenerator referenceNumberGenerator) {
                this.repository = repository;
                this.supplierRepository = supplierRepository;
                this.variantRepository = variantRepository;
                this.warehouseRepository = warehouseRepository;
                this.inventoryStockService = inventoryStockService;
                this.referenceNumberGenerator = referenceNumberGenerator;
        }

        @Transactional
        public SupplierTransactionDTO updateTransaction(Long id, CreateSupplierTransactionRequestDTO request) {
                LoggerUtil.logBusinessEntry(logger, "UPDATE_TRANSACTION", "id", id, "supplierId",
                                request.getSupplierId(), "variantId", request.getVariantId());

                SupplierTransaction transaction = repository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Transaction not found with id: " + id));

                // Validate request
                if (request == null || request.getWarehouseId() == null || request.getSupplierId() == null
                                || request.getVariantId() == null) {
                        LoggerUtil.logBusinessError(logger, "UPDATE_TRANSACTION", "Invalid request", "reason",
                                        "warehouse, supplier or variant ID is null");
                        throw new IllegalArgumentException("Warehouse ID, Supplier ID and Variant ID cannot be null");
                }

                // Warehouse cannot be changed after transaction creation
                if (!transaction.getWarehouse().getId().equals(request.getWarehouseId())) {
                        LoggerUtil.logBusinessError(logger, "UPDATE_TRANSACTION", "Warehouse change not allowed",
                                        "originalWarehouse", transaction.getWarehouse().getId(),
                                        "requestedWarehouse", request.getWarehouseId());
                        throw new IllegalArgumentException("Warehouse cannot be changed after transaction creation");
                }

                if (request.getFilledReceived() < 0 || request.getEmptySent() < 0) {
                        LoggerUtil.logBusinessError(logger, "UPDATE_TRANSACTION", "Negative quantities", "filled",
                                        request.getFilledReceived(), "empty", request.getEmptySent());
                        throw new IllegalArgumentException("Quantities cannot be negative");
                }
                if (request.getFilledReceived() == 0 && request.getEmptySent() == 0) {
                        LoggerUtil.logBusinessError(logger, "UPDATE_TRANSACTION", "Zero quantities", "reason",
                                        "must have at least one quantity");
                        throw new IllegalArgumentException(
                                        "Transaction must have at least one quantity (filled or empty)");
                }

                Warehouse warehouse = transaction.getWarehouse(); // Use existing warehouse
                Supplier supplier = supplierRepository.findById(request.getSupplierId())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Supplier not found with id: " + request.getSupplierId()));
                CylinderVariant variant = variantRepository.findById(request.getVariantId())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Variant not found with id: " + request.getVariantId()));

                // Inventory adjustment: only adjust quantities for the same warehouse
                // Calculate quantity differences
                long filledDifference = request.getFilledReceived() - transaction.getFilledReceived();
                long emptyDifference = request.getEmptySent() - transaction.getEmptySent();

                // Apply adjustments only if there are differences
                if (filledDifference > 0) {
                        inventoryStockService.incrementFilledQty(warehouse, transaction.getVariant(), filledDifference);
                } else if (filledDifference < 0) {
                        inventoryStockService.decrementFilledQty(warehouse, transaction.getVariant(),
                                        Math.abs(filledDifference));
                }

                if (emptyDifference > 0) {
                        inventoryStockService.incrementEmptyQty(warehouse, transaction.getVariant(), emptyDifference);
                } else if (emptyDifference < 0) {
                        inventoryStockService.decrementEmptyQty(warehouse, transaction.getVariant(),
                                        Math.abs(emptyDifference));
                }

                // Update transaction fields (warehouse remains unchanged)
                transaction.setSupplier(supplier);
                transaction.setVariant(variant);
                transaction.setTransactionDate(request.getTransactionDate() != null ? request.getTransactionDate()
                                : transaction.getTransactionDate());
                transaction.setFilledReceived(request.getFilledReceived());
                transaction.setEmptySent(request.getEmptySent());
                transaction.setReference(request.getReference());
                transaction.setAmount(request.getAmount());
                transaction = repository.save(transaction);

                LoggerUtil.logBusinessSuccess(logger, "UPDATE_TRANSACTION", "id", transaction.getId(), "supplierId",
                                supplier.getId(), "filled", request.getFilledReceived());
                LoggerUtil.logAudit("UPDATE", "SUPPLIER_TRANSACTION", "transactionId", transaction.getId(),
                                "supplierId", supplier.getId());

                return toDTO(transaction);
        }

        @Transactional
        public SupplierTransactionDTO recordTransaction(CreateSupplierTransactionRequestDTO request) {
                LoggerUtil.logBusinessEntry(logger, "RECORD_TRANSACTION", "warehouseId",
                                request != null ? request.getWarehouseId() : "null", "supplierId",
                                request != null ? request.getSupplierId() : "null", "variantId",
                                request != null ? request.getVariantId() : "null");

                // Validate request
                if (request == null || request.getWarehouseId() == null || request.getSupplierId() == null
                                || request.getVariantId() == null) {
                        LoggerUtil.logBusinessError(logger, "RECORD_TRANSACTION", "Invalid request", "reason",
                                        "warehouse, supplier or variant ID is null");
                        throw new IllegalArgumentException("Warehouse ID, Supplier ID and Variant ID cannot be null");
                }

                if (request.getFilledReceived() < 0 || request.getEmptySent() < 0) {
                        LoggerUtil.logBusinessError(logger, "RECORD_TRANSACTION", "Negative quantities", "filled",
                                        request.getFilledReceived(), "empty", request.getEmptySent());
                        throw new IllegalArgumentException("Quantities cannot be negative");
                }

                if (request.getFilledReceived() == 0 && request.getEmptySent() == 0) {
                        LoggerUtil.logBusinessError(logger, "RECORD_TRANSACTION", "Zero quantities", "reason",
                                        "must have at least one quantity");
                        throw new IllegalArgumentException(
                                        "Transaction must have at least one quantity (filled or empty)");
                }

                Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                                .orElseThrow(
                                                () -> {
                                                        LoggerUtil.logBusinessError(logger, "RECORD_TRANSACTION",
                                                                        "Warehouse not found", "warehouseId",
                                                                        request.getWarehouseId());
                                                        return new ResourceNotFoundException(
                                                                        "Warehouse not found with id: "
                                                                                        + request.getWarehouseId());
                                                });

                Supplier supplier = supplierRepository.findById(request.getSupplierId())
                                .orElseThrow(
                                                () -> {
                                                        LoggerUtil.logBusinessError(logger, "RECORD_TRANSACTION",
                                                                        "Supplier not found", "supplierId",
                                                                        request.getSupplierId());
                                                        return new ResourceNotFoundException(
                                                                        "Supplier not found with id: "
                                                                                        + request.getSupplierId());
                                                });

                CylinderVariant variant = variantRepository.findById(request.getVariantId())
                                .orElseThrow(
                                                () -> {
                                                        LoggerUtil.logBusinessError(logger, "RECORD_TRANSACTION",
                                                                        "Variant not found", "variantId",
                                                                        request.getVariantId());
                                                        return new ResourceNotFoundException(
                                                                        "Variant not found with id: "
                                                                                        + request.getVariantId());
                                                });

                // Create transaction with warehouse
                SupplierTransaction transaction = new SupplierTransaction(
                                warehouse, supplier, variant, LocalDate.now(),
                                request.getFilledReceived(), request.getEmptySent(), request.getReference(),
                                request.getAmount());

                // Generate reference number BEFORE initial save
                String referenceNumber = referenceNumberGenerator.generateSupplierPurchaseOrderReference(
                                supplier.getCode());
                transaction.setReference(referenceNumber);

                transaction = repository.save(transaction);
                logger.info("Supplier transaction created with id: {} - Reference: {}",
                                transaction.getId(), referenceNumber);

                // Update inventory for the specific warehouse using warehouse-aware methods
                inventoryStockService.incrementFilledQty(warehouse, variant, request.getFilledReceived());
                inventoryStockService.decrementEmptyQty(warehouse, variant, request.getEmptySent());

                LoggerUtil.logBusinessSuccess(logger, "RECORD_TRANSACTION", "id", transaction.getId(), "supplierId",
                                supplier.getId(), "filled", request.getFilledReceived());
                LoggerUtil.logAudit("CREATE", "SUPPLIER_TRANSACTION", "transactionId", transaction.getId(),
                                "supplierId", supplier.getId());

                return toDTO(transaction);
        }

        public SupplierTransactionDTO getTransactionById(Long id) {
                LoggerUtil.logDatabaseOperation(logger, "SELECT", "SUPPLIER_TRANSACTION", "id", id);

                SupplierTransaction transaction = repository.findById(id)
                                .orElseThrow(() -> {
                                        LoggerUtil.logBusinessError(logger, "GET_TRANSACTION", "Transaction not found",
                                                        "id", id);
                                        return new ResourceNotFoundException(
                                                        "Transaction not found with id: " + id);
                                });
                return toDTO(transaction);
        }

        public List<SupplierTransactionDTO> getAllTransactions() {
                LoggerUtil.logDatabaseOperation(logger, "SELECT_ALL", "SUPPLIER_TRANSACTION");

                return repository.findAll().stream()
                                .map(this::toDTO)
                                .collect(Collectors.toList());
        }

        public Page<SupplierTransactionDTO> getAllTransactions(Pageable pageable) {
                LoggerUtil.logDatabaseOperation(logger, "SELECT_PAGINATED", "SUPPLIER_TRANSACTION", "page",
                                pageable.getPageNumber(), "size", pageable.getPageSize());

                return repository.findAll(pageable)
                                .map(this::toDTO);
        }

        public Page<SupplierTransactionDTO> getAllTransactions(Pageable pageable, String referenceNumber) {
                LoggerUtil.logDatabaseOperation(logger, "SELECT_PAGINATED", "SUPPLIER_TRANSACTION", "page",
                                pageable.getPageNumber(), "size", pageable.getPageSize(), "referenceNumber",
                                referenceNumber);

                Page<SupplierTransaction> result = repository.findAll(pageable);

                if (referenceNumber != null && !referenceNumber.isEmpty()) {
                        String refFilter = referenceNumber.toLowerCase();
                        List<SupplierTransactionDTO> filteredList = result.getContent().stream()
                                        .filter(transaction -> transaction.getReference() != null &&
                                                        transaction.getReference().toLowerCase().contains(refFilter))
                                        .map(this::toDTO)
                                        .collect(Collectors.toList());
                        return new org.springframework.data.domain.PageImpl<>(filteredList, pageable,
                                        filteredList.size());
                }

                return result.map(this::toDTO);
        }

        public List<SupplierTransactionDTO> getTransactionsBySupplier(Long supplierId) {
                LoggerUtil.logDatabaseOperation(logger, "SELECT", "SUPPLIER_TRANSACTION", "supplierId", supplierId);

                Supplier supplier = supplierRepository.findById(supplierId)
                                .orElseThrow(() -> {
                                        LoggerUtil.logBusinessError(logger, "GET_TRANSACTIONS_BY_SUPPLIER",
                                                        "Supplier not found", "supplierId", supplierId);
                                        return new ResourceNotFoundException(
                                                        "Supplier not found with id: " + supplierId);
                                });
                return repository.findBySupplier(supplier).stream()
                                .map(this::toDTO)
                                .collect(Collectors.toList());
        }

        public List<SupplierTransactionDTO> getTransactionsByWarehouse(Long warehouseId) {
                LoggerUtil.logDatabaseOperation(logger, "SELECT", "SUPPLIER_TRANSACTION", "warehouseId", warehouseId);

                Warehouse warehouse = warehouseRepository.findById(warehouseId)
                                .orElseThrow(() -> {
                                        LoggerUtil.logBusinessError(logger, "GET_TRANSACTIONS_BY_WAREHOUSE",
                                                        "Warehouse not found", "warehouseId", warehouseId);
                                        return new ResourceNotFoundException(
                                                        "Warehouse not found with id: " + warehouseId);
                                });
                return repository.findByWarehouse(warehouse).stream()
                                .map(this::toDTO)
                                .collect(Collectors.toList());
        }

        private SupplierTransactionDTO toDTO(SupplierTransaction transaction) {
                return new SupplierTransactionDTO(
                                transaction.getId(),
                                transaction.getWarehouse().getId(),
                                transaction.getWarehouse().getName(),
                                transaction.getSupplier().getId(),
                                transaction.getSupplier().getName(),
                                transaction.getVariant().getId(),
                                transaction.getVariant().getName(),
                                transaction.getTransactionDate(),
                                transaction.getFilledReceived(),
                                transaction.getEmptySent(),
                                transaction.getReference(),
                                transaction.getAmount());
        }
}
