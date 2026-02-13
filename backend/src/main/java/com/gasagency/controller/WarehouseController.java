package com.gasagency.controller;

import com.gasagency.dto.request.CreateWarehouseRequestDTO;
import com.gasagency.dto.response.WarehouseDTO;
import com.gasagency.service.WarehouseService;
import com.gasagency.exception.ResourceNotFoundException;
import com.gasagency.exception.InvalidOperationException;
import com.gasagency.util.ApiResponse;
import com.gasagency.util.ApiResponseUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/warehouses")
public class WarehouseController {

    @Autowired
    private WarehouseService warehouseService;

    /**
     * GET /api/warehouses - Get all warehouses
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<WarehouseDTO>>> getAllWarehouses() {
        try {
            List<WarehouseDTO> warehouses = warehouseService.getAllWarehouses();
            return ResponseEntity.ok(ApiResponseUtil.success("Warehouses fetched successfully", warehouses));
        } catch (Exception e) {
            return buildErrorResponse("Error fetching warehouses", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * GET /api/warehouses/active - Get only active warehouses
     */
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<WarehouseDTO>>> getActiveWarehouses() {
        try {
            List<WarehouseDTO> warehouses = warehouseService.getActiveWarehouses();
            return ResponseEntity.ok(ApiResponseUtil.success("Active warehouses fetched successfully", warehouses));
        } catch (Exception e) {
            return buildErrorResponse("Error fetching active warehouses", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * GET /api/warehouses/{id} - Get warehouse by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<WarehouseDTO>> getWarehouseById(@PathVariable Long id) {
        try {
            WarehouseDTO warehouse = warehouseService.getWarehouseById(id);
            return ResponseEntity.ok(ApiResponseUtil.success("Warehouse fetched successfully", warehouse));
        } catch (ResourceNotFoundException e) {
            return buildErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (IllegalArgumentException e) {
            return buildErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return buildErrorResponse("Error fetching warehouse", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * GET /api/warehouses/name/{name} - Get warehouse by name
     */
    @GetMapping("/name/{name}")
    public ResponseEntity<ApiResponse<WarehouseDTO>> getWarehouseByName(@PathVariable String name) {

        try {
            WarehouseDTO warehouse = warehouseService.getWarehouseByName(name);
            return ResponseEntity.ok(ApiResponseUtil.success("Warehouse fetched successfully", warehouse));
        } catch (ResourceNotFoundException e) {

            return buildErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (IllegalArgumentException e) {

            return buildErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {

            return buildErrorResponse("Error fetching warehouse", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * POST /api/warehouses - Create new warehouse
     */
    @PostMapping
    public ResponseEntity<ApiResponse<WarehouseDTO>> createWarehouse(
            @Valid @RequestBody CreateWarehouseRequestDTO request) {

        try {
            String name = request.getName();
            if (name == null || name.trim().isEmpty()) {
                return buildErrorResponse("Warehouse name is required", HttpStatus.BAD_REQUEST);
            }

            if (request.getBusinessId() == null) {
                return buildErrorResponse("businessId is required", HttpStatus.BAD_REQUEST);
            }

            Long businessId = request.getBusinessId();
            WarehouseDTO warehouse = warehouseService.createWarehouse(name, businessId);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponseUtil.success("Warehouse created successfully", warehouse));
        } catch (InvalidOperationException e) {

            return buildErrorResponse(e.getMessage(), HttpStatus.CONFLICT);
        } catch (IllegalArgumentException e) {

            return buildErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {

            return buildErrorResponse("Error creating warehouse", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * PUT /api/warehouses/{id} - Update warehouse
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<WarehouseDTO>> updateWarehouse(@PathVariable Long id,
            @Valid @RequestBody WarehouseDTO updateDTO) {

        try {
            WarehouseDTO warehouse = warehouseService.updateWarehouse(id, updateDTO);
            return ResponseEntity.ok(ApiResponseUtil.success("Warehouse updated successfully", warehouse));
        } catch (ResourceNotFoundException e) {

            return buildErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (InvalidOperationException e) {

            return buildErrorResponse(e.getMessage(), HttpStatus.CONFLICT);
        } catch (IllegalArgumentException e) {

            return buildErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {

            return buildErrorResponse("Error updating warehouse", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * PUT /api/warehouses/{id}/activate - Activate warehouse
     */
    @PutMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<WarehouseDTO>> activateWarehouse(@PathVariable Long id) {

        try {
            WarehouseDTO warehouse = warehouseService.activateWarehouse(id);
            return ResponseEntity.ok(ApiResponseUtil.success("Warehouse activated successfully", warehouse));
        } catch (ResourceNotFoundException e) {

            return buildErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {

            return buildErrorResponse("Error activating warehouse", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * PUT /api/warehouses/{id}/deactivate - Deactivate warehouse
     */
    @PutMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<WarehouseDTO>> deactivateWarehouse(@PathVariable Long id) {

        try {
            WarehouseDTO warehouse = warehouseService.deactivateWarehouse(id);
            return ResponseEntity.ok(ApiResponseUtil.success("Warehouse deactivated successfully", warehouse));
        } catch (ResourceNotFoundException e) {

            return buildErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {

            return buildErrorResponse("Error deactivating warehouse", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Build error response helper
     */
    private <T> ResponseEntity<ApiResponse<T>> buildErrorResponse(String message, HttpStatus status) {
        @SuppressWarnings("unchecked")
        ApiResponse<T> response = (ApiResponse<T>) ApiResponseUtil.error(message, "WAREHOUSE_OPERATION_FAILED");
        return ResponseEntity.status(status).body(response);
    }
}

