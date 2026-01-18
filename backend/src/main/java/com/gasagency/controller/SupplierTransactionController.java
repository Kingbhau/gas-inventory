
package com.gasagency.controller;

import com.gasagency.dto.CreateSupplierTransactionRequestDTO;
import com.gasagency.dto.SupplierTransactionDTO;
import com.gasagency.service.SupplierTransactionService;
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
@RequestMapping("/api/supplier-transactions")
public class SupplierTransactionController {
    private final SupplierTransactionService service;

    public SupplierTransactionController(SupplierTransactionService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<SupplierTransactionDTO> recordTransaction(
            @Valid @RequestBody CreateSupplierTransactionRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.recordTransaction(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SupplierTransactionDTO> updateTransaction(@PathVariable Long id,
            @Valid @RequestBody CreateSupplierTransactionRequestDTO request) {
        return ResponseEntity.ok(service.updateTransaction(id, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SupplierTransactionDTO> getTransaction(@PathVariable Long id) {
        return ResponseEntity.ok(service.getTransactionById(id));
    }

    @GetMapping
    public ResponseEntity<Page<SupplierTransactionDTO>> getAllTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "ASC") String direction,
            @RequestParam(required = false) String referenceNumber) {
        Sort.Direction sortDirection = Sort.Direction.fromString(direction.toUpperCase());
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        return ResponseEntity.ok(service.getAllTransactions(pageable, referenceNumber));
    }

    @GetMapping("/supplier/{supplierId}")
    public ResponseEntity<List<SupplierTransactionDTO>> getTransactionsBySupplier(@PathVariable Long supplierId) {
        return ResponseEntity.ok(service.getTransactionsBySupplier(supplierId));
    }

    @GetMapping("/warehouse/{warehouseId}")
    public ResponseEntity<List<SupplierTransactionDTO>> getTransactionsByWarehouse(@PathVariable Long warehouseId) {
        return ResponseEntity.ok(service.getTransactionsByWarehouse(warehouseId));
    }
}
