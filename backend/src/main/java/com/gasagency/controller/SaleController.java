
package com.gasagency.controller;

import com.gasagency.dto.CreateSaleRequestDTO;
import com.gasagency.dto.SaleDTO;
import com.gasagency.service.SaleService;
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
@RequestMapping("/api/sales")
public class SaleController {
    private final SaleService service;

    public SaleController(SaleService service) {
        this.service = service;
    }

    @GetMapping("/recent")
    public ResponseEntity<List<SaleDTO>> getRecentSales() {
        return ResponseEntity.ok(service.getRecentSales());
    }

    @GetMapping("/summary")
    public ResponseEntity<com.gasagency.dto.SaleSummaryDTO> getSalesSummary(
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long variantId,
            @RequestParam(required = false) Double minAmount,
            @RequestParam(required = false) Double maxAmount,
            @RequestParam(required = false) String referenceNumber) {
        return ResponseEntity
                .ok(service.getSalesSummary(fromDate, toDate, customerId, variantId, minAmount, maxAmount,
                        referenceNumber));
    }

    @GetMapping("/payment-mode-summary")
    public ResponseEntity<com.gasagency.dto.PaymentModeSummaryDTO> getPaymentModeSummary(
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String paymentMode,
            @RequestParam(required = false) Long variantId,
            @RequestParam(required = false) Long bankAccountId,
            @RequestParam(required = false) Double minAmount,
            @RequestParam(required = false) Double maxAmount,
            @RequestParam(required = false) Integer minTransactionCount) {
        return ResponseEntity.ok(service.getPaymentModeSummary(fromDate, toDate, customerId, paymentMode,
                variantId, bankAccountId, minAmount, maxAmount, minTransactionCount));
    }

    @PostMapping
    public ResponseEntity<SaleDTO> createSale(@Valid @RequestBody CreateSaleRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createSale(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SaleDTO> getSale(@PathVariable Long id) {
        return ResponseEntity.ok(service.getSaleById(id));
    }

    @GetMapping
    public ResponseEntity<Page<SaleDTO>> getAllSales(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "saleDate") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long variantId,
            @RequestParam(required = false) Double minAmount,
            @RequestParam(required = false) Double maxAmount,
            @RequestParam(required = false) String referenceNumber) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        return ResponseEntity
                .ok(service.getAllSales(pageable, fromDate, toDate, customerId, variantId, minAmount, maxAmount,
                        referenceNumber));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<Page<SaleDTO>> getSalesByCustomer(
            @PathVariable Long customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("saleDate").descending());
        return ResponseEntity.ok(service.getSalesByCustomer(customerId, pageable));
    }
}
