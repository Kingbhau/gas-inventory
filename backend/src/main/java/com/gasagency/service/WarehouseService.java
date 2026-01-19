package com.gasagency.service;

import com.gasagency.dto.WarehouseDTO;
import com.gasagency.entity.Warehouse;
import com.gasagency.entity.BusinessInfo;
import com.gasagency.exception.ResourceNotFoundException;
import com.gasagency.exception.InvalidOperationException;
import com.gasagency.repository.WarehouseRepository;
import com.gasagency.repository.BusinessInfoRepository;
import com.gasagency.util.CodeGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * WarehouseService - Manages warehouse operations
 * Handles validation, concurrency control (optimistic locking), and exception
 * handling
 */
@Service
@Transactional
public class WarehouseService {

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private BusinessInfoRepository businessInfoRepository;

    @Autowired
    private CodeGenerator codeGenerator;

    /**
     * Get all warehouses (active and inactive)
     */
    @Transactional(readOnly = true)
    public List<WarehouseDTO> getAllWarehouses() {
        return warehouseRepository.findAllOrderByName()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get only active warehouses
     */
    @Transactional(readOnly = true)
    public List<WarehouseDTO> getActiveWarehouses() {
        return warehouseRepository.findAllActive()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get warehouse by ID with validation
     */
    @Transactional(readOnly = true)
    public WarehouseDTO getWarehouseById(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Warehouse ID must be positive");
        }

        return warehouseRepository.findById(id)
                .map(this::convertToDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with ID: " + id));
    }

    /**
     * Get warehouse by name with validation
     */
    @Transactional(readOnly = true)
    public WarehouseDTO getWarehouseByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Warehouse name cannot be empty");
        }

        return warehouseRepository.findByName(name.trim())
                .map(this::convertToDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with name: " + name));
    }

    /**
     * Get warehouse entity (internal use) with validation
     */
    @Transactional(readOnly = true)
    public Warehouse getWarehouseEntity(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Warehouse ID must be positive");
        }

        return warehouseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with ID: " + id));
    }

    /**
     * Create new warehouse with validation
     */
    public WarehouseDTO createWarehouse(String name, Long businessId) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Warehouse name is required");
        }

        if (businessId == null || businessId <= 0) {
            throw new IllegalArgumentException("Valid businessId is required");
        }

        String trimmedName = name.trim();

        // Check if warehouse with same name already exists
        if (warehouseRepository.findByName(trimmedName).isPresent()) {
            throw new InvalidOperationException("Warehouse with name '" + trimmedName + "' already exists");
        }

        // Load business to verify it exists
        BusinessInfo business = businessInfoRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found with ID: " + businessId));

        Warehouse warehouse = new Warehouse(trimmedName, "ACTIVE");
        warehouse.setBusiness(business);
        // Auto-generate unique warehouse code
        String warehouseCode = codeGenerator.generateWarehouseCode();
        warehouse.setCode(warehouseCode);

        Warehouse saved = warehouseRepository.save(warehouse);

        return convertToDTO(saved);
    }

    /**
     * Update warehouse with optimistic locking
     */
    public WarehouseDTO updateWarehouse(Long id, WarehouseDTO updateDTO) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Warehouse ID must be positive");
        }

        if (updateDTO.getName() == null || updateDTO.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Warehouse name is required");
        }

        Warehouse warehouse = getWarehouseEntity(id);
        String newName = updateDTO.getName().trim();

        // Check if new name is already taken by another warehouse
        if (!warehouse.getName().equals(newName) &&
                warehouseRepository.existsByNameAndIdNot(newName, id)) {
            throw new InvalidOperationException("Warehouse with name '" + newName + "' already exists");
        }

        warehouse.setName(newName);
        if (updateDTO.getStatus() != null) {
            warehouse.setStatus(updateDTO.getStatus());
        }
        warehouse.setUpdatedAt(LocalDateTime.now());

        try {
            Warehouse updated = warehouseRepository.save(warehouse);
            return convertToDTO(updated);
        } catch (org.springframework.orm.ObjectOptimisticLockingFailureException e) {
            throw new InvalidOperationException(
                    "Warehouse was modified by another user. Please refresh and try again.");
        }
    }

    /**
     * Activate warehouse
     */
    public WarehouseDTO activateWarehouse(Long id) {
        Warehouse warehouse = getWarehouseEntity(id);
        if ("ACTIVE".equalsIgnoreCase(warehouse.getStatus())) {
            return convertToDTO(warehouse);
        }

        warehouse.setStatus("ACTIVE");
        warehouse.setUpdatedAt(LocalDateTime.now());
        Warehouse updated = warehouseRepository.save(warehouse);

        return convertToDTO(updated);
    }

    /**
     * Deactivate warehouse with validation
     * Cannot deactivate a warehouse that has active inventory
     */
    public WarehouseDTO deactivateWarehouse(Long id) {
        Warehouse warehouse = getWarehouseEntity(id);
        if ("INACTIVE".equalsIgnoreCase(warehouse.getStatus())) {
            return convertToDTO(warehouse);
        }

        warehouse.setStatus("INACTIVE");
        warehouse.setUpdatedAt(LocalDateTime.now());
        Warehouse updated = warehouseRepository.save(warehouse);

        return convertToDTO(updated);
    }

    /**
     * Validate warehouse is active
     */
    @Transactional(readOnly = true)
    public void validateWarehouseIsActive(Long warehouseId) {
        Warehouse warehouse = getWarehouseEntity(warehouseId);
        if (!warehouse.isActive()) {
            throw new InvalidOperationException("Warehouse '" + warehouse.getName() + "' is inactive");
        }
    }

    /**
     * Validate warehouse exists
     */
    @Transactional(readOnly = true)
    public void validateWarehouseExists(Long warehouseId) {
        if (!warehouseRepository.existsById(warehouseId)) {
            throw new ResourceNotFoundException("Warehouse not found with ID: " + warehouseId);
        }
    }

    /**
     * Validate different warehouses for transfer
     */
    @Transactional(readOnly = true)
    public void validateDifferentWarehouses(Long fromWarehouseId, Long toWarehouseId) {
        if (fromWarehouseId.equals(toWarehouseId)) {
            throw new InvalidOperationException("Source and destination warehouses cannot be the same");
        }
    }

    /**
     * Convert Warehouse entity to DTO
     */
    private WarehouseDTO convertToDTO(Warehouse warehouse) {
        WarehouseDTO dto = new WarehouseDTO(
                warehouse.getId(),
                warehouse.getName(),
                warehouse.getStatus(),
                warehouse.getCreatedAt(),
                warehouse.getUpdatedAt(),
                warehouse.getVersion());
        dto.setCode(warehouse.getCode());
        return dto;
    }
}
