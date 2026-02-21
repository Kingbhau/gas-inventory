
package com.gasagency.controller;

import com.gasagency.dto.request.CreateSaleRequestDTO;
import com.gasagency.dto.response.SaleDTO;
import com.gasagency.dto.response.SaleSummaryDTO;
import com.gasagency.dto.response.PaymentModeSummaryDTO;
import com.gasagency.dto.response.PagedResponseDTO;
import com.gasagency.service.SaleService;
import com.gasagency.util.ApiResponse;
import com.gasagency.util.ApiResponseUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
    public ResponseEntity<ApiResponse<List<SaleDTO>>> getRecentSales() {
        List<SaleDTO> sales = service.getRecentSales();
        return ResponseEntity.ok(ApiResponseUtil.success("Recent sales retrieved successfully", sales));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<SaleSummaryDTO>> getSalesSummary(
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long variantId,
            @RequestParam(required = false) Double minAmount,
            @RequestParam(required = false) Double maxAmount,
            @RequestParam(required = false) String referenceNumber,
            @RequestParam(required = false) String createdBy,
            Authentication authentication) {
        String effectiveCreatedBy = resolveCreatedBy(authentication, createdBy);
        SaleSummaryDTO summary = service.getSalesSummary(fromDate, toDate, customerId, variantId,
                minAmount, maxAmount, referenceNumber, effectiveCreatedBy);
        return ResponseEntity.ok(ApiResponseUtil.success("Sales summary retrieved successfully", summary));
    }

    @GetMapping("/payment-mode-summary")
    public ResponseEntity<ApiResponse<PaymentModeSummaryDTO>> getPaymentModeSummary(
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String paymentMode,
            @RequestParam(required = false) Long variantId,
            @RequestParam(required = false) Long bankAccountId,
            @RequestParam(required = false) Double minAmount,
            @RequestParam(required = false) Double maxAmount,
            @RequestParam(required = false) Integer minTransactionCount) {
        PaymentModeSummaryDTO summary = service.getPaymentModeSummary(fromDate, toDate, customerId,
                paymentMode, variantId, bankAccountId, minAmount, maxAmount, minTransactionCount);
        return ResponseEntity.ok(ApiResponseUtil.success("Payment mode summary retrieved successfully", summary));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SaleDTO>> createSale(@Valid @RequestBody CreateSaleRequestDTO request) {
        SaleDTO created = service.createSale(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseUtil.success("Sale created successfully", created));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SaleDTO>> getSale(@PathVariable Long id) {
        SaleDTO sale = service.getSaleById(id);
        return ResponseEntity.ok(ApiResponseUtil.success("Sale retrieved successfully", sale));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponseDTO<SaleDTO>>> getAllSales(
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
            @RequestParam(required = false) String referenceNumber,
            @RequestParam(required = false) String createdBy,
            Authentication authentication) {
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(direction, sortBy).and(Sort.by(Sort.Direction.DESC, "id"))
        );
        String effectiveCreatedBy = resolveCreatedBy(authentication, createdBy);
        Page<SaleDTO> sales = service.getAllSales(pageable, fromDate, toDate, customerId, variantId, minAmount,
                maxAmount, referenceNumber, effectiveCreatedBy);
        return ResponseEntity.ok(ApiResponseUtil.success("Sales retrieved successfully", sales));
    }

    private String resolveCreatedBy(Authentication authentication, String requestedCreatedBy) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return requestedCreatedBy;
        }
        boolean isStaff = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_STAFF".equals(authority.getAuthority()));
        return isStaff ? authentication.getName() : requestedCreatedBy;
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<ApiResponse<PagedResponseDTO<SaleDTO>>> getSalesByCustomer(
            @PathVariable Long customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("saleDate").descending());
        Page<SaleDTO> sales = service.getSalesByCustomer(customerId, pageable);
        return ResponseEntity.ok(ApiResponseUtil.success("Sales retrieved successfully", sales));
    }
}

