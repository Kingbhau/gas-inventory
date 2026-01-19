package com.gasagency.service;

import com.gasagency.dto.BankAccountDTO;
import com.gasagency.dto.BankAccountLedgerDTO;
import com.gasagency.dto.CreateBankAccountRequestDTO;
import com.gasagency.entity.BankAccount;
import com.gasagency.entity.BankAccountLedger;
import com.gasagency.entity.Sale;
import com.gasagency.exception.ResourceNotFoundException;
import com.gasagency.repository.BankAccountRepository;
import com.gasagency.repository.BankAccountLedgerRepository;
import com.gasagency.repository.SaleRepository;
import com.gasagency.util.ReferenceNumberGenerator;
import com.gasagency.util.CodeGenerator;
import org.slf4j.LoggerFactory;
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
        private final SaleRepository saleRepository;
        private final ReferenceNumberGenerator referenceNumberGenerator;
        private final CodeGenerator codeGenerator;

        public BankAccountService(BankAccountRepository bankAccountRepository,
                        BankAccountLedgerRepository bankAccountLedgerRepository,
                        SaleRepository saleRepository,
                        ReferenceNumberGenerator referenceNumberGenerator,
                        CodeGenerator codeGenerator) {
                this.bankAccountRepository = bankAccountRepository;
                this.bankAccountLedgerRepository = bankAccountLedgerRepository;
                this.saleRepository = saleRepository;
                this.referenceNumberGenerator = referenceNumberGenerator;
                this.codeGenerator = codeGenerator;
        }

        public BankAccountDTO createBankAccount(CreateBankAccountRequestDTO request) {
                // Check if account number already exists
                bankAccountRepository.findByAccountNumber(request.getAccountNumber())
                                .ifPresent(existingAccount -> {
                                        throw new IllegalArgumentException("Account number already exists");
                                });

                // Generate auto-assigned bank code
                String bankCode = codeGenerator.generateBankCode();

                BankAccount bankAccount = new BankAccount(
                                bankCode,
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
                bankAccount.setUpdatedDate(LocalDateTime.now());

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
                bankAccount.setUpdatedDate(LocalDateTime.now());
                bankAccountRepository.save(bankAccount);
        }

        public void activateBankAccount(Long bankAccountId) {
                BankAccount bankAccount = bankAccountRepository.findById(bankAccountId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Bank account not found with id: " + bankAccountId));

                bankAccount.setIsActive(true);
                bankAccount.setUpdatedDate(LocalDateTime.now());
                bankAccountRepository.save(bankAccount);
        }

        /**
         * Record a deposit to the bank account and create a ledger entry
         */
        @Transactional
        public BankAccountLedger recordDeposit(Long bankAccountId, BigDecimal amount, Long saleId,
                        String referenceNumber, String description) {
                BankAccount bankAccount = bankAccountRepository.findById(bankAccountId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Bank account not found with id: " + bankAccountId));

                LoggerFactory.getLogger(this.getClass())
                                .info("recordDeposit called - bankAccountId: {}, amount: {}, saleId: {}",
                                                bankAccountId, amount, saleId);

                // Generate bank transaction reference
                String bankReference = referenceNumberGenerator.generateBankTransactionReference(
                                bankAccount.getCode(), "DEP");

                LoggerFactory.getLogger(this.getClass())
                                .info("Found bank account: {} - Current balance: {}", bankAccount.getBankName(),
                                                bankAccount.getCurrentBalance());

                // Update bank account balance
                BigDecimal newBalance = bankAccount.getCurrentBalance().add(amount);
                bankAccount.setCurrentBalance(newBalance);
                bankAccount.setUpdatedDate(LocalDateTime.now());
                BankAccount savedBankAccount = bankAccountRepository.save(bankAccount);

                LoggerFactory.getLogger(this.getClass())
                                .info("Bank account balance updated - Old: {}, New: {}",
                                                bankAccount.getCurrentBalance().subtract(amount), newBalance);

                // Create ledger entry with generated bank reference
                Sale sale = null;
                if (saleId != null) {
                        sale = saleRepository.findById(saleId).orElse(null);
                }
                BankAccountLedger ledgerEntry = new BankAccountLedger(
                                savedBankAccount,
                                "DEPOSIT",
                                amount,
                                newBalance,
                                sale,
                                bankReference,
                                description);
                BankAccountLedger savedEntry = bankAccountLedgerRepository.save(ledgerEntry);

                LoggerFactory.getLogger(this.getClass())
                                .info("Bank account ledger entry created - ID: {}, Reference: {}", savedEntry.getId(),
                                                bankReference);

                return savedEntry;
        }

        /**
         * Record a withdrawal from the bank account and create a ledger entry
         */
        @Transactional
        public BankAccountLedger recordWithdrawal(Long bankAccountId, BigDecimal amount,
                        String referenceNumber, String description) {
                BankAccount bankAccount = bankAccountRepository.findById(bankAccountId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Bank account not found with id: " + bankAccountId));

                LoggerFactory.getLogger(this.getClass())
                                .info("recordWithdrawal called - bankAccountId: {}, amount: {}",
                                                bankAccountId, amount);

                // Generate bank transaction reference
                String bankReference = referenceNumberGenerator.generateBankTransactionReference(
                                bankAccount.getCode(), "WIT");

                LoggerFactory.getLogger(this.getClass())
                                .info("Found bank account: {} - Current balance: {}", bankAccount.getBankName(),
                                                bankAccount.getCurrentBalance());

                // Update bank account balance
                BigDecimal newBalance = bankAccount.getCurrentBalance().subtract(amount);
                bankAccount.setCurrentBalance(newBalance);
                bankAccount.setUpdatedDate(LocalDateTime.now());
                BankAccount savedBankAccount = bankAccountRepository.save(bankAccount);

                LoggerFactory.getLogger(this.getClass())
                                .info("Bank account balance updated - Old: {}, New: {}",
                                                bankAccount.getCurrentBalance().add(amount),
                                                newBalance);

                // Create ledger entry with generated bank reference
                BankAccountLedger ledgerEntry = new BankAccountLedger(
                                savedBankAccount,
                                "WITHDRAWAL",
                                amount,
                                newBalance,
                                null,
                                bankReference,
                                description);
                BankAccountLedger savedEntry = bankAccountLedgerRepository.save(ledgerEntry);

                LoggerFactory.getLogger(this.getClass())
                                .info("Bank account ledger entry created - ID: {}, Reference: {}", savedEntry.getId(),
                                                bankReference);

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
                                bankAccount.getCode(),
                                bankAccount.getBankName(),
                                bankAccount.getAccountNumber(),
                                bankAccount.getAccountHolderName(),
                                bankAccount.getAccountName(),
                                bankAccount.getAccountType(),
                                bankAccount.getCurrentBalance(),
                                bankAccount.getIsActive(),
                                bankAccount.getCreatedDate(),
                                bankAccount.getUpdatedDate());
        }

        private BankAccountLedgerDTO mapLedgerToDTO(BankAccountLedger ledger) {
                String saleReferenceNumber = null;
                Long saleId = null;
                if (ledger.getSale() != null) {
                        saleReferenceNumber = ledger.getSale().getReferenceNumber();
                        saleId = ledger.getSale().getId();
                }
                return new BankAccountLedgerDTO(
                                ledger.getId(),
                                ledger.getBankAccount().getId(),
                                ledger.getBankAccount().getBankName() + " - "
                                                + ledger.getBankAccount().getAccountNumber(),
                                ledger.getTransactionType(),
                                ledger.getAmount(),
                                ledger.getBalanceAfter(),
                                saleId,
                                saleReferenceNumber,
                                ledger.getReferenceNumber(),
                                ledger.getDescription(),
                                ledger.getTransactionDate(),
                                ledger.getCreatedDate());
        }
}
