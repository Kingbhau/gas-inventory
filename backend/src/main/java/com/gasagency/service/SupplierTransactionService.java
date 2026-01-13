
package com.gasagency.service;

import com.gasagency.dto.CreateSupplierTransactionRequestDTO;
import com.gasagency.dto.SupplierTransactionDTO;
import com.gasagency.entity.*;
import com.gasagency.repository.*;
import com.gasagency.exception.ResourceNotFoundException;
import com.gasagency.util.LoggerUtil;
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
        private final InventoryStockService inventoryStockService;

        public SupplierTransactionService(SupplierTransactionRepository repository,
                        SupplierRepository supplierRepository,
                        CylinderVariantRepository variantRepository,
                        InventoryStockService inventoryStockService) {
                this.repository = repository;
                this.supplierRepository = supplierRepository;
                this.variantRepository = variantRepository;
                this.inventoryStockService = inventoryStockService;
        }

        @Transactional
        public SupplierTransactionDTO updateTransaction(Long id, CreateSupplierTransactionRequestDTO request) {
                LoggerUtil.logBusinessEntry(logger, "UPDATE_TRANSACTION", "id", id, "supplierId",
                                request.getSupplierId(), "variantId", request.getVariantId());

                SupplierTransaction transaction = repository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Transaction not found with id: " + id));

                // Validate request
                if (request == null || request.getSupplierId() == null || request.getVariantId() == null) {
                        LoggerUtil.logBusinessError(logger, "UPDATE_TRANSACTION", "Invalid request", "reason",
                                        "supplier or variant ID is null");
                        throw new IllegalArgumentException("Supplier ID and Variant ID cannot be null");
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

                Supplier supplier = supplierRepository.findById(request.getSupplierId())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Supplier not found with id: " + request.getSupplierId()));
                CylinderVariant variant = variantRepository.findById(request.getVariantId())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Variant not found with id: " + request.getVariantId()));

                // Inventory adjustment: revert old transaction, apply new
                inventoryStockService.decrementFilledQty(transaction.getVariant().getId(),
                                transaction.getFilledReceived());
                inventoryStockService.incrementEmptyQty(transaction.getVariant().getId(), transaction.getEmptySent());

                inventoryStockService.incrementFilledQty(variant.getId(), request.getFilledReceived());
                inventoryStockService.decrementEmptyQty(variant.getId(), request.getEmptySent());

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
                LoggerUtil.logBusinessEntry(logger, "RECORD_TRANSACTION", "supplierId",
                                request != null ? request.getSupplierId() : "null", "variantId",
                                request != null ? request.getVariantId() : "null");

                // Validate request
                if (request == null || request.getSupplierId() == null || request.getVariantId() == null) {
                        LoggerUtil.logBusinessError(logger, "RECORD_TRANSACTION", "Invalid request", "reason",
                                        "supplier or variant ID is null");
                        throw new IllegalArgumentException("Supplier ID and Variant ID cannot be null");
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

                // Create transaction
                SupplierTransaction transaction = new SupplierTransaction(
                                supplier, variant, LocalDate.now(),
                                request.getFilledReceived(), request.getEmptySent(), request.getReference(),
                                request.getAmount());
                transaction = repository.save(transaction);

                // Update inventory
                inventoryStockService.incrementFilledQty(variant.getId(), request.getFilledReceived());
                inventoryStockService.decrementEmptyQty(variant.getId(), request.getEmptySent());

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

        private SupplierTransactionDTO toDTO(SupplierTransaction transaction) {
                return new SupplierTransactionDTO(
                                transaction.getId(),
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
