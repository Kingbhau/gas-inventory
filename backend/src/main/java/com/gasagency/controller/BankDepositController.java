package com.gasagency.controller;

import com.gasagency.dto.response.BankDepositDTO;
import com.gasagency.dto.response.BankDepositSummaryDTO;
import com.gasagency.dto.response.PagedResponseDTO;
import com.gasagency.dto.response.SimpleStatusDTO;
import com.gasagency.service.BankDepositService;
import com.gasagency.util.ApiResponse;
import com.gasagency.util.ApiResponseUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;


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
    public ResponseEntity<ApiResponse<BankDepositDTO>> createDeposit(@Valid @RequestBody BankDepositDTO depositDTO) {
        BankDepositDTO created = bankDepositService.createDeposit(depositDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseUtil.success("Bank deposit created successfully", created));
    }

    /**
     * Get deposit by ID - OWNER and MANAGER can view
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<BankDepositDTO>> getDeposit(@PathVariable Long id) {
        BankDepositDTO deposit = bankDepositService.getDepositById(id);
        return ResponseEntity.ok(ApiResponseUtil.success("Bank deposit retrieved successfully", deposit));
    }

    /**
     * Get all deposits with filters and pagination - OWNER and MANAGER can view
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<PagedResponseDTO<BankDepositDTO>>> getDeposits(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "depositDate") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortOrder,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) Long bankAccountId,
            @RequestParam(required = false) String paymentMode,
            @RequestParam(required = false) String referenceNumber,
            @RequestParam(required = false) String createdBy) {

        org.springframework.data.domain.Page<BankDepositDTO> deposits = bankDepositService.getDeposits(
                page, size, sortBy, sortOrder,
                fromDate, toDate, bankAccountId,
                paymentMode, referenceNumber, createdBy);

        return ResponseEntity.ok(ApiResponseUtil.success("Bank deposits retrieved successfully", deposits));
    }

    /**
     * Update a bank deposit - OWNER ONLY
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiResponse<BankDepositDTO>> updateDeposit(
            @PathVariable Long id,
            @Valid @RequestBody BankDepositDTO depositDTO) {
        BankDepositDTO updated = bankDepositService.updateDeposit(id, depositDTO);
        return ResponseEntity.ok(ApiResponseUtil.success("Bank deposit updated successfully", updated));
    }

    /**
     * Delete a bank deposit - OWNER ONLY
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiResponse<SimpleStatusDTO>> deleteDeposit(@PathVariable Long id) {
        bankDepositService.deleteDeposit(id);
        return ResponseEntity.ok(ApiResponseUtil.success("Bank deposit deleted successfully",
                new SimpleStatusDTO("SUCCESS")));
    }

    /**
     * Get deposit summary - OWNER ONLY
     */
    @GetMapping("/summary")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiResponse<BankDepositSummaryDTO>> getDepositSummary(
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) Long bankAccountId,
            @RequestParam(required = false) String paymentMode,
            @RequestParam(required = false) String referenceNumber,
            @RequestParam(required = false) String createdBy) {

        BankDepositSummaryDTO summary = bankDepositService.getDepositSummary(fromDate, toDate, bankAccountId,
                paymentMode,
                referenceNumber, createdBy);
        return ResponseEntity.ok(ApiResponseUtil.success("Bank deposit summary retrieved successfully", summary));
    }
}

