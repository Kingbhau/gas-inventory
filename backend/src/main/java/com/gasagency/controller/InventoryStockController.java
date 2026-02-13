package com.gasagency.controller;

import com.gasagency.dto.response.InventoryStockDTO;
import com.gasagency.dto.response.PagedResponseDTO;
import com.gasagency.dto.response.SimpleStatusDTO;
import com.gasagency.dto.request.WarehouseInventorySetupRequestDTO;
import com.gasagency.dto.response.WarehouseTransferDTO;
import com.gasagency.service.InventoryStockService;
import com.gasagency.service.WarehouseTransferService;
import com.gasagency.util.ApiResponse;
import com.gasagency.util.ApiResponseUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/inventory")
public class InventoryStockController {
    private final InventoryStockService service;
    private final WarehouseTransferService warehouseTransferService;

    public InventoryStockController(InventoryStockService service, WarehouseTransferService warehouseTransferService) {
        this.service = service;
        this.warehouseTransferService = warehouseTransferService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InventoryStockDTO>> getStock(@PathVariable Long id) {
        InventoryStockDTO stock = service.getStockById(id);
        return ResponseEntity.ok(ApiResponseUtil.success("Inventory stock retrieved successfully", stock));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponseDTO<InventoryStockDTO>>> getAllStock(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "ASC") String direction) {
        Sort.Direction sortDirection = Sort.Direction.fromString(direction.toUpperCase());
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        Page<InventoryStockDTO> stock = service.getAllStock(pageable);
        return ResponseEntity.ok(ApiResponseUtil.success("Inventory stock list retrieved successfully", stock));
    }

    @PostMapping("/setup")
    public ResponseEntity<ApiResponse<SimpleStatusDTO>> setupWarehouseInventory(
            @RequestBody WarehouseInventorySetupRequestDTO payload) {
        service.setupWarehouseInventory(payload.getWarehouseId(), payload.getInventoryItems());

        return ResponseEntity.ok(ApiResponseUtil.success("Warehouse inventory setup completed successfully",
                new SimpleStatusDTO("SUCCESS")));
    }

    @PostMapping("/transfer")
    public ResponseEntity<ApiResponse<WarehouseTransferDTO>> transferStock(
            @RequestBody WarehouseTransferDTO transferRequest) {
        WarehouseTransferDTO transfer = warehouseTransferService.transferCylinders(transferRequest);
        return ResponseEntity.ok(ApiResponseUtil.success("Stock transfer completed successfully", transfer));
    }

    @GetMapping("/variant/{variantId}")
    public ResponseEntity<ApiResponse<InventoryStockDTO>> getStockByVariant(@PathVariable Long variantId) {
        InventoryStockDTO stock = service.getStockByVariant(variantId);
        return ResponseEntity.ok(ApiResponseUtil.success("Inventory stock retrieved successfully", stock));
    }

    @GetMapping("/warehouse/{warehouseId}")
    public ResponseEntity<ApiResponse<List<InventoryStockDTO>>> getStockByWarehouse(@PathVariable Long warehouseId) {
        List<InventoryStockDTO> stock = service.getStockDTOsByWarehouseId(warehouseId);
        return ResponseEntity.ok(ApiResponseUtil.success("Inventory stock retrieved successfully", stock));
    }
}

