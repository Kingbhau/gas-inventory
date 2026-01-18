package com.gasagency.service;

import com.gasagency.dto.WarehouseTransferDTO;
import com.gasagency.entity.WarehouseTransfer;
import com.gasagency.entity.Warehouse;
import com.gasagency.entity.CylinderVariant;
import com.gasagency.entity.InventoryStock;
import com.gasagency.exception.ResourceNotFoundException;
import com.gasagency.exception.InvalidOperationException;
import com.gasagency.repository.WarehouseTransferRepository;
import com.gasagency.util.ReferenceNumberGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * WarehouseTransferService - Manages cylinder transfers between warehouses
 * Handles:
 * - Transfer validation (enough stock, different warehouses, active status)
 * - Inventory deduction and addition (atomic transaction)
 * - Concurrency control (optimistic locking on inventory)
 * - Race condition prevention (pessimistic locking during transfer)
 */
@Service
@Transactional
public class WarehouseTransferService {

    private static final Logger logger = LoggerFactory.getLogger(WarehouseTransferService.class);

    @Autowired
    private WarehouseTransferRepository warehouseTransferRepository;

    @Autowired
    private WarehouseService warehouseService;

    @Autowired
    private InventoryStockService inventoryStockService;

    @Autowired
    private CylinderVariantService cylinderVariantService;

    @Autowired
    private ReferenceNumberGenerator referenceNumberGenerator;

    /**
     * Perform warehouse transfer with comprehensive validation
     * ATOMIC TRANSACTION: If any step fails, entire transfer is rolled back
     */
    public WarehouseTransferDTO transferCylinders(WarehouseTransferDTO transferDTO) {
        // 1. Validate input
        validateTransferInput(transferDTO);

        // 2. Get and validate warehouses
        Warehouse fromWarehouse = warehouseService.getWarehouseEntity(transferDTO.getFromWarehouseId());
        Warehouse toWarehouse = warehouseService.getWarehouseEntity(transferDTO.getToWarehouseId());

        warehouseService.validateWarehouseIsActive(fromWarehouse.getId());
        warehouseService.validateWarehouseIsActive(toWarehouse.getId());
        warehouseService.validateDifferentWarehouses(fromWarehouse.getId(), toWarehouse.getId());

        // 3. Get and validate variant
        CylinderVariant variant = cylinderVariantService.getCylinderVariantEntity(transferDTO.getVariantId());

        // 4. Check source warehouse stock (with pessimistic lock to prevent race
        // conditions)
        InventoryStock fromStock = inventoryStockService.getStockWithLock(fromWarehouse, variant);
        if (fromStock == null) {
            throw new InvalidOperationException("No stock record found for variant in source warehouse");
        }

        // 5. Validate sufficient stock
        Long filledQty = transferDTO.getFilledQty() != null ? transferDTO.getFilledQty() : 0L;
        Long emptyQty = transferDTO.getEmptyQty() != null ? transferDTO.getEmptyQty() : 0L;

        if (fromStock.getFilledQty() < filledQty) {
            throw new InvalidOperationException(
                    "Insufficient filled cylinders in " + fromWarehouse.getName() +
                            ". Available: " + fromStock.getFilledQty() +
                            ", Required: " + filledQty);
        }

        if (fromStock.getEmptyQty() < emptyQty) {
            throw new InvalidOperationException(
                    "Insufficient empty cylinders in " + fromWarehouse.getName() +
                            ". Available: " + fromStock.getEmptyQty() +
                            ", Required: " + emptyQty);
        }

        // 6. Get or create destination warehouse stock
        InventoryStock toStock = inventoryStockService.getOrCreateStock(toWarehouse, variant);

        try {
            // 7. Deduct from source (atomic with version check)
            if (filledQty > 0) {
                fromStock.setFilledQty(fromStock.getFilledQty() - filledQty);
            }
            if (emptyQty > 0) {
                fromStock.setEmptyQty(fromStock.getEmptyQty() - emptyQty);
            }
            inventoryStockService.updateStock(fromStock);

            // 8. Add to destination (atomic with version check)
            if (filledQty > 0) {
                toStock.setFilledQty(toStock.getFilledQty() + filledQty);
            }
            if (emptyQty > 0) {
                toStock.setEmptyQty(toStock.getEmptyQty() + emptyQty);
            }
            inventoryStockService.updateStock(toStock);

            // 9. Create transfer record (storing total quantity for reference)
            Long totalQty = filledQty + emptyQty;
            WarehouseTransfer transfer = new WarehouseTransfer(fromWarehouse, toWarehouse, variant,
                    totalQty);
            if (transferDTO.getNotes() != null && !transferDTO.getNotes().trim().isEmpty()) {
                transfer.setNotes(transferDTO.getNotes().trim());
            }
            if (transferDTO.getTransferDate() != null) {
                transfer.setTransferDate(transferDTO.getTransferDate());
            }

            // Generate reference number BEFORE initial save
            String referenceNumber = referenceNumberGenerator.generateWarehouseTransferReference(
                    fromWarehouse, toWarehouse);
            transfer.setReferenceNumber(referenceNumber);

            WarehouseTransfer savedTransfer = warehouseTransferRepository.save(transfer);
            logger.info("Warehouse transfer created with id: {} - Reference: {}",
                    savedTransfer.getId(), referenceNumber);

            return convertToDTO(savedTransfer);

        } catch (org.springframework.orm.ObjectOptimisticLockingFailureException e) {
            throw new InvalidOperationException(
                    "Inventory stock was modified by another user. Please retry the transfer.");
        }
    }

