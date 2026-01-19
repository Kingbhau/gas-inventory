package com.gasagency.service;

import com.gasagency.dto.BankAccountLedgerDTO;
import com.gasagency.entity.BankAccountLedger;
import com.gasagency.repository.BankAccountLedgerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class BankAccountLedgerService {

    private final BankAccountLedgerRepository bankAccountLedgerRepository;

    public BankAccountLedgerService(BankAccountLedgerRepository bankAccountLedgerRepository) {
        this.bankAccountLedgerRepository = bankAccountLedgerRepository;
    }

    @Transactional(readOnly = true)
    public Page<BankAccountLedgerDTO> getAllBankTransactions(
            int page,
            int size,
            Pageable pageable,
            Long bankAccountId,
            String transactionType,
            LocalDate fromDate,
            LocalDate toDate,
            String referenceNumber) {

        Page<BankAccountLedger> transactions;

        // Apply filters based on what's provided
        if (bankAccountId != null && fromDate != null && toDate != null && transactionType != null) {
            // All filters: bankAccountId, dateRange, and transactionType
            LocalDateTime startDate = fromDate.atStartOfDay();
            LocalDateTime endDate = toDate.atTime(23, 59, 59);
            transactions = bankAccountLedgerRepository.findByBankAccountIdAndDateRangeAndType(
                    bankAccountId, startDate, endDate, transactionType, pageable);
        } else if (bankAccountId != null && fromDate != null && toDate != null) {
            // BankAccountId and date range only
            LocalDateTime startDate = fromDate.atStartOfDay();
            LocalDateTime endDate = toDate.atTime(23, 59, 59);
            transactions = bankAccountLedgerRepository.findByBankAccountIdAndDateRange(
                    bankAccountId, startDate, endDate, pageable);
        } else if (bankAccountId != null && transactionType != null) {
            // BankAccountId and transaction type only
            transactions = bankAccountLedgerRepository.findByBankAccountIdAndTransactionType(
                    bankAccountId, transactionType, pageable);
        } else if (bankAccountId != null) {
            // BankAccountId only
            transactions = bankAccountLedgerRepository.findByBankAccountId(bankAccountId, pageable);
        } else if (fromDate != null && toDate != null && transactionType != null) {
            // Date range and transaction type only
            LocalDateTime startDate = fromDate.atStartOfDay();
            LocalDateTime endDate = toDate.atTime(23, 59, 59);
            transactions = bankAccountLedgerRepository.findByDateRangeAndType(
                    startDate, endDate, transactionType, pageable);
        } else if (fromDate != null && toDate != null) {
            // Date range only
            LocalDateTime startDate = fromDate.atStartOfDay();
            LocalDateTime endDate = toDate.atTime(23, 59, 59);
            transactions = bankAccountLedgerRepository.findByDateRange(
                    startDate, endDate, pageable);
        } else if (transactionType != null) {
            // Transaction type only
            transactions = bankAccountLedgerRepository.findByTransactionType(
                    transactionType, pageable);
        } else {
            // No filters - get all
            transactions = bankAccountLedgerRepository.findAll(pageable);
        }

        // Apply reference filter in-memory after fetching
        Page<BankAccountLedger> result = transactions;
        if (referenceNumber != null && !referenceNumber.isEmpty()) {
            final String refFilter = referenceNumber.toLowerCase();
            List<BankAccountLedger> filteredList = transactions.getContent().stream()
                    .filter(ledger -> ledger.getReferenceNumber() != null &&
                            ledger.getReferenceNumber().toLowerCase().contains(refFilter))
                    .toList();
            result = new org.springframework.data.domain.PageImpl<>(filteredList, pageable, filteredList.size());
        }

        return result.map(this::convertToDTO);
    }

    @Transactional(readOnly = true)
    public BankAccountLedgerDTO getBankTransactionById(Long id) {
        return bankAccountLedgerRepository.findById(id)
                .map(this::convertToDTO)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getSummary(Long bankAccountId, String transactionType, LocalDate fromDate,
            LocalDate toDate, String referenceNumber) {
        List<BankAccountLedger> transactions;

        // Apply all filter combinations
        if (bankAccountId != null && fromDate != null && toDate != null && transactionType != null) {
            // All filters
            LocalDateTime startDate = fromDate.atStartOfDay();
            LocalDateTime endDate = toDate.atTime(23, 59, 59);
            transactions = bankAccountLedgerRepository.findByBankAccountIdAndDateRangeAndTransactionType(
                    bankAccountId, startDate, endDate, transactionType);
        } else if (bankAccountId != null && fromDate != null && toDate != null) {
            // Bank account + date range
            LocalDateTime startDate = fromDate.atStartOfDay();
            LocalDateTime endDate = toDate.atTime(23, 59, 59);
            transactions = bankAccountLedgerRepository.findByBankAccountIdAndDateRange(
                    bankAccountId, startDate, endDate);
        } else if (bankAccountId != null && transactionType != null) {
            // Bank account + transaction type
            transactions = bankAccountLedgerRepository.findByBankAccountIdAndTransactionType(
                    bankAccountId, transactionType);
        } else if (fromDate != null && toDate != null && transactionType != null) {
            // Date range + transaction type
            LocalDateTime startDate = fromDate.atStartOfDay();
            LocalDateTime endDate = toDate.atTime(23, 59, 59);
            transactions = bankAccountLedgerRepository.findByDateRangeAndTransactionType(
                    startDate, endDate, transactionType);
        } else if (bankAccountId != null) {
            // Bank account only
            transactions = bankAccountLedgerRepository.findByBankAccountIdOrderByTransactionDateDesc(bankAccountId);
        } else if (fromDate != null && toDate != null) {
            // Date range only
            LocalDateTime startDate = fromDate.atStartOfDay();
            LocalDateTime endDate = toDate.atTime(23, 59, 59);
            transactions = bankAccountLedgerRepository.findByTransactionDateBetween(startDate, endDate);
        } else if (transactionType != null) {
            // Transaction type only
            transactions = bankAccountLedgerRepository.findByTransactionType(transactionType);
        } else {
            // No filters - get all
            transactions = bankAccountLedgerRepository.findAll();
        }

        // Apply reference filter
        if (referenceNumber != null && !referenceNumber.isEmpty()) {
            final String refFilter = referenceNumber.toLowerCase();
            transactions = transactions.stream()
                    .filter(t -> t.getReferenceNumber() != null
                            && t.getReferenceNumber().toLowerCase().contains(refFilter))
                    .toList();
        }

        // Calculate summary
        BigDecimal totalDeposits = BigDecimal.ZERO;
        BigDecimal totalWithdrawals = BigDecimal.ZERO;
        BigDecimal balanceAfter = BigDecimal.ZERO;

        // Track balance by bank account for multiple banks
        Map<Long, BigDecimal> bankBalances = new java.util.HashMap<>();
        Map<Long, String> bankNames = new java.util.HashMap<>();

        // Find the maximum balance (most recent transaction)
        for (BankAccountLedger transaction : transactions) {
            BigDecimal amount = transaction.getAmount();
            if (amount == null) {
                amount = BigDecimal.ZERO;
            }

            if ("DEPOSIT".equals(transaction.getTransactionType())) {
                totalDeposits = totalDeposits.add(amount);
            } else if ("WITHDRAWAL".equals(transaction.getTransactionType())) {
                totalWithdrawals = totalWithdrawals.add(amount);
            }

            // Track balance by bank account
            if (transaction.getBankAccount() != null && transaction.getBalanceAfter() != null) {
                Long bankId = transaction.getBankAccount().getId();
                BigDecimal currentBalance = bankBalances.getOrDefault(bankId, BigDecimal.ZERO);

                // Update balance if this transaction is more recent
                if (transaction.getBalanceAfter().compareTo(currentBalance) > 0) {
                    bankBalances.put(bankId, transaction.getBalanceAfter());
                }

                // Store bank name
                bankNames.put(bankId, transaction.getBankAccount().getBankName() + " - " +
                        transaction.getBankAccount().getAccountNumber());
            }

            // Get the highest balance across all banks
            if (transaction.getBalanceAfter() != null &&
                    transaction.getBalanceAfter().compareTo(balanceAfter) > 0) {
                balanceAfter = transaction.getBalanceAfter();
            }
        }

        BigDecimal netBalance = totalDeposits.subtract(totalWithdrawals);

        Map<String, Object> summary = new java.util.HashMap<>();
        summary.put("totalDeposits", totalDeposits);
        summary.put("totalWithdrawals", totalWithdrawals);
        summary.put("netBalance", netBalance);
        summary.put("balanceAfter", balanceAfter);
        summary.put("transactionCount", transactions.size());

        // Add bank-wise balances
        if (!bankBalances.isEmpty()) {
            Map<String, BigDecimal> bankwiseBalances = new java.util.LinkedHashMap<>();
            for (Long bankId : bankBalances.keySet()) {
                String bankName = bankNames.get(bankId);
                bankwiseBalances.put(bankName, bankBalances.get(bankId));
            }
            summary.put("bankwiseBalances", bankwiseBalances);
        }

        return summary;
    }

    private BankAccountLedgerDTO convertToDTO(BankAccountLedger entity) {
        BankAccountLedgerDTO dto = new BankAccountLedgerDTO();
        dto.setId(entity.getId());
        if (entity.getBankAccount() != null) {
            dto.setBankAccountId(entity.getBankAccount().getId());
            dto.setBankAccountName(entity.getBankAccount().getBankName() + " - " +
                    entity.getBankAccount().getAccountNumber());
        }
        dto.setTransactionType(entity.getTransactionType());
        dto.setAmount(entity.getAmount());
        dto.setBalanceAfter(entity.getBalanceAfter());

        Long saleId = null;
        String saleReferenceNumber = null;
        if (entity.getSale() != null) {
            saleId = entity.getSale().getId();
            saleReferenceNumber = entity.getSale().getReferenceNumber();
        }
        dto.setSaleId(saleId);
        dto.setSaleReferenceNumber(saleReferenceNumber);

        dto.setReferenceNumber(entity.getReferenceNumber());
        dto.setDescription(entity.getDescription());
        dto.setTransactionDate(entity.getTransactionDate());
        return dto;
    }
}
