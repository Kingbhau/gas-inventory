package com.gasagency.controller;

import com.gasagency.dto.BankAccountDTO;
import com.gasagency.dto.BankAccountLedgerDTO;
import com.gasagency.dto.CreateBankAccountRequestDTO;
import com.gasagency.service.BankAccountService;
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
@RequestMapping("/api/bank-accounts")
public class BankAccountController {
    private final BankAccountService bankAccountService;

    public BankAccountController(BankAccountService bankAccountService) {
        this.bankAccountService = bankAccountService;
    }

    @PostMapping
    public ResponseEntity<BankAccountDTO> createBankAccount(@Valid @RequestBody CreateBankAccountRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bankAccountService.createBankAccount(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BankAccountDTO> getBankAccount(@PathVariable Long id) {
        return ResponseEntity.ok(bankAccountService.getBankAccountById(id));
    }

    @GetMapping
    public ResponseEntity<Page<BankAccountDTO>> getAllBankAccounts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {
        // Only allow sorting by id to avoid issues with invalid field names
        String validSortField = "id";
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, validSortField));
        return ResponseEntity.ok(bankAccountService.getAllBankAccounts(pageable));
    }

    @GetMapping("/active/list")
    public ResponseEntity<List<BankAccountDTO>> getActiveBankAccounts() {
        return ResponseEntity.ok(bankAccountService.getActiveBankAccounts());
    }

    @PutMapping("/{id}")
    public ResponseEntity<BankAccountDTO> updateBankAccount(
            @PathVariable Long id,
            @Valid @RequestBody CreateBankAccountRequestDTO request) {
        return ResponseEntity.ok(bankAccountService.updateBankAccount(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBankAccount(@PathVariable Long id) {
        bankAccountService.deleteBankAccount(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateBankAccount(@PathVariable Long id) {
        bankAccountService.deactivateBankAccount(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/activate")
    public ResponseEntity<Void> activateBankAccount(@PathVariable Long id) {
        bankAccountService.activateBankAccount(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/ledger")
    public ResponseEntity<Page<BankAccountLedgerDTO>> getBankAccountLedger(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "transactionDate") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        return ResponseEntity.ok(bankAccountService.getBankAccountLedgerDTO(id, pageable));
    }
}
