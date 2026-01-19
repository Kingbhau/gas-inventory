// ...existing code...
package com.gasagency.service;

import com.gasagency.dto.InventoryStockDTO;
import com.gasagency.dto.WarehouseTransferDTO;
import com.gasagency.entity.InventoryStock;
import com.gasagency.entity.CylinderVariant;
import com.gasagency.entity.Warehouse;
import com.gasagency.repository.InventoryStockRepository;
import com.gasagency.repository.CylinderVariantRepository;
import com.gasagency.repository.WarehouseRepository;
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
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class InventoryStockService {


        private static final Logger logger = LoggerFactory.getLogger(InventoryStockService.class);

        private final InventoryStockRepository repository;
        private final CylinderVariantRepository variantRepository;
        private final WarehouseRepository warehouseRepository;

        public InventoryStockService(InventoryStockRepository repository,
                        CylinderVariantRepository variantRepository,
                        WarehouseRepository warehouseRepository) {
                this.repository = repository;
                this.variantRepository = variantRepository;
                this.warehouseRepository = warehouseRepository;
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

        @Transactional
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
                                stock.getVariant().getName(),
                                stock.getWarehouse() != null ? stock.getWarehouse().getId() : null,
                                stock.getWarehouse() != null ? stock.getWarehouse().getName() : "Unknown",
                                stock.getFilledQty(), stock.getEmptyQty(),
                                stock.getLastUpdated());
        }

        // ============= WAREHOUSE-AWARE METHODS (NEW) =============

        /**
         * Get or create inventory stock for a warehouse and variant
         * Used for warehouse transfers
         */
        @Transactional
        public InventoryStock getOrCreateStock(Warehouse warehouse, CylinderVariant variant) {
                return repository.findByWarehouseAndVariant(warehouse, variant)
                                .orElseGet(() -> {
                                        LoggerUtil.logDatabaseOperation(logger, "CREATE", "INVENTORY_STOCK",
                                                        "warehouseId", warehouse.getId(),
                                                        "variantId", variant.getId(),
                                                        "reason", "Auto-created for warehouse");
                                        InventoryStock newStock = new InventoryStock(warehouse, variant);
                                        newStock.setFilledQty(0L);
                                        newStock.setEmptyQty(0L);
                                        newStock.setLastUpdated(LocalDateTime.now());
                                        return repository.save(newStock);
                                });
        }

        /**
         * Get stock for warehouse and variant (warehouse-aware)
         */
        @Transactional(readOnly = true)
        public InventoryStock getStock(Warehouse warehouse, CylinderVariant variant) {
                LoggerUtil.logDatabaseOperation(logger, "SELECT", "INVENTORY_STOCK",
                                "warehouseId", warehouse.getId(), "variantId", variant.getId());

                return repository.findByWarehouseAndVariant(warehouse, variant)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Stock not found for warehouse: " + warehouse.getName() +
                                                                " and variant: " + variant.getName()));
        }

        /**
         * Get stock with pessimistic lock for warehouse and variant
         * Prevents race conditions during concurrent transfers
         */
        @Transactional
        public InventoryStock getStockWithLock(Warehouse warehouse, CylinderVariant variant) {
                LoggerUtil.logDatabaseOperation(logger, "SELECT_WITH_LOCK", "INVENTORY_STOCK",
                                "warehouseId", warehouse.getId(), "variantId", variant.getId());

                return repository.findByWarehouseAndVariantWithLock(warehouse, variant)
                                .orElse(null); // Return null if not found, for transfer creation
        }

        /**
         * Get all stock for a warehouse
         */
        @Transactional(readOnly = true)
        public List<InventoryStock> getStockByWarehouse(Warehouse warehouse) {
                LoggerUtil.logDatabaseOperation(logger, "SELECT_ALL", "INVENTORY_STOCK",
                                "warehouseId", warehouse.getId());

                return repository.findByWarehouse(warehouse);
        }

        /**
         * Get all stock for a warehouse as DTOs
         */
        @Transactional(readOnly = true)
        public List<InventoryStockDTO> getStockDTOsByWarehouse(Warehouse warehouse) {
                return getStockByWarehouse(warehouse)
                                .stream()
                                .map(this::toDTO)
                                .collect(Collectors.toList());
        }

        /**
         * Update stock with optimistic locking (warehouse-aware)
         * Used during warehouse transfers
         */
        @Transactional
        public void updateStock(InventoryStock stock) {
                LoggerUtil.logBusinessEntry(logger, "UPDATE_STOCK",
                                "stockId", stock.getId(),
                                "filledQty", stock.getFilledQty(),
                                "emptyQty", stock.getEmptyQty());

                stock.setLastUpdated(LocalDateTime.now());

                try {
                        repository.save(stock);
                        LoggerUtil.logBusinessSuccess(logger, "UPDATE_STOCK", "stockId", stock.getId());
                } catch (org.springframework.orm.ObjectOptimisticLockingFailureException e) {
                        LoggerUtil.logBusinessError(logger, "UPDATE_STOCK", "Optimistic lock failure", "stockId",
                                        stock.getId());
                        throw new RuntimeException("Stock was modified by another user. Please retry.");
                }
        }

        /**
         * Check if stock exists for warehouse and variant
         */
        @Transactional(readOnly = true)
        public boolean stockExists(Warehouse warehouse, CylinderVariant variant) {
                return repository.existsByWarehouseAndVariant(warehouse, variant);
        }

        /**
         * Get stock for a specific warehouse and variant with pessimistic lock
         * Used during sales to check and lock inventory
         */
        @Transactional
        public InventoryStock getStockByWarehouseAndVariantWithLock(Warehouse warehouse, CylinderVariant variant) {
                LoggerUtil.logDatabaseOperation(logger, "SELECT_WITH_LOCK", "INVENTORY_STOCK",
                                "warehouseId", warehouse.getId(),
                                "variantId", variant.getId());

                InventoryStock stock = repository.findByWarehouseAndVariantWithLock(warehouse, variant)
                                .orElseThrow(() -> {
                                        LoggerUtil.logBusinessError(logger, "GET_STOCK_WAREHOUSE_VARIANT_WITH_LOCK",
                                                        "Stock not found for warehouse",
                                                        "warehouseId", warehouse.getId(),
                                                        "variantId", variant.getId());
                                        return new ResourceNotFoundException(
                                                        "Variant '" + variant.getName()
                                                                        + "' is not available in warehouse '" +
                                                                        warehouse.getName()
                                                                        + "'. Please check inventory or select a different warehouse.");
                                });

                LoggerUtil.logBusinessSuccess(logger, "GET_STOCK_WAREHOUSE_VARIANT_WITH_LOCK",
                                "warehouseId", warehouse.getId(),
                                "variantId", variant.getId(),
                                "filledQty", stock.getFilledQty(),
                                "emptyQty", stock.getEmptyQty());

                return stock;
        }

        /**
         * Increment filled quantity for a warehouse and variant (warehouse-aware)
         * Used for supplier transactions in specific warehouses
         */
        @Transactional
        public void incrementFilledQty(Warehouse warehouse, CylinderVariant variant, Long quantity) {
                LoggerUtil.logDatabaseOperation(logger, "INCREMENT_FILLED", "INVENTORY_STOCK",
                                "warehouseId", warehouse.getId(),
                                "variantId", variant.getId(),
                                "qty", quantity);

                if (quantity == null || quantity < 0) {
                        LoggerUtil.logBusinessError(logger, "INCREMENT_FILLED_QTY", "Invalid quantity", "qty",
                                        quantity);
                        throw new IllegalArgumentException("Quantity cannot be null or negative");
                }

                if (quantity == 0) {
                        return; // No increment needed
                }

                InventoryStock stock = getOrCreateStock(warehouse, variant);
                long newQty = (stock.getFilledQty() != null ? stock.getFilledQty() : 0L) + quantity;
                stock.setFilledQty(newQty);
                stock.setLastUpdated(LocalDateTime.now());
                repository.save(stock);

                LoggerUtil.logBusinessSuccess(logger, "INCREMENT_FILLED_QTY",
                                "warehouseId", warehouse.getId(),
                                "variantId", variant.getId(),
                                "newQty", newQty);
        }

        /**
         * Decrement empty quantity for a warehouse and variant (warehouse-aware)
         * Used for supplier transactions in specific warehouses
         */
        @Transactional
        public void decrementEmptyQty(Warehouse warehouse, CylinderVariant variant, Long quantity) {
                LoggerUtil.logDatabaseOperation(logger, "DECREMENT_EMPTY", "INVENTORY_STOCK",
                                "warehouseId", warehouse.getId(),
                                "variantId", variant.getId(),
                                "qty", quantity);

                if (quantity == null || quantity < 0) {
                        LoggerUtil.logBusinessError(logger, "DECREMENT_EMPTY_QTY", "Invalid quantity", "qty",
                                        quantity);
                        throw new IllegalArgumentException("Quantity cannot be null or negative");
                }

                if (quantity == 0) {
                        return; // No decrement needed
                }

                InventoryStock stock = getOrCreateStock(warehouse, variant);
                Long currentEmpty = stock.getEmptyQty() != null ? stock.getEmptyQty() : 0L;
                Long newQuantity = currentEmpty - quantity;

                if (newQuantity < 0) {
                        LoggerUtil.logBusinessError(logger, "DECREMENT_EMPTY_QTY", "Insufficient quantity",
                                        "warehouseId", warehouse.getId(),
                                        "current", currentEmpty,
                                        "decrement", quantity);
                        throw new IllegalArgumentException(
                                        "Operation would result in negative empty quantity. Current: "
                                                        + currentEmpty + ", Decrement: " + quantity);
                }

                stock.setEmptyQty(newQuantity);
                stock.setLastUpdated(LocalDateTime.now());
                repository.save(stock);

                LoggerUtil.logBusinessSuccess(logger, "DECREMENT_EMPTY_QTY",
                                "warehouseId", warehouse.getId(),
                                "variantId", variant.getId(),
                                "newQty", newQuantity);
        }

        /**
         * Increment empty quantity for a warehouse and variant (warehouse-aware)
         * Used for supplier transactions in specific warehouses
         */
        @Transactional
        public void incrementEmptyQty(Warehouse warehouse, CylinderVariant variant, Long quantity) {
                LoggerUtil.logDatabaseOperation(logger, "INCREMENT_EMPTY", "INVENTORY_STOCK",
                                "warehouseId", warehouse.getId(),
                                "variantId", variant.getId(),
                                "qty", quantity);

                if (quantity == null || quantity < 0) {
                        LoggerUtil.logBusinessError(logger, "INCREMENT_EMPTY_QTY", "Invalid quantity", "qty",
                                        quantity);
                        throw new IllegalArgumentException("Quantity cannot be null or negative");
                }

                if (quantity == 0) {
                        return; // No increment needed
                }

                InventoryStock stock = getOrCreateStock(warehouse, variant);
                long newQty = (stock.getEmptyQty() != null ? stock.getEmptyQty() : 0L) + quantity;
                stock.setEmptyQty(newQty);
                stock.setLastUpdated(LocalDateTime.now());
                repository.save(stock);

                LoggerUtil.logBusinessSuccess(logger, "INCREMENT_EMPTY_QTY",
                                "warehouseId", warehouse.getId(),
                                "variantId", variant.getId(),
                                "newQty", newQty);
        }

        /**
         * Decrement filled quantity for a warehouse and variant (warehouse-aware)
         * Used for supplier transactions in specific warehouses
         */
        @Transactional
        public void decrementFilledQty(Warehouse warehouse, CylinderVariant variant, Long quantity) {
                LoggerUtil.logDatabaseOperation(logger, "DECREMENT_FILLED", "INVENTORY_STOCK",
                                "warehouseId", warehouse.getId(),
                                "variantId", variant.getId(),
                                "qty", quantity);

                if (quantity == null || quantity < 0) {
                        LoggerUtil.logBusinessError(logger, "DECREMENT_FILLED_QTY", "Invalid quantity", "qty",
                                        quantity);
                        throw new IllegalArgumentException("Quantity cannot be null or negative");
                }

                if (quantity == 0) {
                        return; // No decrement needed
                }

                InventoryStock stock = getOrCreateStock(warehouse, variant);
                Long currentFilled = stock.getFilledQty() != null ? stock.getFilledQty() : 0L;
                Long newQuantity = currentFilled - quantity;

                if (newQuantity < 0) {
                        LoggerUtil.logBusinessError(logger, "DECREMENT_FILLED_QTY", "Insufficient quantity",
                                        "warehouseId", warehouse.getId(),
                                        "current", currentFilled,
                                        "decrement", quantity);
                        throw new IllegalArgumentException(
                                        "Operation would result in negative filled quantity. Current: "
                                                        + currentFilled + ", Decrement: " + quantity);
                }

                stock.setFilledQty(newQuantity);
                stock.setLastUpdated(LocalDateTime.now());
                repository.save(stock);

                LoggerUtil.logBusinessSuccess(logger, "DECREMENT_FILLED_QTY",
                                "warehouseId", warehouse.getId(),
                                "variantId", variant.getId(),
                                "newQty", newQuantity);
        }

        /**
         * Transfer stock from one warehouse to another
         */
        @Transactional
        public WarehouseTransferDTO transferStock(WarehouseTransferDTO transferRequest) {
                LoggerUtil.logBusinessEntry(logger, "TRANSFER_STOCK",
                                "fromWarehouseId", transferRequest.getFromWarehouseId(),
                                "toWarehouseId", transferRequest.getToWarehouseId(),
                                "variantId", transferRequest.getVariantId());

                // Validate that warehouses are different
                if (transferRequest.getFromWarehouseId().equals(transferRequest.getToWarehouseId())) {
                        throw new IllegalArgumentException("Source and destination warehouses must be different");
                }

                // Validate that at least one quantity is being transferred
                Long filledQty = transferRequest.getFilledQty() != null ? transferRequest.getFilledQty() : 0L;
                Long emptyQty = transferRequest.getEmptyQty() != null ? transferRequest.getEmptyQty() : 0L;

                if (filledQty == 0 && emptyQty == 0) {
                        throw new IllegalArgumentException("Please enter quantity to transfer");
                }

                // Get warehouse and variant entities
                Warehouse fromWarehouse = warehouseRepository.findById(transferRequest.getFromWarehouseId())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "From warehouse not found with id: "
                                                                + transferRequest.getFromWarehouseId()));

                Warehouse toWarehouse = warehouseRepository.findById(transferRequest.getToWarehouseId())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "To warehouse not found with id: "
                                                                + transferRequest.getToWarehouseId()));

                CylinderVariant variant = variantRepository.findById(transferRequest.getVariantId())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Variant not found with id: " + transferRequest.getVariantId()));

                // Validate sufficient stock in source warehouse
                InventoryStock fromStock = getStock(fromWarehouse, variant);
                Long currentFilled = fromStock.getFilledQty() != null ? fromStock.getFilledQty() : 0L;
                Long currentEmpty = fromStock.getEmptyQty() != null ? fromStock.getEmptyQty() : 0L;

                if (filledQty > currentFilled) {
                        throw new IllegalArgumentException(
                                        "Insufficient filled cylinders in " + fromWarehouse.getName() +
                                                        ". Available: " + currentFilled + ", Required: " + filledQty);
                }
                if (emptyQty > currentEmpty) {
                        throw new IllegalArgumentException(
                                        "Insufficient empty cylinders in " + fromWarehouse.getName() +
                                                        ". Available: " + currentEmpty + ", Required: " + emptyQty);
                }

                // Decrement from source warehouse
                if (filledQty > 0) {
                        decrementFilledQty(fromWarehouse, variant, filledQty);
                }
                if (emptyQty > 0) {
                        decrementEmptyQty(fromWarehouse, variant, emptyQty);
                }

                // Increment to destination warehouse
                if (filledQty > 0) {
                        incrementFilledQty(toWarehouse, variant, filledQty);
                }
                if (emptyQty > 0) {
                        incrementEmptyQty(toWarehouse, variant, emptyQty);
                }

                LoggerUtil.logBusinessSuccess(logger, "TRANSFER_STOCK",
                                "fromWarehouseId", fromWarehouse.getId(),
                                "toWarehouseId", toWarehouse.getId(),
                                "variantId", variant.getId(),
                                "filledQty", filledQty,
                                "emptyQty", emptyQty);

                transferRequest.setFromWarehouseName(fromWarehouse.getName());
                transferRequest.setToWarehouseName(toWarehouse.getName());
                transferRequest.setVariantName(variant.getName());

                return transferRequest;
        }

        @Transactional
        public void setupWarehouseInventory(Long warehouseId, List<Map<String, Object>> inventoryItems) {
                LoggerUtil.logBusinessEntry(logger, "SETUP_WAREHOUSE_INVENTORY", "warehouseId", warehouseId,
                                "itemsCount", inventoryItems.size());

                Warehouse warehouse = warehouseRepository.findById(warehouseId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Warehouse not found with id: " + warehouseId));

                for (Map<String, Object> item : inventoryItems) {
                        Long variantId = Long.valueOf(item.get("variantId").toString());
                        Long filledQty = Long.valueOf(item.get("filledQty").toString());
                        Long emptyQty = Long.valueOf(item.get("emptyQty").toString());

                        CylinderVariant variant = variantRepository.findById(variantId)
                                        .orElseThrow(() -> new ResourceNotFoundException(
                                                        "Variant not found with id: " + variantId));

                        // Check if inventory exists for this warehouse-variant combination
                        InventoryStock stock = repository.findByWarehouseAndVariant(warehouse, variant)
                                        .orElse(null);

                        if (stock == null) {
                                // Create new inventory record
                                stock = new InventoryStock(warehouse, variant);
                                stock.setFilledQty(filledQty);
                                stock.setEmptyQty(emptyQty);
                                stock.setLastUpdated(LocalDateTime.now());
                                repository.save(stock);
                                LoggerUtil.logAudit("CREATE", "INVENTORY_STOCK", "warehouseId", warehouse.getId(),
                                                "variantId", variant.getId(), "filledQty", filledQty, "emptyQty",
                                                emptyQty);
                        } else {
                                // Update existing inventory record
                                stock.setFilledQty(filledQty);
                                stock.setEmptyQty(emptyQty);
                                stock.setLastUpdated(LocalDateTime.now());
                                repository.save(stock);
                                LoggerUtil.logAudit("UPDATE", "INVENTORY_STOCK", "warehouseId", warehouse.getId(),
                                                "variantId", variant.getId(), "filledQty", filledQty, "emptyQty",
                                                emptyQty);
                        }
                }

                LoggerUtil.logBusinessSuccess(logger, "SETUP_WAREHOUSE_INVENTORY", "warehouseId", warehouseId);
        }
}
