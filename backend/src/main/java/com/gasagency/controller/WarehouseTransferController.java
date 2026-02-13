package com.gasagency.controller;

import com.gasagency.dto.response.WarehouseTransferDTO;
import com.gasagency.dto.response.PagedResponseDTO;
import com.gasagency.service.WarehouseTransferService;
import com.gasagency.exception.ResourceNotFoundException;
import com.gasagency.exception.InvalidOperationException;
import com.gasagency.util.ApiResponse;
import com.gasagency.util.ApiResponseUtil;
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
    public ResponseEntity<ApiResponse<WarehouseTransferDTO>> transferCylinders(
            @Valid @RequestBody WarehouseTransferDTO transferDTO) {
        try {
            WarehouseTransferDTO savedTransfer = warehouseTransferService.transferCylinders(transferDTO);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponseUtil.success("Warehouse transfer completed successfully", savedTransfer));
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
    public ResponseEntity<ApiResponse<List<WarehouseTransferDTO>>> getAllTransfers() {
        try {
            List<WarehouseTransferDTO> transfers = warehouseTransferService.getAllTransfers();
            return ResponseEntity.ok(ApiResponseUtil.success("Transfers fetched successfully", transfers));
        } catch (Exception e) {
            return buildErrorResponse("Error fetching transfers", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * GET /api/warehouse-transfers/paged - Paginated transfers
     */
    @GetMapping("/paged")
    public ResponseEntity<ApiResponse<PagedResponseDTO<WarehouseTransferDTO>>> getAllTransfersPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "transferDate") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction) {
        Sort.Direction sortDirection = Sort.Direction.fromString(direction.toUpperCase());
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy).and(Sort.by("id").descending()));
        Page<WarehouseTransferDTO> transfers = warehouseTransferService.getAllTransfers(pageable);
        return ResponseEntity.ok(ApiResponseUtil.success("Transfers fetched successfully", transfers));
    }

    /**
     * GET /api/warehouse-transfers/{id} - Get transfer by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<WarehouseTransferDTO>> getTransferById(@PathVariable Long id) {
        try {
            WarehouseTransferDTO transfer = warehouseTransferService.getTransferById(id);
            return ResponseEntity.ok(ApiResponseUtil.success("Transfer fetched successfully", transfer));
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
    public ResponseEntity<ApiResponse<List<WarehouseTransferDTO>>> getTransfersForWarehouse(
            @PathVariable Long warehouseId) {
        try {
            List<WarehouseTransferDTO> transfers = warehouseTransferService.getTransfersForWarehouse(warehouseId);
            return ResponseEntity.ok(ApiResponseUtil.success("Warehouse transfers fetched successfully", transfers));
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
    public ResponseEntity<ApiResponse<PagedResponseDTO<WarehouseTransferDTO>>> getTransfersForWarehousePaged(
            @PathVariable Long warehouseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "transferDate") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction) {
        Sort.Direction sortDirection = Sort.Direction.fromString(direction.toUpperCase());
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy).and(Sort.by("id").descending()));
        Page<WarehouseTransferDTO> transfers = warehouseTransferService.getTransfersForWarehouse(warehouseId, pageable);
        return ResponseEntity.ok(ApiResponseUtil.success("Warehouse transfers fetched successfully", transfers));
    }

    /**
     * GET /api/warehouse-transfers/from/{warehouseId} - Get outgoing transfers
     */
    @GetMapping("/from/{warehouseId}")
    public ResponseEntity<ApiResponse<List<WarehouseTransferDTO>>> getTransfersFrom(@PathVariable Long warehouseId) {
        try {
            List<WarehouseTransferDTO> transfers = warehouseTransferService.getTransfersFrom(warehouseId);
            return ResponseEntity.ok(ApiResponseUtil.success("Outgoing transfers fetched successfully", transfers));
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
    public ResponseEntity<ApiResponse<PagedResponseDTO<WarehouseTransferDTO>>> getTransfersFromPaged(
            @PathVariable Long warehouseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "transferDate") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction) {
        Sort.Direction sortDirection = Sort.Direction.fromString(direction.toUpperCase());
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy).and(Sort.by("id").descending()));
        Page<WarehouseTransferDTO> transfers = warehouseTransferService.getTransfersFrom(warehouseId, pageable);
        return ResponseEntity.ok(ApiResponseUtil.success("Outgoing transfers fetched successfully", transfers));
    }

    /**
     * GET /api/warehouse-transfers/to/{warehouseId} - Get incoming transfers
     */
    @GetMapping("/to/{warehouseId}")
    public ResponseEntity<ApiResponse<List<WarehouseTransferDTO>>> getTransfersTo(@PathVariable Long warehouseId) {
        try {
            List<WarehouseTransferDTO> transfers = warehouseTransferService.getTransfersTo(warehouseId);
            return ResponseEntity.ok(ApiResponseUtil.success("Incoming transfers fetched successfully", transfers));
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
    public ResponseEntity<ApiResponse<PagedResponseDTO<WarehouseTransferDTO>>> getTransfersToPaged(
            @PathVariable Long warehouseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "transferDate") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction) {
        Sort.Direction sortDirection = Sort.Direction.fromString(direction.toUpperCase());
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy).and(Sort.by("id").descending()));
        Page<WarehouseTransferDTO> transfers = warehouseTransferService.getTransfersTo(warehouseId, pageable);
        return ResponseEntity.ok(ApiResponseUtil.success("Incoming transfers fetched successfully", transfers));
    }

    /**
     * GET /api/warehouse-transfers/between/{fromId}/{toId} - Get transfers between
     * two warehouses
     */
    @GetMapping("/between/{fromId}/{toId}")
    public ResponseEntity<ApiResponse<List<WarehouseTransferDTO>>> getTransfersBetweenWarehouses(
            @PathVariable Long fromId,
            @PathVariable Long toId) {
        try {
            List<WarehouseTransferDTO> transfers = warehouseTransferService.getTransfersBetweenWarehouses(fromId, toId);
            return ResponseEntity.ok(ApiResponseUtil.success("Transfers fetched successfully", transfers));
        } catch (ResourceNotFoundException e) {
            return buildErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return buildErrorResponse("Error fetching transfers", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Build error response helper
     */
    private <T> ResponseEntity<ApiResponse<T>> buildErrorResponse(String message, HttpStatus status) {
        @SuppressWarnings("unchecked")
        ApiResponse<T> response = (ApiResponse<T>) ApiResponseUtil.error(message, "WAREHOUSE_TRANSFER_FAILED");
        return ResponseEntity.status(status).body(response);
    }
}

