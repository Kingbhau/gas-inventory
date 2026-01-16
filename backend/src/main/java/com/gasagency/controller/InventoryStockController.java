package com.gasagency.controller;

import com.gasagency.dto.InventoryStockDTO;
import com.gasagency.dto.WarehouseTransferDTO;
import com.gasagency.entity.Warehouse;
import com.gasagency.repository.WarehouseRepository;
import com.gasagency.service.InventoryStockService;
import com.gasagency.service.WarehouseTransferService;
import com.gasagency.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
public class InventoryStockController {
    private final InventoryStockService service;
    private final WarehouseTransferService warehouseTransferService;
    private final WarehouseRepository warehouseRepository;

    public InventoryStockController(InventoryStockService service, WarehouseTransferService warehouseTransferService,
            WarehouseRepository warehouseRepository) {
        this.service = service;
        this.warehouseTransferService = warehouseTransferService;
        this.warehouseRepository = warehouseRepository;
    }

    @GetMapping("/{id}")
    public ResponseEntity<InventoryStockDTO> getStock(@PathVariable Long id) {
        return ResponseEntity.ok(service.getStockById(id));
    }

    @GetMapping
    public ResponseEntity<Page<InventoryStockDTO>> getAllStock(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "ASC") String direction) {
        Sort.Direction sortDirection = Sort.Direction.fromString(direction.toUpperCase());
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        return ResponseEntity.ok(service.getAllStock(pageable));
    }

    @PostMapping("/setup")
    public ResponseEntity<Map<String, String>> setupWarehouseInventory(@RequestBody Map<String, Object> payload) {
        Long warehouseId = Long.valueOf(payload.get("warehouseId").toString());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> inventoryItems = (List<Map<String, Object>>) payload.get("inventoryItems");

        service.setupWarehouseInventory(warehouseId, inventoryItems);

        return ResponseEntity.ok(Map.of("message", "Warehouse inventory setup completed successfully"));
    }

    @PostMapping("/transfer")
    public ResponseEntity<WarehouseTransferDTO> transferStock(@RequestBody WarehouseTransferDTO transferRequest) {
        return ResponseEntity.ok(warehouseTransferService.transferCylinders(transferRequest));
    }

    @GetMapping("/variant/{variantId}")
    public ResponseEntity<InventoryStockDTO> getStockByVariant(@PathVariable Long variantId) {
        return ResponseEntity.ok(service.getStockByVariant(variantId));
    }

    @GetMapping("/warehouse/{warehouseId}")
    public ResponseEntity<List<InventoryStockDTO>> getStockByWarehouse(@PathVariable Long warehouseId) {
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with id: " + warehouseId));
        return ResponseEntity.ok(service.getStockDTOsByWarehouse(warehouse));
    }
}
