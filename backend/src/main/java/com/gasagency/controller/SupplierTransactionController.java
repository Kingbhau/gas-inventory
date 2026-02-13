
package com.gasagency.controller;

import com.gasagency.dto.request.CreateSupplierTransactionRequestDTO;
import com.gasagency.dto.response.SupplierTransactionDTO;
import com.gasagency.dto.response.PagedResponseDTO;
import com.gasagency.service.SupplierTransactionService;
import com.gasagency.util.ApiResponse;
import com.gasagency.util.ApiResponseUtil;
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
    public ResponseEntity<ApiResponse<SupplierTransactionDTO>> recordTransaction(
            @Valid @RequestBody CreateSupplierTransactionRequestDTO request) {
        SupplierTransactionDTO created = service.recordTransaction(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseUtil.success("Supplier transaction recorded successfully", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SupplierTransactionDTO>> updateTransaction(@PathVariable Long id,
            @Valid @RequestBody CreateSupplierTransactionRequestDTO request) {
        SupplierTransactionDTO updated = service.updateTransaction(id, request);
        return ResponseEntity.ok(ApiResponseUtil.success("Supplier transaction updated successfully", updated));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SupplierTransactionDTO>> getTransaction(@PathVariable Long id) {
        SupplierTransactionDTO transaction = service.getTransactionById(id);
        return ResponseEntity.ok(ApiResponseUtil.success("Supplier transaction retrieved successfully", transaction));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponseDTO<SupplierTransactionDTO>>> getAllTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "ASC") String direction,
            @RequestParam(required = false) String referenceNumber,
            @RequestParam(required = false) String createdBy) {
        Sort.Direction sortDirection = Sort.Direction.fromString(direction.toUpperCase());
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        Page<SupplierTransactionDTO> transactions = service.getAllTransactions(pageable, referenceNumber, createdBy);
        return ResponseEntity.ok(ApiResponseUtil.success("Supplier transactions retrieved successfully", transactions));
    }

    @GetMapping("/supplier/{supplierId}")
    public ResponseEntity<ApiResponse<List<SupplierTransactionDTO>>> getTransactionsBySupplier(
            @PathVariable Long supplierId) {
        List<SupplierTransactionDTO> transactions = service.getTransactionsBySupplier(supplierId);
        return ResponseEntity.ok(ApiResponseUtil.success("Supplier transactions retrieved successfully", transactions));
    }

    @GetMapping("/warehouse/{warehouseId}")
    public ResponseEntity<ApiResponse<List<SupplierTransactionDTO>>> getTransactionsByWarehouse(
            @PathVariable Long warehouseId) {
        List<SupplierTransactionDTO> transactions = service.getTransactionsByWarehouse(warehouseId);
        return ResponseEntity.ok(ApiResponseUtil.success("Supplier transactions retrieved successfully", transactions));
    }
}

