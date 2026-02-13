package com.gasagency.repository;

import com.gasagency.entity.BankDeposit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.List;

@Repository
public interface BankDepositRepository extends JpaRepository<BankDeposit, Long> {

    /**
     * Find deposit by reference number
     */
    Optional<BankDeposit> findByReferenceNumber(String referenceNumber);

    /**
     * Find all deposits for a specific bank account
     */
    Page<BankDeposit> findByBankAccountId(Long bankAccountId, Pageable pageable);

    /**
     * Find deposits by date range
     */
    @Query("SELECT bd FROM BankDeposit bd WHERE bd.depositDate BETWEEN :startDate AND :endDate ORDER BY bd.depositDate DESC")
    Page<BankDeposit> findByDateRange(@Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate, Pageable pageable);

    /**
     * Find deposits by bank account and date range
     */
    @Query("SELECT bd FROM BankDeposit bd WHERE bd.bankAccount.id = :accountId AND bd.depositDate BETWEEN :startDate AND :endDate ORDER BY bd.depositDate DESC")
    Page<BankDeposit> findByBankAccountAndDateRange(@Param("accountId") Long accountId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate, Pageable pageable);

    /**
     * Find deposits by payment mode
     */
    Page<BankDeposit> findByPaymentMode(String paymentMode, Pageable pageable);

    /**
     * Find deposits by reference number pattern
     */
    @Query("SELECT bd FROM BankDeposit bd WHERE bd.referenceNumber LIKE CONCAT('%', :reference, '%') ORDER BY bd.depositDate DESC")
    Page<BankDeposit> findByReferenceNumberContaining(@Param("reference") String reference, Pageable pageable);

    /**
     * Get total deposit amount by date range (handles null dates)
     */
    @Query("SELECT COALESCE(SUM(bd.depositAmount), 0) FROM BankDeposit bd " +
            "WHERE (CAST(:startDate AS java.time.LocalDate) IS NULL OR bd.depositDate >= :startDate) " +
            "AND (CAST(:endDate AS java.time.LocalDate) IS NULL OR bd.depositDate <= :endDate)")
    BigDecimal getTotalDepositAmount(@Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Get total deposit amount by bank account and date range (handles null dates)
     */
    @Query("SELECT COALESCE(SUM(bd.depositAmount), 0) FROM BankDeposit bd " +
            "WHERE bd.bankAccount.id = :accountId " +
            "AND (CAST(:startDate AS java.time.LocalDate) IS NULL OR bd.depositDate >= :startDate) " +
            "AND (CAST(:endDate AS java.time.LocalDate) IS NULL OR bd.depositDate <= :endDate)")
    BigDecimal getTotalDepositAmountByAccount(@Param("accountId") Long accountId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Find recent deposits
     */
    @Query("SELECT bd FROM BankDeposit bd ORDER BY bd.depositDate DESC")
    List<BankDeposit> findRecentDeposits(Pageable pageable);

    @Query("SELECT bd FROM BankDeposit bd WHERE bd.depositDate = :date ORDER BY bd.depositDate DESC")
    List<BankDeposit> findByDepositDate(@Param("date") LocalDate date);

    @Query("SELECT bd FROM BankDeposit bd WHERE bd.depositDate = :date " +
            "AND (:createdBy IS NULL OR :createdBy = '' OR bd.createdBy = :createdBy) " +
            "ORDER BY bd.depositDate DESC")
    List<BankDeposit> findByDepositDateAndCreatedBy(@Param("date") LocalDate date,
            @Param("createdBy") String createdBy);

    /**
     * Check if deposit exists for a reference number
     */
    boolean existsByReferenceNumber(String referenceNumber);

    /**
     * Get total deposit amount with all filter options
     */
    @Query("SELECT COALESCE(SUM(bd.depositAmount), 0) FROM BankDeposit bd " +
            "WHERE (CAST(:startDate AS java.time.LocalDate) IS NULL OR bd.depositDate >= :startDate) " +
            "AND (CAST(:endDate AS java.time.LocalDate) IS NULL OR bd.depositDate <= :endDate) " +
            "AND (CAST(:accountId AS java.lang.Long) IS NULL OR bd.bankAccount.id = :accountId) " +
            "AND (CAST(:paymentMode AS java.lang.String) IS NULL OR bd.paymentMode = :paymentMode) " +
            "AND (CAST(:reference AS java.lang.String) IS NULL OR CAST(bd.referenceNumber AS java.lang.String) LIKE CONCAT('%', CAST(:reference AS java.lang.String), '%')) " +
            "AND (CAST(:createdBy AS java.lang.String) IS NULL OR bd.createdBy = :createdBy)")
    java.math.BigDecimal getTotalDepositAmountWithAllFilters(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("accountId") Long accountId,
            @Param("paymentMode") String paymentMode,
            @Param("reference") String reference,
            @Param("createdBy") String createdBy);

    /**
     * Find deposits with all filter options (date range, bank account, payment
     * mode, reference)
     */
    @Query("SELECT bd FROM BankDeposit bd " +
            "WHERE (CAST(:startDate AS java.time.LocalDate) IS NULL OR bd.depositDate >= :startDate) " +
            "AND (CAST(:endDate AS java.time.LocalDate) IS NULL OR bd.depositDate <= :endDate) " +
            "AND (CAST(:accountId AS java.lang.Long) IS NULL OR bd.bankAccount.id = :accountId) " +
            "AND (CAST(:paymentMode AS java.lang.String) IS NULL OR bd.paymentMode = :paymentMode) " +
            "AND (CAST(:reference AS java.lang.String) IS NULL OR CAST(bd.referenceNumber AS java.lang.String) LIKE CONCAT('%', CAST(:reference AS java.lang.String), '%')) " +
            "AND (CAST(:createdBy AS java.lang.String) IS NULL OR bd.createdBy = :createdBy) " +
            "ORDER BY bd.depositDate DESC")
    Page<BankDeposit> findByAllFilters(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("accountId") Long accountId,
            @Param("paymentMode") String paymentMode,
            @Param("reference") String reference,
            @Param("createdBy") String createdBy,
            Pageable pageable);
}

