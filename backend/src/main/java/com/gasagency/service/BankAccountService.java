package com.gasagency.service;

import com.gasagency.dto.BankAccountDTO;
import com.gasagency.dto.BankAccountLedgerDTO;
import com.gasagency.dto.CreateBankAccountRequestDTO;
import com.gasagency.entity.BankAccount;
import com.gasagency.entity.BankAccountLedger;
import com.gasagency.entity.Warehouse;
import com.gasagency.exception.ResourceNotFoundException;
import com.gasagency.repository.BankAccountRepository;
import com.gasagency.repository.BankAccountLedgerRepository;
import com.gasagency.repository.WarehouseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class BankAccountService {
        private final BankAccountRepository bankAccountRepository;
        private final BankAccountLedgerRepository bankAccountLedgerRepository;

        public BankAccountService(BankAccountRepository bankAccountRepository,
                        BankAccountLedgerRepository bankAccountLedgerRepository) {
                this.bankAccountRepository = bankAccountRepository;
                this.bankAccountLedgerRepository = bankAccountLedgerRepository;
        }

        public BankAccountDTO createBankAccount(CreateBankAccountRequestDTO request) {
                // Check if account number already exists
                bankAccountRepository.findByAccountNumber(request.getAccountNumber())
                                .ifPresent(existingAccount -> {
                                        throw new IllegalArgumentException("Account number already exists");
                                });

                BankAccount bankAccount = new BankAccount(
                                request.getBankName(),
                                request.getAccountNumber(),
                                request.getAccountHolderName(),
                                request.getCurrentBalance());

                bankAccount.setAccountName(request.getAccountName());
                bankAccount.setAccountType(request.getAccountType());

                BankAccount savedBankAccount = bankAccountRepository.save(bankAccount);
                return mapToDTO(savedBankAccount);
        }

        public BankAccountDTO getBankAccountById(Long bankAccountId) {
                BankAccount bankAccount = bankAccountRepository.findById(bankAccountId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Bank account not found with id: " + bankAccountId));
                return mapToDTO(bankAccount);
        }

        public Page<BankAccountDTO> getAllBankAccounts(Pageable pageable) {
                Page<BankAccount> bankAccounts = bankAccountRepository.findAllAccounts(pageable);
                List<BankAccountDTO> dtos = bankAccounts.getContent().stream()
                                .map(this::mapToDTO)
                                .collect(Collectors.toList());

                return new PageImpl<>(dtos, pageable, bankAccounts.getTotalElements());
        }

        public List<BankAccountDTO> getActiveBankAccounts() {
                return bankAccountRepository.findActiveAccounts().stream()
                                .map(this::mapToDTO)
                                .collect(Collectors.toList());
        }

        public BankAccountDTO updateBankAccount(Long bankAccountId, CreateBankAccountRequestDTO request) {
                BankAccount bankAccount = bankAccountRepository.findById(bankAccountId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Bank account not found with id: " + bankAccountId));

                // Check if account number already exists for another account
                if (!bankAccount.getAccountNumber().equals(request.getAccountNumber())) {
                        bankAccountRepository.findByAccountNumber(request.getAccountNumber())
                                        .ifPresent(existingAccount -> {
                                                throw new IllegalArgumentException("Account number already exists");
                                        });
                }

                bankAccount.setBankName(request.getBankName());
                bankAccount.setAccountNumber(request.getAccountNumber());
                bankAccount.setAccountHolderName(request.getAccountHolderName());
                bankAccount.setAccountName(request.getAccountName());
                bankAccount.setAccountType(request.getAccountType());
                bankAccount.setCurrentBalance(request.getCurrentBalance());
                bankAccount.setUpdatedAt(LocalDateTime.now());

                BankAccount updatedBankAccount = bankAccountRepository.save(bankAccount);
                return mapToDTO(updatedBankAccount);
        }

        public void deleteBankAccount(Long bankAccountId) {
                BankAccount bankAccount = bankAccountRepository.findById(bankAccountId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Bank account not found with id: " + bankAccountId));

                bankAccountRepository.delete(bankAccount);
        }

        public void deactivateBankAccount(Long bankAccountId) {
                BankAccount bankAccount = bankAccountRepository.findById(bankAccountId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Bank account not found with id: " + bankAccountId));

                bankAccount.setIsActive(false);
                bankAccount.setUpdatedAt(LocalDateTime.now());
                bankAccountRepository.save(bankAccount);
        }

        public void activateBankAccount(Long bankAccountId) {
                BankAccount bankAccount = bankAccountRepository.findById(bankAccountId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Bank account not found with id: " + bankAccountId));

                bankAccount.setIsActive(true);
                bankAccount.setUpdatedAt(LocalDateTime.now());
                bankAccountRepository.save(bankAccount);
        }

        /**
         * Record a deposit to the bank account and create a ledger entry
         */
        @Transactional
        public BankAccountLedger recordDeposit(Long bankAccountId, BigDecimal amount, Long saleId,
                        String referenceNumber, String description) {
                org.slf4j.LoggerFactory.getLogger(this.getClass())
                                .info("recordDeposit called - bankAccountId: {}, amount: {}, reference: {}",
                                                bankAccountId, amount,
                                                referenceNumber);

                BankAccount bankAccount = bankAccountRepository.findById(bankAccountId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Bank account not found with id: " + bankAccountId));

                org.slf4j.LoggerFactory.getLogger(this.getClass())
                                .info("Found bank account: {} - Current balance: {}", bankAccount.getBankName(),
                                                bankAccount.getCurrentBalance());

                // Update bank account balance
                BigDecimal newBalance = bankAccount.getCurrentBalance().add(amount);
                bankAccount.setCurrentBalance(newBalance);
                bankAccount.setUpdatedAt(LocalDateTime.now());
                BankAccount savedBankAccount = bankAccountRepository.save(bankAccount);

                org.slf4j.LoggerFactory.getLogger(this.getClass())
                                .info("Bank account balance updated - Old: {}, New: {}",
                                                bankAccount.getCurrentBalance().subtract(amount), newBalance);

                // Create ledger entry
                BankAccountLedger ledgerEntry = new BankAccountLedger(
                                savedBankAccount,
                                "DEPOSIT",
                                amount,
                                newBalance,
                                saleId,
                                referenceNumber,
                                description);
                BankAccountLedger savedEntry = bankAccountLedgerRepository.save(ledgerEntry);

                org.slf4j.LoggerFactory.getLogger(this.getClass())
                                .info("Bank account ledger entry created - ID: {}, Reference: {}", savedEntry.getId(),
                                                referenceNumber);

                return savedEntry;
        }

        /**
         * Record a withdrawal from the bank account and create a ledger entry
         */
        @Transactional
        public BankAccountLedger recordWithdrawal(Long bankAccountId, BigDecimal amount,
                        String referenceNumber, String description) {
                org.slf4j.LoggerFactory.getLogger(this.getClass())
                                .info("recordWithdrawal called - bankAccountId: {}, amount: {}, reference: {}",
                                                bankAccountId, amount,
                                                referenceNumber);

                BankAccount bankAccount = bankAccountRepository.findById(bankAccountId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Bank account not found with id: " + bankAccountId));

                org.slf4j.LoggerFactory.getLogger(this.getClass())
                                .info("Found bank account: {} - Current balance: {}", bankAccount.getBankName(),
                                                bankAccount.getCurrentBalance());

                // Update bank account balance
                BigDecimal newBalance = bankAccount.getCurrentBalance().subtract(amount);
                bankAccount.setCurrentBalance(newBalance);
                bankAccount.setUpdatedAt(LocalDateTime.now());
                BankAccount savedBankAccount = bankAccountRepository.save(bankAccount);

                org.slf4j.LoggerFactory.getLogger(this.getClass())
                                .info("Bank account balance updated - Old: {}, New: {}",
                                                bankAccount.getCurrentBalance().add(amount),
                                                newBalance);

                // Create ledger entry
                BankAccountLedger ledgerEntry = new BankAccountLedger(
                                savedBankAccount,
                                "WITHDRAWAL",
                                amount,
                                newBalance,
                                null,
                                referenceNumber,
                                description);
                BankAccountLedger savedEntry = bankAccountLedgerRepository.save(ledgerEntry);

                org.slf4j.LoggerFactory.getLogger(this.getClass())
                                .info("Bank account ledger entry created - ID: {}, Reference: {}", savedEntry.getId(),
                                                referenceNumber);

                return savedEntry;
        }

        /**
         * Get ledger entries for a bank account
         */
        public Page<BankAccountLedger> getBankAccountLedger(Long bankAccountId, Pageable pageable) {
                return bankAccountLedgerRepository.findByBankAccountId(bankAccountId, pageable);
        }

        /**
         * Get paginated ledger DTOs for a bank account
         */
        public Page<BankAccountLedgerDTO> getBankAccountLedgerDTO(Long bankAccountId, Pageable pageable) {
                Page<BankAccountLedger> ledgerPage = bankAccountLedgerRepository.findByBankAccountId(bankAccountId,
                                pageable);
                return ledgerPage.map(this::mapLedgerToDTO);
        }

        private BankAccountDTO mapToDTO(BankAccount bankAccount) {
                return new BankAccountDTO(
                                bankAccount.getId(),
                                bankAccount.getBankName(),
                                bankAccount.getAccountNumber(),
                                bankAccount.getAccountHolderName(),
                                bankAccount.getAccountName(),
                                bankAccount.getAccountType(),
                                bankAccount.getCurrentBalance(),
                                bankAccount.getIsActive(),
                                bankAccount.getCreatedAt(),
                                bankAccount.getUpdatedAt());
        }

        private BankAccountLedgerDTO mapLedgerToDTO(BankAccountLedger ledger) {
                return new BankAccountLedgerDTO(
                                ledger.getId(),
                                ledger.getBankAccount().getId(),
                                ledger.getBankAccount().getBankName() + " - "
                                                + ledger.getBankAccount().getAccountNumber(),
                                ledger.getTransactionType(),
                                ledger.getAmount(),
                                ledger.getBalanceAfter(),
                                ledger.getSaleId(),
                                ledger.getReferenceNumber(),
                                ledger.getDescription(),
                                ledger.getTransactionDate(),
                                ledger.getCreatedAt());
        }
}
