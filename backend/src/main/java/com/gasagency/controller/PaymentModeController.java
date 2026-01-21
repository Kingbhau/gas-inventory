package com.gasagency.controller;

import com.gasagency.dto.PaymentModeDTO;
import com.gasagency.service.PaymentModeService;
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
@PreAuthorize("hasRole('MANAGER')")
public class PaymentModeController {

    private final PaymentModeService service;

    public PaymentModeController(PaymentModeService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<Page<PaymentModeDTO>> getAllPaymentModes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(service.getAllPaymentModes(pageable));
    }

    @GetMapping("/active")
    public ResponseEntity<List<PaymentModeDTO>> getActivePaymentModes() {
        return ResponseEntity.ok(service.getActivePaymentModes());
    }

    @GetMapping("/names")
    public ResponseEntity<List<String>> getActivePaymentModeNames() {
        return ResponseEntity.ok(service.getActivePaymentModeNames());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentModeDTO> getPaymentModeById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(service.getPaymentModeById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<?> createPaymentMode(@RequestBody PaymentModeDTO dto) {
        try {
            PaymentModeDTO created = service.createPaymentMode(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updatePaymentMode(@PathVariable Long id, @RequestBody PaymentModeDTO dto) {
        try {
            PaymentModeDTO updated = service.updatePaymentMode(id, dto);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePaymentMode(@PathVariable Long id) {
        try {
            service.deletePaymentMode(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> togglePaymentModeStatus(@PathVariable Long id, @RequestParam Boolean isActive) {
        try {
            PaymentModeDTO updated = service.togglePaymentModeStatus(id, isActive);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
