package com.gasagency.service;

import com.gasagency.dto.BankDepositDTO;
import com.gasagency.entity.BankDeposit;
import com.gasagency.entity.BankAccount;
import com.gasagency.repository.BankDepositRepository;
import com.gasagency.repository.BankAccountRepository;
import com.gasagency.exception.ResourceNotFoundException;
import com.gasagency.exception.InvalidOperationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@Service
public class BankDepositService {
    private static final Logger logger = LoggerFactory.getLogger(BankDepositService.class);
    private final BankDepositRepository bankDepositRepository;
    private final BankAccountRepository bankAccountRepository;

    public BankDepositService(BankDepositRepository bankDepositRepository,
            BankAccountRepository bankAccountRepository) {
        this.bankDepositRepository = bankDepositRepository;
        this.bankAccountRepository = bankAccountRepository;
    }

    /**
     * Create a new bank deposit
     */
    @Transactional
    public BankDepositDTO createDeposit(BankDepositDTO depositDTO) {
        logger.info("Creating new bank deposit for account: {}", depositDTO.getBankAccountId());

        // Validate bank account exists
        BankAccount bankAccount = bankAccountRepository.findById(depositDTO.getBankAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Bank account not found"));

        if (!bankAccount.getIsActive()) {
            throw new InvalidOperationException("Bank account is inactive");
        }

        // Create entity
        BankDeposit deposit = new BankDeposit(
                bankAccount,
                depositDTO.getDepositDate(),
                depositDTO.getDepositAmount(),
                depositDTO.getReferenceNumber(),
                depositDTO.getPaymentMode());

        if (depositDTO.getNotes() != null) {
            deposit.setNotes(depositDTO.getNotes());
        }

        BankDeposit saved = bankDepositRepository.save(deposit);
        logger.info("Bank deposit created successfully with ID: {}", saved.getId());

        return toDTO(saved);
    }

    /**
     * Get deposit by ID
     */
    @Transactional(readOnly = true)
    public BankDepositDTO getDepositById(Long id) {
        logger.info("Fetching bank deposit with ID: {}", id);
        BankDeposit deposit = bankDepositRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bank deposit not found"));
        return toDTO(deposit);
    }

    /**
     * Update an existing bank deposit
     */
    @Transactional
    public BankDepositDTO updateDeposit(Long id, BankDepositDTO depositDTO) {
        logger.info("Updating bank deposit with ID: {}", id);

        BankDeposit deposit = bankDepositRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bank deposit not found"));

        // If bank account changed, validate it
        if (!deposit.getBankAccount().getId().equals(depositDTO.getBankAccountId())) {
            BankAccount bankAccount = bankAccountRepository.findById(depositDTO.getBankAccountId())
                    .orElseThrow(() -> new ResourceNotFoundException("Bank account not found"));

            if (!bankAccount.getIsActive()) {
                throw new InvalidOperationException("Bank account is inactive");
            }
            deposit.setBankAccount(bankAccount);
        }

        deposit.setDepositDate(depositDTO.getDepositDate());
        deposit.setDepositAmount(depositDTO.getDepositAmount());
        deposit.setPaymentMode(depositDTO.getPaymentMode());
        deposit.setNotes(depositDTO.getNotes());

        BankDeposit updated = bankDepositRepository.save(deposit);
        logger.info("Bank deposit updated successfully with ID: {}", id);

        return toDTO(updated);
    }

    /**
     * Delete deposit
     */
    @Transactional
    public void deleteDeposit(Long id) {
        logger.info("Deleting bank deposit with ID: {}", id);
        if (!bankDepositRepository.existsById(id)) {
            throw new ResourceNotFoundException("Bank deposit not found");
        }
        bankDepositRepository.deleteById(id);
    }

    /**
     * Get all bank deposits with pagination and filtering
     */
    @Transactional(readOnly = true)
    public Page<BankDepositDTO> getDeposits(int page, int size, String sortBy, String sortOrder,
            String fromDate, String toDate, Long bankAccountId, String paymentMode,
            String referenceNumber, String createdBy) {

        logger.info("Fetching bank deposits with filters");

        Pageable pageable = createPageable(page, size, sortBy, sortOrder);

        // Parse dates
        LocalDate startDate = null;
        LocalDate endDate = null;

        try {
            if (fromDate != null && !fromDate.isEmpty()) {
                startDate = LocalDate.parse(fromDate);
            }
            if (toDate != null && !toDate.isEmpty()) {
                endDate = LocalDate.parse(toDate);
            }
        } catch (DateTimeParseException e) {
            logger.warn("Invalid date format provided", e);
        }

        // Use unified filter method that handles all combinations
        Page<BankDeposit> deposits = bankDepositRepository.findByAllFilters(
                startDate,
                endDate,
                bankAccountId,
                (paymentMode != null && !paymentMode.isEmpty()) ? paymentMode : null,
                (referenceNumber != null && !referenceNumber.isEmpty()) ? referenceNumber : null,
                (createdBy != null && !createdBy.isEmpty()) ? createdBy : null,
                pageable);

        return deposits.map(this::toDTO);
    }

    /**
     * Get deposit summary with all filters
     */
    @Transactional(readOnly = true)
    public DepositSummaryDTO getDepositSummary(String fromDate, String toDate, Long bankAccountId,
            String paymentMode, String referenceNumber, String createdBy) {
        LocalDate startDate = null;
        LocalDate endDate = null;

        try {
            if (fromDate != null && !fromDate.isEmpty()) {
                startDate = LocalDate.parse(fromDate);
            }
            if (toDate != null && !toDate.isEmpty()) {
                endDate = LocalDate.parse(toDate);
            }
        } catch (DateTimeParseException e) {
            // Handle parse error - return zero summary
            return new DepositSummaryDTO(java.math.BigDecimal.ZERO);
        }

        var totalAmount = bankDepositRepository.getTotalDepositAmountWithAllFilters(
                startDate, endDate, bankAccountId, paymentMode, referenceNumber,
                (createdBy != null && !createdBy.isEmpty()) ? createdBy : null);

        return new DepositSummaryDTO(totalAmount);
    }

    /**
     * Convert entity to DTO
     */
    private BankDepositDTO toDTO(BankDeposit deposit) {
        BankDepositDTO dto = new BankDepositDTO(
                deposit.getId(),
                deposit.getBankAccount().getId(),
                deposit.getBankAccount().getBankName(),
                deposit.getBankAccount().getAccountNumber(),
                deposit.getDepositDate(),
                deposit.getDepositAmount(),
                deposit.getReferenceNumber(),
                deposit.getPaymentMode(),
                deposit.getNotes());

        if (deposit.getCreatedDate() != null) {
            dto.setCreatedAt(deposit.getCreatedDate().toString());
        }
        if (deposit.getUpdatedDate() != null) {
            dto.setUpdatedAt(deposit.getUpdatedDate().toString());
        }
        if (deposit.getCreatedBy() != null) {
            dto.setCreatedBy(deposit.getCreatedBy());
        }
        if (deposit.getUpdatedBy() != null) {
            dto.setUpdatedBy(deposit.getUpdatedBy());
        }

        return dto;
    }

    /**
     * Create pageable with sort
     */
    private Pageable createPageable(int page, int size, String sortBy, String sortOrder) {
        Sort.Direction direction = "ASC".equalsIgnoreCase(sortOrder) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String validSortField = "depositDate".equals(sortBy) ? sortBy : "depositDate";
        return org.springframework.data.domain.PageRequest.of(page, size,
                Sort.by(direction, validSortField));
    }

    /**
     * Summary DTO
     */
    public static class DepositSummaryDTO {
        private java.math.BigDecimal totalAmount;

        public DepositSummaryDTO(java.math.BigDecimal totalAmount) {
            this.totalAmount = totalAmount;
        }

        public java.math.BigDecimal getTotalAmount() {
            return totalAmount;
        }
    }
}
