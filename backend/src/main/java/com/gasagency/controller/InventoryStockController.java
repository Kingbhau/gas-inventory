package com.gasagency.controller;

import com.gasagency.dto.InventoryStockDTO;
import com.gasagency.service.InventoryStockService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory")
public class InventoryStockController {
    private final InventoryStockService service;

    public InventoryStockController(InventoryStockService service) {
        this.service = service;
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

    @GetMapping("/variant/{variantId}")
    public ResponseEntity<InventoryStockDTO> getStockByVariant(@PathVariable Long variantId) {
        return ResponseEntity.ok(service.getStockByVariant(variantId));
    }
}
