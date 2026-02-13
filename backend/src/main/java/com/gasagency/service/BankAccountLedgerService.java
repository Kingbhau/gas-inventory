package com.gasagency.service;

import com.gasagency.dto.response.BankAccountBalanceDTO;
import com.gasagency.dto.response.BankAccountLedgerDTO;
import com.gasagency.dto.response.BankAccountLedgerSummaryDTO;
import com.gasagency.entity.BankAccountLedger;
import com.gasagency.repository.BankAccountLedgerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
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

        LocalDateTime startDate = fromDate != null ? fromDate.atStartOfDay() : null;
        LocalDateTime endDate = toDate != null ? toDate.atTime(23, 59, 59) : null;

        Page<BankAccountLedger> transactions = bankAccountLedgerRepository.findByFilters(
                bankAccountId,
                transactionType,
                startDate,
                endDate,
                referenceNumber,
                pageable);

        return transactions.map(this::convertToDTO);
    }

    @Transactional(readOnly = true)
    public BankAccountLedgerDTO getBankTransactionById(Long id) {
        return bankAccountLedgerRepository.findById(id)
                .map(this::convertToDTO)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public BankAccountLedgerSummaryDTO getSummary(Long bankAccountId, String transactionType, LocalDate fromDate,
            LocalDate toDate, String referenceNumber) {
        LocalDateTime startDate = fromDate != null ? fromDate.atStartOfDay() : null;
        LocalDateTime endDate = toDate != null ? toDate.atTime(23, 59, 59) : null;

        List<BankAccountLedger> transactions = bankAccountLedgerRepository.findByFilters(
                bankAccountId,
                transactionType,
                startDate,
                endDate,
                referenceNumber);

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

        BankAccountLedgerSummaryDTO summary = new BankAccountLedgerSummaryDTO();
        summary.setTotalDeposits(totalDeposits);
        summary.setTotalWithdrawals(totalWithdrawals);
        summary.setNetBalance(netBalance);
        summary.setBalanceAfter(balanceAfter);
        summary.setTransactionCount(transactions.size());

        if (!bankBalances.isEmpty()) {
            List<BankAccountBalanceDTO> balances = new ArrayList<>();
            for (Long bankId : bankBalances.keySet()) {
                BankAccountBalanceDTO dto = new BankAccountBalanceDTO();
                dto.setBankName(bankNames.get(bankId));
                dto.setBalance(bankBalances.get(bankId));
                balances.add(dto);
            }
            summary.setBankwiseBalances(balances);
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

