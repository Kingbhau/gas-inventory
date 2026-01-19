package com.gasagency.controller;

import com.gasagency.dto.BankAccountLedgerDTO;
import com.gasagency.service.BankAccountLedgerService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/bank-account-ledgers")
public class BankAccountLedgerController {

    private final BankAccountLedgerService bankAccountLedgerService;

    public BankAccountLedgerController(BankAccountLedgerService bankAccountLedgerService) {
        this.bankAccountLedgerService = bankAccountLedgerService;
    }

    @GetMapping
    public ResponseEntity<Page<BankAccountLedgerDTO>> getAllBankTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "transactionDate") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction,
            @RequestParam(required = false) Long bankAccountId,
            @RequestParam(required = false) String transactionType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String referenceNumber) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<BankAccountLedgerDTO> transactions = bankAccountLedgerService.getAllBankTransactions(
                page, size, pageable, bankAccountId, transactionType, fromDate, toDate, referenceNumber);

        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BankAccountLedgerDTO> getBankTransactionById(@PathVariable Long id) {
        BankAccountLedgerDTO transaction = bankAccountLedgerService.getBankTransactionById(id);
        if (transaction == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(transaction);
    }

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getBankTransactionsSummary(
            @RequestParam(required = false) Long bankAccountId,
            @RequestParam(required = false) String transactionType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String referenceNumber) {

        Map<String, Object> summary = bankAccountLedgerService.getSummary(bankAccountId, transactionType, fromDate,
                toDate, referenceNumber);

        return ResponseEntity.ok(summary);
    }
}
