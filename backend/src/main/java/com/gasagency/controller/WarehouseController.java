package com.gasagency.controller;

import com.gasagency.dto.WarehouseDTO;
import com.gasagency.service.WarehouseService;
import com.gasagency.exception.ResourceNotFoundException;
import com.gasagency.exception.InvalidOperationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/warehouses")
public class WarehouseController {

    @Autowired
    private WarehouseService warehouseService;

    /**
     * GET /api/warehouses - Get all warehouses
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllWarehouses() {
        try {
            List<WarehouseDTO> warehouses = warehouseService.getAllWarehouses();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", warehouses);
            response.put("message", "Warehouses fetched successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return buildErrorResponse("Error fetching warehouses", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * GET /api/warehouses/active - Get only active warehouses
     */
    @GetMapping("/active")
    public ResponseEntity<Map<String, Object>> getActiveWarehouses() {
        try {
            List<WarehouseDTO> warehouses = warehouseService.getActiveWarehouses();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", warehouses);
            response.put("message", "Active warehouses fetched successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return buildErrorResponse("Error fetching active warehouses", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * GET /api/warehouses/{id} - Get warehouse by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getWarehouseById(@PathVariable Long id) {
        try {
            WarehouseDTO warehouse = warehouseService.getWarehouseById(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", warehouse);
            response.put("message", "Warehouse fetched successfully");
            return ResponseEntity.ok(response);
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
    public ResponseEntity<Map<String, Object>> getWarehouseByName(@PathVariable String name) {

        try {
            WarehouseDTO warehouse = warehouseService.getWarehouseByName(name);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", warehouse);
            response.put("message", "Warehouse fetched successfully");
            return ResponseEntity.ok(response);
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
    public ResponseEntity<Map<String, Object>> createWarehouse(@Valid @RequestBody Map<String, String> request) {

        try {
            String name = request.get("name");
            if (name == null || name.trim().isEmpty()) {
                return buildErrorResponse("Warehouse name is required", HttpStatus.BAD_REQUEST);
            }

            if (!request.containsKey("businessId")) {
                return buildErrorResponse("businessId is required", HttpStatus.BAD_REQUEST);
            }

            Long businessId = Long.parseLong(request.get("businessId"));
            WarehouseDTO warehouse = warehouseService.createWarehouse(name, businessId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", warehouse);
            response.put("message", "Warehouse created successfully");
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
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
    public ResponseEntity<Map<String, Object>> updateWarehouse(@PathVariable Long id,
            @Valid @RequestBody WarehouseDTO updateDTO) {

        try {
            WarehouseDTO warehouse = warehouseService.updateWarehouse(id, updateDTO);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", warehouse);
            response.put("message", "Warehouse updated successfully");
            return ResponseEntity.ok(response);
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
    public ResponseEntity<Map<String, Object>> activateWarehouse(@PathVariable Long id) {

        try {
            WarehouseDTO warehouse = warehouseService.activateWarehouse(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", warehouse);
            response.put("message", "Warehouse activated successfully");
            return ResponseEntity.ok(response);
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
    public ResponseEntity<Map<String, Object>> deactivateWarehouse(@PathVariable Long id) {

        try {
            WarehouseDTO warehouse = warehouseService.deactivateWarehouse(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", warehouse);
            response.put("message", "Warehouse deactivated successfully");
            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException e) {

            return buildErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {

            return buildErrorResponse("Error deactivating warehouse", HttpStatus.INTERNAL_SERVER_ERROR);
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
