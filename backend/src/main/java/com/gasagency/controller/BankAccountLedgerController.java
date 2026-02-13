package com.gasagency.controller;

import com.gasagency.dto.response.BankAccountLedgerDTO;
import com.gasagency.dto.response.BankAccountLedgerSummaryDTO;
import com.gasagency.dto.response.PagedResponseDTO;
import com.gasagency.service.BankAccountLedgerService;
import com.gasagency.util.ApiResponse;
import com.gasagency.util.ApiResponseUtil;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/bank-account-ledgers")
public class BankAccountLedgerController {

    private final BankAccountLedgerService bankAccountLedgerService;

    public BankAccountLedgerController(BankAccountLedgerService bankAccountLedgerService) {
        this.bankAccountLedgerService = bankAccountLedgerService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponseDTO<BankAccountLedgerDTO>>> getAllBankTransactions(
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
        org.springframework.data.domain.Page<BankAccountLedgerDTO> transactions = bankAccountLedgerService.getAllBankTransactions(
                page, size, pageable, bankAccountId, transactionType, fromDate, toDate, referenceNumber);

        return ResponseEntity.ok(ApiResponseUtil.success("Bank account transactions retrieved successfully", transactions));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BankAccountLedgerDTO>> getBankTransactionById(@PathVariable Long id) {
        BankAccountLedgerDTO transaction = bankAccountLedgerService.getBankTransactionById(id);
        if (transaction == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponseUtil.error("Bank transaction not found", "RESOURCE_NOT_FOUND"));
        }
        return ResponseEntity.ok(ApiResponseUtil.success("Bank transaction retrieved successfully", transaction));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<BankAccountLedgerSummaryDTO>> getBankTransactionsSummary(
            @RequestParam(required = false) Long bankAccountId,
            @RequestParam(required = false) String transactionType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String referenceNumber) {

        BankAccountLedgerSummaryDTO summary = bankAccountLedgerService.getSummary(bankAccountId, transactionType,
                fromDate,
                toDate, referenceNumber);

        return ResponseEntity.ok(ApiResponseUtil.success("Bank transactions summary retrieved successfully", summary));
    }
}

