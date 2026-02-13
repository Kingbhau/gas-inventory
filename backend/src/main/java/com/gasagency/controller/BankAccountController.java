package com.gasagency.controller;

import com.gasagency.dto.response.BankAccountDTO;
import com.gasagency.dto.response.BankAccountLedgerDTO;
import com.gasagency.dto.response.PagedResponseDTO;
import com.gasagency.dto.response.SimpleStatusDTO;
import com.gasagency.dto.request.CreateBankAccountRequestDTO;
import com.gasagency.service.BankAccountService;
import com.gasagency.util.ApiResponse;
import com.gasagency.util.ApiResponseUtil;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;


@RestController
@RequestMapping("/api/bank-accounts")
public class BankAccountController {
    private final BankAccountService bankAccountService;

    public BankAccountController(BankAccountService bankAccountService) {
        this.bankAccountService = bankAccountService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BankAccountDTO>> createBankAccount(
            @Valid @RequestBody CreateBankAccountRequestDTO request) {
        BankAccountDTO created = bankAccountService.createBankAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseUtil.success("Bank account created successfully", created));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BankAccountDTO>> getBankAccount(@PathVariable Long id) {
        BankAccountDTO account = bankAccountService.getBankAccountById(id);
        return ResponseEntity.ok(ApiResponseUtil.success("Bank account retrieved successfully", account));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponseDTO<BankAccountDTO>>> getAllBankAccounts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {
        // Only allow sorting by id to avoid issues with invalid field names
        String validSortField = "id";
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, validSortField));
        org.springframework.data.domain.Page<BankAccountDTO> accounts = bankAccountService.getAllBankAccounts(pageable);
        return ResponseEntity.ok(ApiResponseUtil.success("Bank accounts retrieved successfully", accounts));
    }

    @GetMapping("/active/list")
    public ResponseEntity<ApiResponse<List<BankAccountDTO>>> getActiveBankAccounts() {
        List<BankAccountDTO> accounts = bankAccountService.getActiveBankAccounts();
        return ResponseEntity.ok(ApiResponseUtil.success("Active bank accounts retrieved successfully", accounts));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BankAccountDTO>> updateBankAccount(
            @PathVariable Long id,
            @Valid @RequestBody CreateBankAccountRequestDTO request) {
        BankAccountDTO updated = bankAccountService.updateBankAccount(id, request);
        return ResponseEntity.ok(ApiResponseUtil.success("Bank account updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<SimpleStatusDTO>> deleteBankAccount(@PathVariable Long id) {
        bankAccountService.deleteBankAccount(id);
        return ResponseEntity.ok(ApiResponseUtil.success("Bank account deleted successfully",
                new SimpleStatusDTO("SUCCESS")));
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<SimpleStatusDTO>> deactivateBankAccount(@PathVariable Long id) {
        bankAccountService.deactivateBankAccount(id);
        return ResponseEntity.ok(ApiResponseUtil.success("Bank account deactivated successfully",
                new SimpleStatusDTO("SUCCESS")));
    }

    @PutMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<SimpleStatusDTO>> activateBankAccount(@PathVariable Long id) {
        bankAccountService.activateBankAccount(id);
        return ResponseEntity.ok(ApiResponseUtil.success("Bank account activated successfully",
                new SimpleStatusDTO("SUCCESS")));
    }

    @GetMapping("/{id}/ledger")
    public ResponseEntity<ApiResponse<PagedResponseDTO<BankAccountLedgerDTO>>> getBankAccountLedger(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "transactionDate") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        org.springframework.data.domain.Page<BankAccountLedgerDTO> ledger = bankAccountService.getBankAccountLedgerDTO(id, pageable);
        return ResponseEntity.ok(ApiResponseUtil.success("Bank account ledger retrieved successfully", ledger));
    }
}

