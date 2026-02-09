package com.gasagency.controller;

import com.gasagency.dto.CustomerVariantPriceDTO;
import com.gasagency.service.CustomerVariantPriceService;
import com.gasagency.util.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/customers/{customerId}/variant-prices")
public class CustomerVariantPriceController {
    private final CustomerVariantPriceService service;

    public CustomerVariantPriceController(CustomerVariantPriceService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CustomerVariantPriceDTO>> createPrice(
            @PathVariable Long customerId,
            @Valid @RequestBody CustomerVariantPriceDTO dto) {
        dto.setCustomerId(customerId);
        CustomerVariantPriceDTO created = service.createPrice(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Pricing created successfully", created));
    }

    @GetMapping("/{variantId}")
    public ResponseEntity<ApiResponse<CustomerVariantPriceDTO>> getPriceByVariant(
            @PathVariable Long customerId,
            @PathVariable Long variantId) {
        CustomerVariantPriceDTO price = service.getPriceByCustomerAndVariant(customerId, variantId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Pricing retrieved successfully", price));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CustomerVariantPriceDTO>>> getPricesByCustomer(
            @PathVariable Long customerId) {
        List<CustomerVariantPriceDTO> prices = service.getPricesByCustomer(customerId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Pricing list retrieved successfully", prices));
    }

    @PutMapping("/{variantId}")
    public ResponseEntity<ApiResponse<CustomerVariantPriceDTO>> updatePrice(
            @PathVariable Long customerId,
            @PathVariable Long variantId,
            @Valid @RequestBody CustomerVariantPriceDTO dto) {
        // First get the existing price record
        CustomerVariantPriceDTO existing = service.getPriceByCustomerAndVariant(customerId, variantId);

        // Update it with new values
        dto.setCustomerId(customerId);
        dto.setVariantId(variantId);
        CustomerVariantPriceDTO updated = service.updatePrice(existing.getId(), dto);

        return ResponseEntity.ok(new ApiResponse<>(true, "Pricing updated successfully", updated));
    }

    @DeleteMapping("/{variantId}")
    public ResponseEntity<ApiResponse<Void>> deletePrice(
            @PathVariable Long customerId,
            @PathVariable Long variantId) {
        service.deletePriceByCustomerAndVariant(customerId, variantId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Pricing deleted successfully", null));
    }
}
