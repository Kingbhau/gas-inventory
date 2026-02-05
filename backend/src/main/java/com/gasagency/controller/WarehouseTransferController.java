package com.gasagency.controller;

import com.gasagency.dto.WarehouseTransferDTO;
import com.gasagency.service.WarehouseTransferService;
import com.gasagency.exception.ResourceNotFoundException;
import com.gasagency.exception.InvalidOperationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/warehouse-transfers")
public class WarehouseTransferController {

    @Autowired
    private WarehouseTransferService warehouseTransferService;

    /**
     * POST /api/warehouse-transfers - Create new transfer (atomic operation)
     * Validates all preconditions and handles concurrency
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> transferCylinders(@Valid @RequestBody WarehouseTransferDTO transferDTO) {
        try {
            WarehouseTransferDTO savedTransfer = warehouseTransferService.transferCylinders(transferDTO);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", savedTransfer);
            response.put("message", "Warehouse transfer completed successfully");
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (ResourceNotFoundException e) {
            return buildErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (InvalidOperationException e) {
            return buildErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (IllegalArgumentException e) {
            return buildErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return buildErrorResponse("Error creating warehouse transfer: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * GET /api/warehouse-transfers - Get all transfers (audit trail)
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllTransfers() {
        try {
            List<WarehouseTransferDTO> transfers = warehouseTransferService.getAllTransfers();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", transfers);
            response.put("message", "Transfers fetched successfully");
            response.put("count", transfers.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return buildErrorResponse("Error fetching transfers", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * GET /api/warehouse-transfers/paged - Paginated transfers
     */
    @GetMapping("/paged")
    public ResponseEntity<Page<WarehouseTransferDTO>> getAllTransfersPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "transferDate") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction) {
        Sort.Direction sortDirection = Sort.Direction.fromString(direction.toUpperCase());
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy).and(Sort.by("id").descending()));
        return ResponseEntity.ok(warehouseTransferService.getAllTransfers(pageable));
    }

    /**
     * GET /api/warehouse-transfers/{id} - Get transfer by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getTransferById(@PathVariable Long id) {
        try {
            WarehouseTransferDTO transfer = warehouseTransferService.getTransferById(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", transfer);
            response.put("message", "Transfer fetched successfully");
            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException e) {
            return buildErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (IllegalArgumentException e) {
            return buildErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return buildErrorResponse("Error fetching transfer", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * GET /api/warehouse-transfers/warehouse/{warehouseId} - Get transfers for a
     * warehouse
     * Returns both incoming and outgoing transfers
     */
    @GetMapping("/warehouse/{warehouseId}")
    public ResponseEntity<Map<String, Object>> getTransfersForWarehouse(@PathVariable Long warehouseId) {
        try {
            List<WarehouseTransferDTO> transfers = warehouseTransferService.getTransfersForWarehouse(warehouseId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", transfers);
            response.put("message", "Warehouse transfers fetched successfully");
            response.put("count", transfers.size());
            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException e) {
            return buildErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return buildErrorResponse("Error fetching warehouse transfers", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * GET /api/warehouse-transfers/warehouse/{warehouseId}/paged - Paginated transfers for a warehouse
     */
    @GetMapping("/warehouse/{warehouseId}/paged")
    public ResponseEntity<Page<WarehouseTransferDTO>> getTransfersForWarehousePaged(
            @PathVariable Long warehouseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "transferDate") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction) {
        Sort.Direction sortDirection = Sort.Direction.fromString(direction.toUpperCase());
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy).and(Sort.by("id").descending()));
        return ResponseEntity.ok(warehouseTransferService.getTransfersForWarehouse(warehouseId, pageable));
    }

    /**
     * GET /api/warehouse-transfers/from/{warehouseId} - Get outgoing transfers
     */
    @GetMapping("/from/{warehouseId}")
    public ResponseEntity<Map<String, Object>> getTransfersFrom(@PathVariable Long warehouseId) {
        try {
            List<WarehouseTransferDTO> transfers = warehouseTransferService.getTransfersFrom(warehouseId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", transfers);
            response.put("message", "Outgoing transfers fetched successfully");
            response.put("count", transfers.size());
            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException e) {
            return buildErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {

            return buildErrorResponse("Error fetching outgoing transfers", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * GET /api/warehouse-transfers/from/{warehouseId}/paged - Paginated outgoing transfers
     */
    @GetMapping("/from/{warehouseId}/paged")
    public ResponseEntity<Page<WarehouseTransferDTO>> getTransfersFromPaged(
            @PathVariable Long warehouseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "transferDate") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction) {
        Sort.Direction sortDirection = Sort.Direction.fromString(direction.toUpperCase());
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy).and(Sort.by("id").descending()));
        return ResponseEntity.ok(warehouseTransferService.getTransfersFrom(warehouseId, pageable));
    }

    /**
     * GET /api/warehouse-transfers/to/{warehouseId} - Get incoming transfers
     */
    @GetMapping("/to/{warehouseId}")
    public ResponseEntity<Map<String, Object>> getTransfersTo(@PathVariable Long warehouseId) {
        try {
            List<WarehouseTransferDTO> transfers = warehouseTransferService.getTransfersTo(warehouseId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", transfers);
            response.put("message", "Incoming transfers fetched successfully");
            response.put("count", transfers.size());
            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException e) {
            return buildErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return buildErrorResponse("Error fetching incoming transfers", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * GET /api/warehouse-transfers/to/{warehouseId}/paged - Paginated incoming transfers
     */
    @GetMapping("/to/{warehouseId}/paged")
    public ResponseEntity<Page<WarehouseTransferDTO>> getTransfersToPaged(
            @PathVariable Long warehouseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "transferDate") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction) {
        Sort.Direction sortDirection = Sort.Direction.fromString(direction.toUpperCase());
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy).and(Sort.by("id").descending()));
        return ResponseEntity.ok(warehouseTransferService.getTransfersTo(warehouseId, pageable));
    }

    /**
     * GET /api/warehouse-transfers/between/{fromId}/{toId} - Get transfers between
     * two warehouses
     */
    @GetMapping("/between/{fromId}/{toId}")
    public ResponseEntity<Map<String, Object>> getTransfersBetweenWarehouses(@PathVariable Long fromId,
            @PathVariable Long toId) {
        try {
            List<WarehouseTransferDTO> transfers = warehouseTransferService.getTransfersBetweenWarehouses(fromId, toId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", transfers);
            response.put("message", "Transfers fetched successfully");
            response.put("count", transfers.size());
            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException e) {
            return buildErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return buildErrorResponse("Error fetching transfers", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Build error response helper
     */
    private ResponseEntity<Map<String, Object>> buildErrorResponse(String message, HttpStatus status) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return ResponseEntity.status(status).body(response);
    }
}
