package com.gasagency.repository;

import com.gasagency.entity.BankAccountLedger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BankAccountLedgerRepository extends JpaRepository<BankAccountLedger, Long> {

        @Query("SELECT bal FROM BankAccountLedger bal WHERE bal.bankAccount.id = :bankAccountId ORDER BY bal.transactionDate DESC")
        Page<BankAccountLedger> findByBankAccountId(@Param("bankAccountId") Long bankAccountId, Pageable pageable);

        @Query("SELECT bal FROM BankAccountLedger bal WHERE bal.bankAccount.id = :bankAccountId ORDER BY bal.transactionDate DESC")
        List<BankAccountLedger> findByBankAccountIdOrderByTransactionDateDesc(
                        @Param("bankAccountId") Long bankAccountId);

        @Query("SELECT bal FROM BankAccountLedger bal WHERE bal.sale.id = :saleId")
        List<BankAccountLedger> findBySaleId(@Param("saleId") Long saleId);

        @Query("SELECT bal FROM BankAccountLedger bal WHERE bal.bankAccount.id = :bankAccountId " +
                        "AND bal.transactionDate BETWEEN :startDate AND :endDate ORDER BY bal.transactionDate DESC")
        List<BankAccountLedger> findByBankAccountIdAndDateRange(@Param("bankAccountId") Long bankAccountId,
                        @Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        @Query("SELECT bal FROM BankAccountLedger bal WHERE bal.bankAccount.id = :bankAccountId " +
                        "AND bal.transactionDate BETWEEN :startDate AND :endDate ORDER BY bal.transactionDate DESC")
        Page<BankAccountLedger> findByBankAccountIdAndDateRange(@Param("bankAccountId") Long bankAccountId,
                        @Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate,
                        Pageable pageable);

        @Query("SELECT bal FROM BankAccountLedger bal WHERE bal.bankAccount.id = :bankAccountId " +
                        "AND bal.transactionType = :transactionType ORDER BY bal.transactionDate DESC")
        List<BankAccountLedger> findByBankAccountIdAndTransactionType(@Param("bankAccountId") Long bankAccountId,
                        @Param("transactionType") String transactionType);

        @Query("SELECT bal FROM BankAccountLedger bal WHERE bal.bankAccount.id = :bankAccountId " +
                        "AND bal.transactionType = :transactionType ORDER BY bal.transactionDate DESC")
        Page<BankAccountLedger> findByBankAccountIdAndTransactionType(@Param("bankAccountId") Long bankAccountId,
                        @Param("transactionType") String transactionType, Pageable pageable);

        @Query("SELECT bal FROM BankAccountLedger bal WHERE bal.bankAccount.id = :bankAccountId " +
                        "AND bal.transactionDate BETWEEN :startDate AND :endDate " +
                        "AND bal.transactionType = :transactionType ORDER BY bal.transactionDate DESC")
        Page<BankAccountLedger> findByBankAccountIdAndDateRangeAndType(@Param("bankAccountId") Long bankAccountId,
                        @Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate,
                        @Param("transactionType") String transactionType,
                        Pageable pageable);

        @Query("SELECT bal FROM BankAccountLedger bal WHERE bal.transactionDate BETWEEN :startDate AND :endDate " +
                        "ORDER BY bal.transactionDate DESC")
        Page<BankAccountLedger> findByDateRange(@Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate, Pageable pageable);

        @Query("SELECT bal FROM BankAccountLedger bal WHERE bal.transactionDate BETWEEN :startDate AND :endDate " +
                        "AND bal.transactionType = :transactionType ORDER BY bal.transactionDate DESC")
        Page<BankAccountLedger> findByDateRangeAndType(@Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate,
                        @Param("transactionType") String transactionType, Pageable pageable);

        @Query("SELECT bal FROM BankAccountLedger bal WHERE bal.transactionType = :transactionType " +
                        "ORDER BY bal.transactionDate DESC")
        Page<BankAccountLedger> findByTransactionType(@Param("transactionType") String transactionType,
                        Pageable pageable);

        @Query("SELECT bal FROM BankAccountLedger bal WHERE bal.bankAccount.id = :bankAccountId " +
                        "AND bal.transactionDate BETWEEN :startDate AND :endDate " +
                        "AND bal.transactionType = :transactionType ORDER BY bal.transactionDate DESC")
        List<BankAccountLedger> findByBankAccountIdAndDateRangeAndTransactionType(
                        @Param("bankAccountId") Long bankAccountId,
                        @Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate,
                        @Param("transactionType") String transactionType);

        @Query("SELECT bal FROM BankAccountLedger bal WHERE bal.transactionDate BETWEEN :startDate AND :endDate " +
                        "AND bal.transactionType = :transactionType ORDER BY bal.transactionDate DESC")
        List<BankAccountLedger> findByDateRangeAndTransactionType(@Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate,
                        @Param("transactionType") String transactionType);

        @Query("SELECT bal FROM BankAccountLedger bal WHERE bal.transactionType = :transactionType " +
                        "ORDER BY bal.transactionDate DESC")
        List<BankAccountLedger> findByTransactionType(@Param("transactionType") String transactionType);

        @Query("SELECT bal FROM BankAccountLedger bal WHERE bal.transactionDate BETWEEN :startDate AND :endDate " +
                        "ORDER BY bal.transactionDate DESC")
        List<BankAccountLedger> findByTransactionDateBetween(@Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        @Query("SELECT COUNT(bal) FROM BankAccountLedger bal " +
                        "WHERE EXTRACT(MONTH FROM bal.transactionDate) = EXTRACT(MONTH FROM CAST(:date AS DATE)) " +
                        "AND EXTRACT(YEAR FROM bal.transactionDate) = EXTRACT(YEAR FROM CAST(:date AS DATE))")
        long countByCreatedAtMonthYear(@Param("date") LocalDate date);

        @Query("SELECT bal FROM BankAccountLedger bal WHERE " +
                        "(:bankAccountId IS NULL OR bal.bankAccount.id = :bankAccountId) " +
                        "AND (:transactionType IS NULL OR :transactionType = '' OR bal.transactionType = :transactionType) " +
                        "AND (:startDate IS NULL OR bal.transactionDate >= :startDate) " +
                        "AND (:endDate IS NULL OR bal.transactionDate <= :endDate) " +
                        "AND (:referenceNumber IS NULL OR :referenceNumber = '' OR LOWER(bal.referenceNumber) LIKE LOWER(CONCAT('%', :referenceNumber, '%'))) " +
                        "ORDER BY bal.transactionDate DESC")
        Page<BankAccountLedger> findByFilters(
                        @Param("bankAccountId") Long bankAccountId,
                        @Param("transactionType") String transactionType,
                        @Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate,
                        @Param("referenceNumber") String referenceNumber,
                        Pageable pageable);

        @Query("SELECT bal FROM BankAccountLedger bal WHERE " +
                        "(:bankAccountId IS NULL OR bal.bankAccount.id = :bankAccountId) " +
                        "AND (:transactionType IS NULL OR :transactionType = '' OR bal.transactionType = :transactionType) " +
                        "AND (:startDate IS NULL OR bal.transactionDate >= :startDate) " +
                        "AND (:endDate IS NULL OR bal.transactionDate <= :endDate) " +
                        "AND (:referenceNumber IS NULL OR :referenceNumber = '' OR LOWER(bal.referenceNumber) LIKE LOWER(CONCAT('%', :referenceNumber, '%'))) " +
                        "ORDER BY bal.transactionDate DESC")
        List<BankAccountLedger> findByFilters(
                        @Param("bankAccountId") Long bankAccountId,
                        @Param("transactionType") String transactionType,
                        @Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate,
                        @Param("referenceNumber") String referenceNumber);
}

