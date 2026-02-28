package com.gasagency.controller;

import com.gasagency.dto.request.LedgerVerificationActionRequestDTO;
import com.gasagency.dto.request.LedgerBulkVerificationRequestDTO;
import com.gasagency.dto.response.CustomerCylinderLedgerDTO;
import com.gasagency.dto.response.LedgerVerificationSummaryDTO;
import com.gasagency.dto.response.PagedResponseDTO;
import com.gasagency.dto.response.SimpleStatusDTO;
import com.gasagency.service.CustomerCylinderLedgerService;
import com.gasagency.util.ApiResponse;
import com.gasagency.util.ApiResponseUtil;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ledger-verification")
public class LedgerVerificationController {

    private final CustomerCylinderLedgerService ledgerService;

    public LedgerVerificationController(CustomerCylinderLedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    @GetMapping("/queue")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<PagedResponseDTO<CustomerCylinderLedgerDTO>>> getQueue(
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) String transactionType,
            @RequestParam(required = false) String paymentMode,
            @RequestParam(required = false) String createdBy,
            @RequestParam(required = false) Long bankAccountId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "transactionDate") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy).and(Sort.by(Sort.Direction.DESC, "id")));
        Page<CustomerCylinderLedgerDTO> queue = ledgerService.getBankVerificationQueue(
                parseDate(fromDate),
                parseDate(toDate),
                transactionType,
                paymentMode,
                createdBy,
                bankAccountId,
                status,
                search,
                pageable);
        return ResponseEntity.ok(ApiResponseUtil.success("Bank confirmation desk queue retrieved successfully", queue));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<LedgerVerificationSummaryDTO>> getSummary(
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) String transactionType,
            @RequestParam(required = false) String paymentMode,
            @RequestParam(required = false) String createdBy,
            @RequestParam(required = false) Long bankAccountId,
            @RequestParam(required = false) String search) {
        LedgerVerificationSummaryDTO summary = ledgerService.getBankVerificationSummary(
                parseDate(fromDate),
                parseDate(toDate),
                transactionType,
                paymentMode,
                createdBy,
                bankAccountId,
                search);
        return ResponseEntity.ok(ApiResponseUtil.success("Bank confirmation desk summary retrieved successfully", summary));
    }

    @PatchMapping("/{ledgerId}/verify")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiResponse<CustomerCylinderLedgerDTO>> verify(
            @PathVariable Long ledgerId,
            @RequestBody(required = false) LedgerVerificationActionRequestDTO request,
            Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "SYSTEM";
        CustomerCylinderLedgerDTO updated = ledgerService.verifyLedgerAmount(ledgerId, request, username);
        return ResponseEntity.ok(ApiResponseUtil.success("Transaction verified successfully", updated));
    }

    @PatchMapping("/{ledgerId}/reject")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiResponse<CustomerCylinderLedgerDTO>> reject(
            @PathVariable Long ledgerId,
            @RequestBody(required = false) LedgerVerificationActionRequestDTO request,
            Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "SYSTEM";
        CustomerCylinderLedgerDTO updated = ledgerService.rejectLedgerAmount(ledgerId, request, username);
        return ResponseEntity.ok(ApiResponseUtil.success("Transaction rejected successfully", updated));
    }

    @PatchMapping("/{ledgerId}/pending")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiResponse<CustomerCylinderLedgerDTO>> moveToPending(
            @PathVariable Long ledgerId,
            @RequestBody(required = false) LedgerVerificationActionRequestDTO request,
            Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "SYSTEM";
        CustomerCylinderLedgerDTO updated = ledgerService.markLedgerAsPending(ledgerId, request, username);
        return ResponseEntity.ok(ApiResponseUtil.success("Transaction moved back to pending", updated));
    }

    @PostMapping("/bulk/verify")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiResponse<SimpleStatusDTO>> bulkVerify(
            @RequestBody LedgerBulkVerificationRequestDTO request,
            Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "SYSTEM";
        long updatedCount = ledgerService.verifyLedgersInBulk(request, username);
        return ResponseEntity.ok(ApiResponseUtil.success(
                updatedCount + " transaction(s) verified successfully",
                new SimpleStatusDTO("SUCCESS")));
    }

    @PostMapping("/bulk/reject")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiResponse<SimpleStatusDTO>> bulkReject(
            @RequestBody LedgerBulkVerificationRequestDTO request,
            Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "SYSTEM";
        long updatedCount = ledgerService.rejectLedgersInBulk(request, username);
        return ResponseEntity.ok(ApiResponseUtil.success(
                updatedCount + " transaction(s) rejected successfully",
                new SimpleStatusDTO("SUCCESS")));
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        return LocalDate.parse(value);
    }
}
