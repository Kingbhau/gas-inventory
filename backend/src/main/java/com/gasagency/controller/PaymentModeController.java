package com.gasagency.controller;

import com.gasagency.dto.response.PaymentModeDTO;
import com.gasagency.dto.response.PagedResponseDTO;
import com.gasagency.dto.response.SimpleStatusDTO;
import com.gasagency.service.PaymentModeService;
import com.gasagency.util.ApiResponse;
import com.gasagency.util.ApiResponseUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/payment-modes")
@PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
public class PaymentModeController {

    private final PaymentModeService service;

    public PaymentModeController(PaymentModeService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponseDTO<PaymentModeDTO>>> getAllPaymentModes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<PaymentModeDTO> modes = service.getAllPaymentModes(pageable);
        return ResponseEntity.ok(ApiResponseUtil.success("Payment modes retrieved successfully", modes));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<PaymentModeDTO>>> getActivePaymentModes() {
        List<PaymentModeDTO> modes = service.getActivePaymentModes();
        return ResponseEntity.ok(ApiResponseUtil.success("Active payment modes retrieved successfully", modes));
    }

    @GetMapping("/names")
    public ResponseEntity<ApiResponse<List<String>>> getActivePaymentModeNames() {
        List<String> names = service.getActivePaymentModeNames();
        return ResponseEntity.ok(ApiResponseUtil.success("Payment mode names retrieved successfully", names));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PaymentModeDTO>> getPaymentModeById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(ApiResponseUtil.success("Payment mode retrieved successfully",
                    service.getPaymentModeById(id)));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponseUtil.error("Payment mode not found", "RESOURCE_NOT_FOUND"));
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PaymentModeDTO>> createPaymentMode(@RequestBody PaymentModeDTO dto) {
        try {
            PaymentModeDTO created = service.createPaymentMode(dto);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponseUtil.success("Payment mode created successfully", created));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponseUtil.error(e.getMessage(), "PAYMENT_MODE_CREATE_FAILED"));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PaymentModeDTO>> updatePaymentMode(@PathVariable Long id,
            @RequestBody PaymentModeDTO dto) {
        try {
            PaymentModeDTO updated = service.updatePaymentMode(id, dto);
            return ResponseEntity.ok(ApiResponseUtil.success("Payment mode updated successfully", updated));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponseUtil.error(e.getMessage(), "PAYMENT_MODE_UPDATE_FAILED"));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<SimpleStatusDTO>> deletePaymentMode(@PathVariable Long id) {
        try {
            service.deletePaymentMode(id);
            return ResponseEntity.ok(ApiResponseUtil.success("Payment mode deleted successfully",
                    new SimpleStatusDTO("SUCCESS")));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponseUtil.error("Payment mode not found", "RESOURCE_NOT_FOUND"));
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<PaymentModeDTO>> togglePaymentModeStatus(@PathVariable Long id,
            @RequestParam Boolean isActive) {
        try {
            PaymentModeDTO updated = service.togglePaymentModeStatus(id, isActive);
            return ResponseEntity.ok(ApiResponseUtil.success("Payment mode status updated successfully", updated));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponseUtil.error("Payment mode not found", "RESOURCE_NOT_FOUND"));
        }
    }
}