    /**
     * Get all transfers (audit trail)
     */
    @Transactional(readOnly = true)
    public List<WarehouseTransferDTO> getAllTransfers() {
        return warehouseTransferRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get transfers for a specific warehouse (both incoming and outgoing)
     */
    @Transactional(readOnly = true)
    public List<WarehouseTransferDTO> getTransfersForWarehouse(Long warehouseId) {
        Warehouse warehouse = warehouseService.getWarehouseEntity(warehouseId);

        return warehouseTransferRepository.findAllTransfersForWarehouse(warehouse)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get transfers from specific warehouse (outgoing)
     */
    @Transactional(readOnly = true)
    public List<WarehouseTransferDTO> getTransfersFrom(Long warehouseId) {
        Warehouse warehouse = warehouseService.getWarehouseEntity(warehouseId);

        return warehouseTransferRepository.findTransfersFromWarehouse(warehouse)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get transfers to specific warehouse (incoming)
     */
    @Transactional(readOnly = true)
    public List<WarehouseTransferDTO> getTransfersTo(Long warehouseId) {
        Warehouse warehouse = warehouseService.getWarehouseEntity(warehouseId);

        return warehouseTransferRepository.findTransfersToWarehouse(warehouse)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get transfer by ID
     */
    @Transactional(readOnly = true)
    public WarehouseTransferDTO getTransferById(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Transfer ID must be positive");
        }

        return warehouseTransferRepository.findById(id)
                .map(this::convertToDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Transfer not found with ID: " + id));
    }

    /**
     * Get transfers between two warehouses
     */
    @Transactional(readOnly = true)
    public List<WarehouseTransferDTO> getTransfersBetweenWarehouses(Long fromWarehouseId, Long toWarehouseId) {
        Warehouse fromWarehouse = warehouseService.getWarehouseEntity(fromWarehouseId);
        Warehouse toWarehouse = warehouseService.getWarehouseEntity(toWarehouseId);

        return warehouseTransferRepository.findTransfers(fromWarehouse, toWarehouse)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Validate transfer input
     */
    private void validateTransferInput(WarehouseTransferDTO transferDTO) {
        if (transferDTO.getFromWarehouseId() == null || transferDTO.getFromWarehouseId() <= 0) {
            throw new IllegalArgumentException("Source warehouse ID is required and must be positive");
        }

        if (transferDTO.getToWarehouseId() == null || transferDTO.getToWarehouseId() <= 0) {
            throw new IllegalArgumentException("Destination warehouse ID is required and must be positive");
        }

        if (transferDTO.getVariantId() == null || transferDTO.getVariantId() <= 0) {
            throw new IllegalArgumentException("Variant ID is required and must be positive");
        }

        Long filledQty = transferDTO.getFilledQty() != null ? transferDTO.getFilledQty() : 0L;
        Long emptyQty = transferDTO.getEmptyQty() != null ? transferDTO.getEmptyQty() : 0L;

        if (filledQty < 0 || emptyQty < 0) {
            throw new IllegalArgumentException("Quantities cannot be negative");
        }

        if (filledQty == 0 && emptyQty == 0) {
            throw new IllegalArgumentException("At least one quantity must be positive");
        }

        if ((filledQty + emptyQty) > 10000) { // Sanity check
            throw new IllegalArgumentException("Transfer quantity exceeds maximum allowed");
        }
    }

    /**
     * Convert WarehouseTransfer entity to DTO
     */
    private WarehouseTransferDTO convertToDTO(WarehouseTransfer transfer) {
        WarehouseTransferDTO dto = new WarehouseTransferDTO();
        dto.setId(transfer.getId());
        dto.setFromWarehouseId(transfer.getFromWarehouse().getId());
        dto.setToWarehouseId(transfer.getToWarehouse().getId());
        dto.setFromWarehouseName(transfer.getFromWarehouse().getName());
        dto.setToWarehouseName(transfer.getToWarehouse().getName());
        dto.setVariantId(transfer.getVariant().getId());
        dto.setVariantName(transfer.getVariant().getName());
        // Store the total quantity back for compatibility
        dto.setFilledQty(transfer.getQuantity());
        dto.setEmptyQty(0L);
        dto.setTransferDate(transfer.getTransferDate());
        dto.setCreatedAt(transfer.getCreatedDate());
        dto.setNotes(transfer.getNotes());
        dto.setReferenceNumber(transfer.getReferenceNumber());
        dto.setVersion(transfer.getVersion());
        return dto;
    }
}
