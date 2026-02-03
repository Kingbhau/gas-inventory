package com.gasagency.controller;

import com.gasagency.dto.BankDepositDTO;
import com.gasagency.service.BankDepositService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/bank-deposits")
public class BankDepositController {
    private final BankDepositService bankDepositService;

    public BankDepositController(BankDepositService bankDepositService) {
        this.bankDepositService = bankDepositService;
    }

    /**
     * Create a new bank deposit - OWNER ONLY
     */
    @PostMapping
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<BankDepositDTO> createDeposit(@Valid @RequestBody BankDepositDTO depositDTO) {
        BankDepositDTO created = bankDepositService.createDeposit(depositDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Get deposit by ID - OWNER and MANAGER can view
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    public ResponseEntity<BankDepositDTO> getDeposit(@PathVariable Long id) {
        return ResponseEntity.ok(bankDepositService.getDepositById(id));
    }

    /**
     * Get all deposits with filters and pagination - OWNER and MANAGER can view
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    public ResponseEntity<Page<BankDepositDTO>> getDeposits(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "depositDate") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortOrder,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) Long bankAccountId,
            @RequestParam(required = false) String paymentMode,
            @RequestParam(required = false) String referenceNumber) {

        Page<BankDepositDTO> deposits = bankDepositService.getDeposits(
                page, size, sortBy, sortOrder,
                fromDate, toDate, bankAccountId,
                paymentMode, referenceNumber);

        return ResponseEntity.ok(deposits);
    }

    /**
     * Update a bank deposit - OWNER ONLY
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<BankDepositDTO> updateDeposit(
            @PathVariable Long id,
            @Valid @RequestBody BankDepositDTO depositDTO) {
        BankDepositDTO updated = bankDepositService.updateDeposit(id, depositDTO);
        return ResponseEntity.ok(updated);
    }

    /**
     * Delete a bank deposit - OWNER ONLY
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Void> deleteDeposit(@PathVariable Long id) {
        bankDepositService.deleteDeposit(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get deposit summary - OWNER ONLY
     */
    @GetMapping("/summary")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Map<String, Object>> getDepositSummary(
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) Long bankAccountId,
            @RequestParam(required = false) String paymentMode,
            @RequestParam(required = false) String referenceNumber) {

        var summary = bankDepositService.getDepositSummary(fromDate, toDate, bankAccountId, paymentMode,
                referenceNumber);

        Map<String, Object> response = new HashMap<>();
        response.put("totalAmount", summary.getTotalAmount());

        return ResponseEntity.ok(response);
    }
}
