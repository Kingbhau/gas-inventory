
package com.gasagency.service;

import com.gasagency.dto.request.CreateSupplierTransactionRequestDTO;
import com.gasagency.dto.response.SupplierBorrowBalanceDTO;
import com.gasagency.dto.response.SupplierTransactionDTO;
import com.gasagency.entity.*;
import com.gasagency.repository.*;
import com.gasagency.exception.ResourceNotFoundException;
import com.gasagency.exception.InvalidOperationException;
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

                if (request.getFilledReceived() < 0 || request.getEmptyReceived() < 0
                                || request.getFilledSent() < 0 || request.getEmptySent() < 0) {
                        LoggerUtil.logBusinessError(logger, "UPDATE_TRANSACTION", "Negative quantities", "filled",
                                        request.getFilledReceived(), "empty", request.getEmptySent());
                        throw new IllegalArgumentException("Quantities cannot be negative");
                }
                SupplierTransaction.TransactionType existingType = transaction.getTransactionType() != null
                                ? transaction.getTransactionType()
                                : SupplierTransaction.TransactionType.PURCHASE;
                SupplierTransaction.TransactionType requestedType = parseTransactionType(request.getTransactionType());
                if (existingType != requestedType) {
                        throw new IllegalArgumentException("Transaction type cannot be changed after creation");
                }
                validateQuantities(existingType, request.getFilledReceived(), request.getEmptyReceived(),
                                request.getFilledSent(), request.getEmptySent());

                if (existingType == SupplierTransaction.TransactionType.PURCHASE && request.getAmount() == null) {
                        throw new IllegalArgumentException("Amount is required for purchase transactions");
                }

                Warehouse warehouse = transaction.getWarehouse(); // Use existing warehouse
                Supplier supplier = supplierRepository.findById(request.getSupplierId())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Supplier not found with id: " + request.getSupplierId()));
                CylinderVariant variant = variantRepository.findById(request.getVariantId())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Variant not found with id: " + request.getVariantId()));

                // Inventory adjustment: lock stock row to prevent race conditions
                InventoryStock stock = inventoryStockService.getStockWithLock(warehouse, transaction.getVariant());
                if (stock == null) {
                        stock = inventoryStockService.getOrCreateStock(warehouse, transaction.getVariant());
                }

                Long filledReceived = request.getFilledReceived() != null ? request.getFilledReceived() : 0L;
                Long emptyReceived = request.getEmptyReceived() != null ? request.getEmptyReceived() : 0L;
                Long filledSent = request.getFilledSent() != null ? request.getFilledSent() : 0L;
                Long emptySent = request.getEmptySent() != null ? request.getEmptySent() : 0L;

                long prevFilledReceived = transaction.getFilledReceived() != null ? transaction.getFilledReceived() : 0L;
                long prevEmptyReceived = transaction.getEmptyReceived() != null ? transaction.getEmptyReceived() : 0L;
                long prevFilledSent = transaction.getFilledSent() != null ? transaction.getFilledSent() : 0L;
                long prevEmptySent = transaction.getEmptySent() != null ? transaction.getEmptySent() : 0L;

                if (existingType == SupplierTransaction.TransactionType.BORROW_OUT) {
                        validateBorrowOutAvailability(
                                        request.getSupplierId(),
                                        request.getWarehouseId(),
                                        request.getVariantId(),
                                        filledSent,
                                        emptySent,
                                        transaction.getId(),
                                        prevFilledSent,
                                        prevEmptySent);
                }
                if (existingType == SupplierTransaction.TransactionType.PURCHASE_RETURN) {
                        validatePurchaseReturnAvailability(
                                        request.getSupplierId(),
                                        request.getWarehouseId(),
                                        request.getVariantId(),
                                        filledSent,
                                        transaction.getId(),
                                        prevFilledSent);
                }

                // Calculate quantity differences
                long filledDifference = (filledReceived - prevFilledReceived) - (filledSent - prevFilledSent);
                long emptyDifference = (emptyReceived - prevEmptyReceived) - (emptySent - prevEmptySent);

                long currentFilled = stock.getFilledQty() != null ? stock.getFilledQty() : 0L;
                long currentEmpty = stock.getEmptyQty() != null ? stock.getEmptyQty() : 0L;

                long newFilled = currentFilled + filledDifference;
                long newEmpty = currentEmpty + emptyDifference;

                if (newFilled < 0) {
                        throw new InvalidOperationException(
                                        "Insufficient filled stock in " + warehouse.getName() +
                                                        ". Available: " + currentFilled + ", Required: " +
                                                        Math.abs(filledDifference));
                }
                if (newEmpty < 0) {
                        throw new InvalidOperationException(
                                        "Insufficient empty stock in " + warehouse.getName() +
                                                        ". Available: " + currentEmpty + ", Required: " +
                                                        Math.abs(emptyDifference));
                }

                stock.setFilledQty(newFilled);
                stock.setEmptyQty(newEmpty);
                inventoryStockService.updateStock(stock);

                // Update transaction fields (warehouse remains unchanged)
                transaction.setSupplier(supplier);
                transaction.setVariant(variant);
                transaction.setTransactionDate(request.getTransactionDate() != null ? request.getTransactionDate()
                                : transaction.getTransactionDate());
                transaction.setFilledReceived(filledReceived);
                transaction.setEmptyReceived(emptyReceived);
                transaction.setFilledSent(filledSent);
                transaction.setEmptySent(emptySent);
                if (request.getReference() != null && !request.getReference().trim().isEmpty()) {
                        transaction.setReference(request.getReference().trim());
                }
                transaction.setAmount(
                                request.getAmount() != null ? new java.math.BigDecimal(request.getAmount()) : null);
                transaction.setNote(request.getNote());
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

                if (request.getFilledReceived() < 0 || request.getEmptyReceived() < 0
                                || request.getFilledSent() < 0 || request.getEmptySent() < 0) {
                        LoggerUtil.logBusinessError(logger, "RECORD_TRANSACTION", "Negative quantities", "filled",
                                        request.getFilledReceived(), "empty", request.getEmptySent());
                        throw new IllegalArgumentException("Quantities cannot be negative");
                }

                SupplierTransaction.TransactionType type = parseTransactionType(request.getTransactionType());
                validateQuantities(type, request.getFilledReceived(), request.getEmptyReceived(),
                                request.getFilledSent(), request.getEmptySent());

                if (type == SupplierTransaction.TransactionType.PURCHASE && request.getAmount() == null) {
                        throw new IllegalArgumentException("Amount is required for purchase transactions");
                }

                if (type == SupplierTransaction.TransactionType.BORROW_OUT) {
                        validateBorrowOutAvailability(
                                        request.getSupplierId(),
                                        request.getWarehouseId(),
                                        request.getVariantId(),
                                        request.getFilledSent(),
                                        request.getEmptySent(),
                                        null,
                                        0L,
                                        0L);
                }
                if (type == SupplierTransaction.TransactionType.PURCHASE_RETURN) {
                        validatePurchaseReturnAvailability(
                                        request.getSupplierId(),
                                        request.getWarehouseId(),
                                        request.getVariantId(),
                                        request.getFilledSent(),
                                        null,
                                        0L);
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

                Long filledReceived = request.getFilledReceived() != null ? request.getFilledReceived() : 0L;
                Long emptyReceived = request.getEmptyReceived() != null ? request.getEmptyReceived() : 0L;
                Long filledSent = request.getFilledSent() != null ? request.getFilledSent() : 0L;
                Long emptySent = request.getEmptySent() != null ? request.getEmptySent() : 0L;

                String referenceNumber = null;
                if (type == SupplierTransaction.TransactionType.PURCHASE) {
                        referenceNumber = referenceNumberGenerator.generateSupplierPurchaseOrderReference(
                                        supplier.getCode());
                } else if (type == SupplierTransaction.TransactionType.PURCHASE_RETURN) {
                        referenceNumber = referenceNumberGenerator.generateSupplierPurchaseReturnReference(
                                        supplier.getCode());
                } else {
                        referenceNumber = referenceNumberGenerator.generateSupplierBorrowReference(
                                        supplier.getCode(), type.name());
                }

                // Create transaction with warehouse
                SupplierTransaction transaction = new SupplierTransaction(
                                warehouse, supplier, variant,
                                request.getTransactionDate() != null ? request.getTransactionDate() : LocalDate.now(),
                                filledReceived, emptyReceived, filledSent, emptySent,
                                referenceNumber,
                                request.getAmount() != null ? new java.math.BigDecimal(request.getAmount()) : null,
                                type,
                                request.getNote());

                transaction = repository.save(transaction);
                logger.info("Supplier transaction created with id: {} - Reference: {}",
                                transaction.getId(), referenceNumber);

                // Update inventory with lock to avoid concurrent stock mismatches
                InventoryStock stock = inventoryStockService.getStockWithLock(warehouse, variant);
                if (stock == null) {
                        stock = inventoryStockService.getOrCreateStock(warehouse, variant);
                }

                long currentFilled = stock.getFilledQty() != null ? stock.getFilledQty() : 0L;
                long currentEmpty = stock.getEmptyQty() != null ? stock.getEmptyQty() : 0L;

                long newFilled = currentFilled + filledReceived - filledSent;
                long newEmpty = currentEmpty + emptyReceived - emptySent;

                if (newFilled < 0) {
                        throw new InvalidOperationException(
                                        "Insufficient filled stock in " + warehouse.getName() +
                                                        ". Available: " + currentFilled + ", Required: " +
                                                        filledSent);
                }
                if (newEmpty < 0) {
                        throw new InvalidOperationException(
                                        "Insufficient empty stock in " + warehouse.getName() +
                                                        ". Available: " + currentEmpty + ", Required: " +
                                                        emptySent);
                }

                stock.setFilledQty(newFilled);
                stock.setEmptyQty(newEmpty);
                inventoryStockService.updateStock(stock);

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

        public Page<SupplierTransactionDTO> getAllTransactions(Pageable pageable, String referenceNumber, String createdBy,
                        Long supplierId, Long warehouseId, Long variantId, LocalDate fromDate, LocalDate toDate,
                        String transactionType) {
                LoggerUtil.logDatabaseOperation(logger, "SELECT_PAGINATED", "SUPPLIER_TRANSACTION", "page",
                                pageable.getPageNumber(), "size", pageable.getPageSize(), "referenceNumber",
                                referenceNumber);

                SupplierTransaction.TransactionType parsedType = (transactionType == null || transactionType.trim().isEmpty())
                                ? null
                                : parseTransactionType(transactionType);

                return repository.findByFilters(referenceNumber, createdBy, supplierId, warehouseId, variantId,
                                fromDate, toDate, parsedType, pageable)
                                .map(this::toDTO);
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

        public SupplierBorrowBalanceDTO getBorrowBalance(Long supplierId, Long warehouseId, Long variantId,
                        Long excludeId) {
                long borrowedFilled = repository.sumBorrowInFilled(supplierId, warehouseId, variantId, excludeId)
                                - repository.sumBorrowOutFilled(supplierId, warehouseId, variantId, excludeId);
                long borrowedEmpty = repository.sumBorrowInEmpty(supplierId, warehouseId, variantId, excludeId)
                                - repository.sumBorrowOutEmpty(supplierId, warehouseId, variantId, excludeId);
                return new SupplierBorrowBalanceDTO(
                                supplierId,
                                warehouseId,
                                variantId,
                                Math.max(borrowedFilled, 0L),
                                Math.max(borrowedEmpty, 0L));
        }

        private SupplierTransactionDTO toDTO(SupplierTransaction transaction) {
                SupplierTransactionDTO dto = new SupplierTransactionDTO(
                                transaction.getId(),
                                transaction.getWarehouse().getId(),
                                transaction.getWarehouse().getName(),
                                transaction.getSupplier().getId(),
                                transaction.getSupplier().getName(),
                                transaction.getVariant().getId(),
                                transaction.getVariant().getName(),
                                transaction.getTransactionDate(),
                                transaction.getFilledReceived(),
                                transaction.getEmptyReceived(),
                                transaction.getFilledSent(),
                                transaction.getEmptySent(),
                                transaction.getTransactionType() != null ? transaction.getTransactionType().name() : SupplierTransaction.TransactionType.PURCHASE.name(),
                                transaction.getReference(),
                                transaction.getAmount(),
                                transaction.getNote());
                dto.setCreatedBy(transaction.getCreatedBy());
                dto.setCreatedDate(transaction.getCreatedDate());
                dto.setUpdatedBy(transaction.getUpdatedBy());
                dto.setUpdatedDate(transaction.getUpdatedDate());
                return dto;
        }

        private SupplierTransaction.TransactionType parseTransactionType(String value) {
                if (value == null || value.trim().isEmpty()) {
                        return SupplierTransaction.TransactionType.PURCHASE;
                }
                try {
                        return SupplierTransaction.TransactionType.valueOf(value.trim().toUpperCase());
                } catch (IllegalArgumentException ex) {
                        throw new IllegalArgumentException("Invalid transaction type: " + value);
                }
        }

        private void validateQuantities(SupplierTransaction.TransactionType type,
                        Long filledReceived, Long emptyReceived, Long filledSent, Long emptySent) {
                long fr = filledReceived != null ? filledReceived : 0L;
                long er = emptyReceived != null ? emptyReceived : 0L;
                long fs = filledSent != null ? filledSent : 0L;
                long es = emptySent != null ? emptySent : 0L;

                if (fr == 0 && er == 0 && fs == 0 && es == 0) {
                        throw new IllegalArgumentException("At least one quantity must be greater than zero");
                }

                switch (type) {
                        case PURCHASE:
                                if (fs > 0 || er > 0) {
                                        throw new IllegalArgumentException(
                                                        "Purchase transactions cannot have filled sent or empty received");
                                }
                                if (fr == 0 && es == 0) {
                                        throw new IllegalArgumentException(
                                                        "Purchase transactions require filled received or empty sent");
                                }
                                break;
                        case BORROW_IN:
                                if (fs > 0 || es > 0) {
                                        throw new IllegalArgumentException(
                                                        "Borrow in cannot have filled sent or empty sent");
                                }
                                if (fr == 0 && er == 0) {
                                        throw new IllegalArgumentException(
                                                        "Borrow in requires filled received or empty received");
                                }
                                break;
                        case BORROW_OUT:
                                if (fr > 0 || er > 0) {
                                        throw new IllegalArgumentException(
                                                        "Borrow out cannot have filled received or empty received");
                                }
                                if (fs == 0 && es == 0) {
                                        throw new IllegalArgumentException(
                                                        "Borrow out requires filled sent or empty sent");
                                }
                                break;
                        case PURCHASE_RETURN:
                                if (fr > 0 || er > 0 || es > 0) {
                                        throw new IllegalArgumentException(
                                                        "Purchase return supports only filled sent quantity");
                                }
                                if (fs == 0) {
                                        throw new IllegalArgumentException(
                                                        "Purchase return requires filled sent quantity");
                                }
                                break;
                        default:
                                throw new IllegalArgumentException("Unsupported transaction type: " + type);
                }
        }

        private void validateBorrowOutAvailability(Long supplierId, Long warehouseId, Long variantId,
                        Long filledSent, Long emptySent, Long excludeId, Long previousFilledSent, Long previousEmptySent) {
                long requestedFilledSent = filledSent != null ? filledSent : 0L;
                long requestedEmptySent = emptySent != null ? emptySent : 0L;

                long borrowedFilled = repository.sumBorrowInFilled(supplierId, warehouseId, variantId, excludeId)
                                - repository.sumBorrowOutFilled(supplierId, warehouseId, variantId, excludeId);
                long borrowedEmpty = repository.sumBorrowInEmpty(supplierId, warehouseId, variantId, excludeId)
                                - repository.sumBorrowOutEmpty(supplierId, warehouseId, variantId, excludeId);

                long availableFilled = borrowedFilled + (previousFilledSent != null ? previousFilledSent : 0L);
                long availableEmpty = borrowedEmpty + (previousEmptySent != null ? previousEmptySent : 0L);

                if (requestedFilledSent > availableFilled) {
                        throw new InvalidOperationException(
                                        "Cannot return more filled cylinders than borrowed. Borrowed available: "
                                                        + availableFilled + ", Requested: " + requestedFilledSent);
                }
                if (requestedEmptySent > availableEmpty) {
                        throw new InvalidOperationException(
                                        "Cannot return more empty cylinders than borrowed. Borrowed available: "
                                                        + availableEmpty + ", Requested: " + requestedEmptySent);
                }
        }

        private void validatePurchaseReturnAvailability(Long supplierId, Long warehouseId, Long variantId,
                        Long filledSent, Long excludeId, Long previousFilledSent) {
                long requestedFilledSent = filledSent != null ? filledSent : 0L;
                long purchasedFilled = repository.sumPurchaseFilled(supplierId, warehouseId, variantId, excludeId);
                long alreadyReturnedFilled = repository.sumPurchaseReturnFilled(supplierId, warehouseId, variantId, excludeId);
                long availableToReturn = (purchasedFilled - alreadyReturnedFilled)
                                + (previousFilledSent != null ? previousFilledSent : 0L);

                if (requestedFilledSent > availableToReturn) {
                        throw new InvalidOperationException(
                                        "Cannot return more filled cylinders than purchased from this supplier. Purchased available: "
                                                        + availableToReturn + ", Requested: " + requestedFilledSent);
                }
        }
}

