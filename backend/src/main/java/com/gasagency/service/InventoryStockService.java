// ...existing code...
package com.gasagency.service;

import com.gasagency.dto.InventoryStockDTO;
import com.gasagency.entity.InventoryStock;
import com.gasagency.entity.CylinderVariant;
import com.gasagency.repository.InventoryStockRepository;
import com.gasagency.repository.CylinderVariantRepository;
import com.gasagency.exception.ResourceNotFoundException;
import com.gasagency.util.LoggerUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class InventoryStockService {
        /**
         * Increment the empty quantity for a given variant by the specified qty.
         * Throws ResourceNotFoundException if stock or variant not found.
         */
        @Transactional
        public void incrementEmptyQty(Long variantId, Long qty) {
                LoggerUtil.logBusinessEntry(logger, "INCREMENT_EMPTY_QTY", "variantId", variantId, "qty", qty);
                if (qty == null) {
                        throw new IllegalArgumentException("Quantity to increment cannot be null");
                }
                if (qty == 0) {
                        // No increment needed, but not an error. Log and return.
                        LoggerUtil.logBusinessSuccess(logger, "INCREMENT_EMPTY_QTY", "variantId", variantId,
                                        "newEmptyQty", "No increment (qty=0)");
                        return;
                }
                if (qty < 0) {
                        throw new IllegalArgumentException("Quantity to increment cannot be negative");
                }
                CylinderVariant variant = variantRepository.findById(variantId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Variant not found with id: " + variantId));
                InventoryStock stock = repository.findByVariant(variant)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Stock not found for variant id: " + variantId));
                Long currentEmpty = stock.getEmptyQty() != null ? stock.getEmptyQty() : 0L;
                stock.setEmptyQty(currentEmpty + qty);
                repository.save(stock);
                LoggerUtil.logBusinessSuccess(logger, "INCREMENT_EMPTY_QTY", "variantId", variantId, "newEmptyQty",
                                stock.getEmptyQty());
                LoggerUtil.logAudit("UPDATE", "INVENTORY_STOCK", "stockId", stock.getId(), "variantId", variantId,
                                "emptyQty", stock.getEmptyQty());
        }

        private static final Logger logger = LoggerFactory.getLogger(InventoryStockService.class);

        private final InventoryStockRepository repository;
        private final CylinderVariantRepository variantRepository;

        public InventoryStockService(InventoryStockRepository repository,
                        CylinderVariantRepository variantRepository) {
                this.repository = repository;
                this.variantRepository = variantRepository;
        }

        @Transactional
        public InventoryStockDTO createStock(Long variantId) {
                LoggerUtil.logBusinessEntry(logger, "CREATE_STOCK", "variantId", variantId);

                CylinderVariant variant = variantRepository.findById(variantId)
                                .orElseThrow(() -> {
                                        LoggerUtil.logBusinessError(logger, "CREATE_STOCK", "Variant not found",
                                                        "variantId", variantId);
                                        return new ResourceNotFoundException(
                                                        "Variant not found with id: " + variantId);
                                });
                InventoryStock stock = new InventoryStock(variant);
                repository.save(stock);

                LoggerUtil.logBusinessSuccess(logger, "CREATE_STOCK", "id", stock.getId(), "variant",
                                variant.getName());
                LoggerUtil.logAudit("CREATE", "INVENTORY_STOCK", "stockId", stock.getId(), "variantId",
                                variant.getId());

                return toDTO(stock);
        }

        public InventoryStockDTO getStockById(Long id) {
                LoggerUtil.logDatabaseOperation(logger, "SELECT", "INVENTORY_STOCK", "id", id);

                InventoryStock stock = repository.findById(id)
                                .orElseThrow(() -> {
                                        LoggerUtil.logBusinessError(logger, "GET_STOCK", "Stock not found", "id", id);
                                        return new ResourceNotFoundException("Stock not found with id: " + id);
                                });
                return toDTO(stock);
        }

        public InventoryStockDTO getStockByVariant(Long variantId) {
                LoggerUtil.logDatabaseOperation(logger, "SELECT", "INVENTORY_STOCK", "variantId", variantId);

                CylinderVariant variant = variantRepository.findById(variantId)
                                .orElseThrow(() -> {
                                        LoggerUtil.logBusinessError(logger, "GET_STOCK_BY_VARIANT", "Variant not found",
                                                        "variantId", variantId);
                                        return new ResourceNotFoundException(
                                                        "Variant not found with id: " + variantId);
                                });
                InventoryStock stock = repository.findByVariant(variant)
                                .orElseThrow(() -> {
                                        LoggerUtil.logBusinessError(logger, "GET_STOCK_BY_VARIANT", "Stock not found",
                                                        "variantId", variantId);
                                        return new ResourceNotFoundException("Stock not found for variant");
                                });
                return toDTO(stock);
        }

        public List<InventoryStockDTO> getAllStock() {
                LoggerUtil.logDatabaseOperation(logger, "SELECT_ALL", "INVENTORY_STOCK");

                return repository.findAll().stream()
                                .map(this::toDTO)
                                .collect(Collectors.toList());
        }

        public Page<InventoryStockDTO> getAllStock(Pageable pageable) {
                LoggerUtil.logDatabaseOperation(logger, "SELECT_PAGINATED", "INVENTORY_STOCK", "page",
                                pageable.getPageNumber(), "size", pageable.getPageSize());

                return repository.findAll(pageable)
                                .map(this::toDTO);
        }

        public InventoryStock getStockEntityByVariant(Long variantId) {
                LoggerUtil.logDatabaseOperation(logger, "SELECT", "INVENTORY_STOCK", "variantId", variantId);

                CylinderVariant variant = variantRepository.findById(variantId)
                                .orElseThrow(() -> {
                                        LoggerUtil.logBusinessError(logger, "GET_STOCK_ENTITY", "Variant not found",
                                                        "variantId", variantId);
                                        return new ResourceNotFoundException(
                                                        "Variant not found with id: " + variantId);
                                });
                return repository.findByVariant(variant)
                                .orElseThrow(() -> {
                                        LoggerUtil.logBusinessError(logger, "GET_STOCK_ENTITY", "Stock not found",
                                                        "variantId", variantId);
                                        return new ResourceNotFoundException("Stock not found for variant");
                                });
        }

        public InventoryStock getStockEntityWithLock(Long variantId) {
                LoggerUtil.logDatabaseOperation(logger, "SELECT_WITH_LOCK", "INVENTORY_STOCK", "variantId", variantId);

                CylinderVariant variant = variantRepository.findById(variantId)
                                .orElseThrow(() -> {
                                        LoggerUtil.logBusinessError(logger, "GET_STOCK_WITH_LOCK", "Variant not found",
                                                        "variantId", variantId);
                                        return new ResourceNotFoundException(
                                                        "Variant not found with id: " + variantId);
                                });
                return repository.findByVariantWithLock(variant)
                                .orElseThrow(() -> {
                                        LoggerUtil.logBusinessError(logger, "GET_STOCK_WITH_LOCK", "Stock not found",
                                                        "variantId", variantId);
                                        return new ResourceNotFoundException("Stock not found for variant");
                                });
        }

        @Transactional
        public void updateFilledQty(Long variantId, Long filledQty) {
                LoggerUtil.logBusinessEntry(logger, "UPDATE_FILLED_QTY", "variantId", variantId, "qty", filledQty);

                if (filledQty < 0) {
                        LoggerUtil.logBusinessError(logger, "UPDATE_FILLED_QTY", "Negative filledQty", "qty",
                                        filledQty);
                        throw new IllegalArgumentException("Filled quantity cannot be negative");
                }
                CylinderVariant variant = variantRepository.findById(variantId)
                                .orElseThrow(() -> {
                                        LoggerUtil.logBusinessError(logger, "UPDATE_FILLED_QTY", "Variant not found",
                                                        "variantId", variantId);
                                        return new ResourceNotFoundException(
                                                        "Variant not found with id: " + variantId);
                                });
                InventoryStock stock = repository.findByVariantWithLock(variant)
                                .orElseThrow(() -> {
                                        LoggerUtil.logBusinessError(logger, "UPDATE_FILLED_QTY", "Stock not found",
                                                        "variantId", variantId);
                                        return new ResourceNotFoundException("Stock not found for variant");
                                });
                stock.setFilledQty(filledQty);
                stock.setLastUpdated(LocalDateTime.now());
                repository.save(stock);

                LoggerUtil.logBusinessSuccess(logger, "UPDATE_FILLED_QTY", "variantId", variantId, "newQty", filledQty);
        }

        @Transactional
        public void updateEmptyQty(Long variantId, Long emptyQty) {
                LoggerUtil.logBusinessEntry(logger, "UPDATE_EMPTY_QTY", "variantId", variantId, "qty", emptyQty);

                if (emptyQty < 0) {
                        LoggerUtil.logBusinessError(logger, "UPDATE_EMPTY_QTY", "Negative emptyQty", "qty", emptyQty);
                        throw new IllegalArgumentException("Empty quantity cannot be negative");
                }
                CylinderVariant variant = variantRepository.findById(variantId)
                                .orElseThrow(() -> {
                                        LoggerUtil.logBusinessError(logger, "UPDATE_EMPTY_QTY", "Variant not found",
                                                        "variantId", variantId);
                                        return new ResourceNotFoundException(
                                                        "Variant not found with id: " + variantId);
                                });
                InventoryStock stock = repository.findByVariantWithLock(variant)
                                .orElseThrow(() -> {
                                        LoggerUtil.logBusinessError(logger, "UPDATE_EMPTY_QTY", "Stock not found",
                                                        "variantId", variantId);
                                        return new ResourceNotFoundException("Stock not found for variant");
                                });
                stock.setEmptyQty(emptyQty);
                stock.setLastUpdated(LocalDateTime.now());
                repository.save(stock);

                LoggerUtil.logBusinessSuccess(logger, "UPDATE_EMPTY_QTY", "variantId", variantId, "newQty", emptyQty);
        }

        @Transactional
        public void incrementFilledQty(Long variantId, Long quantity) {
                LoggerUtil.logDatabaseOperation(logger, "INCREMENT_FILLED", "INVENTORY_STOCK", "variantId", variantId,
                                "qty", quantity);

                if (quantity < 0) {
                        LoggerUtil.logBusinessError(logger, "INCREMENT_FILLED_QTY", "Negative quantity", "qty",
                                        quantity);
                        throw new IllegalArgumentException("Quantity cannot be negative");
                }
                CylinderVariant variant = variantRepository.findById(variantId)
                                .orElseThrow(() -> {
                                        LoggerUtil.logBusinessError(logger, "INCREMENT_FILLED_QTY", "Variant not found",
                                                        "variantId", variantId);
                                        return new ResourceNotFoundException(
                                                        "Variant not found with id: " + variantId);
                                });
                InventoryStock stock = repository.findByVariantWithLock(variant)
                                .orElseGet(() -> {
                                        // Create new inventory stock if it doesn't exist
                                        LoggerUtil.logDatabaseOperation(logger, "CREATE", "INVENTORY_STOCK",
                                                        "variantId", variantId, "reason",
                                                        "Auto-created for transaction");
                                        InventoryStock newStock = new InventoryStock();
                                        newStock.setVariant(variant);
                                        newStock.setFilledQty(0L);
                                        newStock.setEmptyQty(0L);
                                        newStock.setLastUpdated(LocalDateTime.now());
                                        return repository.save(newStock);
                                });
                long newQty = stock.getFilledQty() + quantity;
                stock.setFilledQty(newQty);
                stock.setLastUpdated(LocalDateTime.now());
                repository.save(stock);

                LoggerUtil.logBusinessSuccess(logger, "INCREMENT_FILLED_QTY", "variantId", variantId, "newQty", newQty);
        }

        @Transactional
        public void decrementFilledQtyWithCheck(Long variantId, Long quantity) {
                LoggerUtil.logBusinessEntry(logger, "DECREMENT_FILLED_WITH_CHECK", "variantId", variantId, "qty",
                                quantity);

                CylinderVariant variant = variantRepository.findById(variantId)
                                .orElseThrow(() -> {
                                        LoggerUtil.logBusinessError(logger, "DECREMENT_FILLED_QTY_CHECK",
                                                        "Variant not found", "variantId", variantId);
                                        return new ResourceNotFoundException(
                                                        "Variant not found with id: " + variantId);
                                });
                InventoryStock stock = repository.findByVariantWithLock(variant)
                                .orElseThrow(() -> {
                                        LoggerUtil.logBusinessError(logger, "DECREMENT_FILLED_QTY_CHECK",
                                                        "Stock not found", "variantId", variantId);
                                        return new ResourceNotFoundException("Stock not found for variant");
                                });

                if (quantity < 0) {
                        LoggerUtil.logBusinessError(logger, "DECREMENT_FILLED_QTY_CHECK", "Negative quantity", "qty",
                                        quantity);
                        throw new IllegalArgumentException("Quantity cannot be negative");
                }

                Long newQuantity = stock.getFilledQty() - quantity;
                if (newQuantity < 0) {
                        LoggerUtil.logBusinessError(logger, "DECREMENT_FILLED_QTY_CHECK", "Insufficient quantity",
                                        "current", stock.getFilledQty(), "decrement", quantity);
                        throw new IllegalArgumentException(
                                        "Operation would result in negative filled quantity. Current: "
                                                        + stock.getFilledQty() + ", Decrement: " + quantity);
                }

                stock.setFilledQty(newQuantity);
                stock.setLastUpdated(LocalDateTime.now());
                repository.save(stock);

                LoggerUtil.logBusinessSuccess(logger, "DECREMENT_FILLED_QTY_CHECK", "variantId", variantId, "newQty",
                                newQuantity);
        }

        @Transactional
        public void decrementFilledQty(Long variantId, Long quantity) {
                LoggerUtil.logDatabaseOperation(logger, "DECREMENT_FILLED", "INVENTORY_STOCK", "variantId", variantId,
                                "qty", quantity);

                if (quantity < 0) {
                        LoggerUtil.logBusinessError(logger, "DECREMENT_FILLED_QTY", "Negative quantity", "qty",
                                        quantity);
                        throw new IllegalArgumentException("Quantity cannot be negative");
                }
                CylinderVariant variant = variantRepository.findById(variantId)
                                .orElseThrow(() -> {
                                        LoggerUtil.logBusinessError(logger, "DECREMENT_FILLED_QTY", "Variant not found",
                                                        "variantId", variantId);
                                        return new ResourceNotFoundException(
                                                        "Variant not found with id: " + variantId);
                                });
                InventoryStock stock = repository.findByVariantWithLock(variant)
                                .orElseThrow(() -> {
                                        LoggerUtil.logBusinessError(logger, "DECREMENT_FILLED_QTY", "Stock not found",
                                                        "variantId", variantId);
                                        return new ResourceNotFoundException("Stock not found for variant");
                                });
                long newQty = stock.getFilledQty() - quantity;
                if (newQty < 0) {
                        LoggerUtil.logBusinessError(logger, "DECREMENT_FILLED_QTY", "Resulting filledQty negative",
                                        "newQty", newQty);
                        throw new IllegalArgumentException(
                                        "Not enough filled cylinders in inventory to complete this operation. Please check your supplier and customer transactions.");
                }
                stock.setFilledQty(newQty);
                stock.setLastUpdated(LocalDateTime.now());
                repository.save(stock);

                LoggerUtil.logBusinessSuccess(logger, "DECREMENT_FILLED_QTY", "variantId", variantId, "newQty", newQty);
                repository.save(stock);

                LoggerUtil.logBusinessSuccess(logger, "INCREMENT_EMPTY_QTY", "variantId", variantId, "newQty",
                                stock.getEmptyQty());

        }

        @Transactional
        public void decrementEmptyQty(Long variantId, Long quantity) {
                LoggerUtil.logDatabaseOperation(logger, "DECREMENT_EMPTY", "INVENTORY_STOCK", "variantId", variantId,
                                "qty", quantity);

                if (quantity < 0) {
                        LoggerUtil.logBusinessError(logger, "DECREMENT_EMPTY_QTY", "Negative quantity", "qty",
                                        quantity);
                        throw new IllegalArgumentException("Quantity cannot be negative");
                }
                CylinderVariant variant = variantRepository.findById(variantId)
                                .orElseThrow(() -> {
                                        LoggerUtil.logBusinessError(logger, "DECREMENT_EMPTY_QTY", "Variant not found",
                                                        "variantId", variantId);
                                        return new ResourceNotFoundException(
                                                        "Variant not found with id: " + variantId);
                                });
                InventoryStock stock = repository.findByVariantWithLock(variant)
                                .orElseGet(() -> {
                                        // Create new inventory stock if it doesn't exist
                                        LoggerUtil.logDatabaseOperation(logger, "CREATE", "INVENTORY_STOCK",
                                                        "variantId", variantId, "reason",
                                                        "Auto-created for transaction");
                                        InventoryStock newStock = new InventoryStock();
                                        newStock.setVariant(variant);
                                        newStock.setFilledQty(0L);
                                        newStock.setEmptyQty(0L);
                                        newStock.setLastUpdated(LocalDateTime.now());
                                        return repository.save(newStock);
                                });

                Long newQuantity = stock.getEmptyQty() - quantity;
                if (newQuantity < 0) {
                        LoggerUtil.logBusinessError(logger, "DECREMENT_EMPTY_QTY", "Insufficient quantity", "current",
                                        stock.getEmptyQty(), "decrement", quantity);
                        throw new IllegalArgumentException(
                                        "Operation would result in negative empty quantity. Current: "
                                                        + stock.getEmptyQty() + ", Decrement: " + quantity);
                }

                stock.setEmptyQty(newQuantity);
                stock.setLastUpdated(LocalDateTime.now());
                repository.save(stock);

                LoggerUtil.logBusinessSuccess(logger, "DECREMENT_EMPTY_QTY", "variantId", variantId, "newQty",
                                newQuantity);
        }

        private InventoryStockDTO toDTO(InventoryStock stock) {
                return new InventoryStockDTO(stock.getId(), stock.getVariant().getId(),
                                stock.getVariant().getName(), stock.getFilledQty(), stock.getEmptyQty(),
                                stock.getLastUpdated());
        }
}
