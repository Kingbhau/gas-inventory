package com.gasagency.repository;

import com.gasagency.entity.Expense;
import com.gasagency.entity.ExpenseCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

        Page<Expense> findByExpenseDateBetween(LocalDate fromDate, LocalDate toDate, Pageable pageable);

        Page<Expense> findByCategory(ExpenseCategory category, Pageable pageable);

        @Query("SELECT e FROM Expense e WHERE e.expenseDate BETWEEN :fromDate AND :toDate " +
                        "AND e.category.id = :categoryId")
        Page<Expense> findByCategoryAndDateRange(
                        @Param("categoryId") Long categoryId,
                        @Param("fromDate") LocalDate fromDate,
                        @Param("toDate") LocalDate toDate,
                        Pageable pageable);

        @Query("SELECT e FROM Expense e WHERE e.expenseDate BETWEEN :fromDate AND :toDate " +
                        "AND e.amount BETWEEN :minAmount AND :maxAmount")
        Page<Expense> findByDateRangeAndAmountRange(
                        @Param("fromDate") LocalDate fromDate,
                        @Param("toDate") LocalDate toDate,
                        @Param("minAmount") BigDecimal minAmount,
                        @Param("maxAmount") BigDecimal maxAmount,
                        Pageable pageable);

        @Query("SELECT SUM(e.amount) FROM Expense e WHERE e.expenseDate BETWEEN :fromDate AND :toDate")
        BigDecimal getTotalAmountBetweenDates(
                        @Param("fromDate") LocalDate fromDate,
                        @Param("toDate") LocalDate toDate);

        @Query("SELECT e.expenseDate, COALESCE(SUM(e.amount), 0) FROM Expense e " +
                        "WHERE e.expenseDate BETWEEN :fromDate AND :toDate " +
                        "GROUP BY e.expenseDate")
        List<Object[]> sumAmountByDateBetween(
                        @Param("fromDate") LocalDate fromDate,
                        @Param("toDate") LocalDate toDate);

        @Query("SELECT e.category.name, COALESCE(SUM(e.amount), 0) FROM Expense e " +
                        "WHERE e.expenseDate BETWEEN :fromDate AND :toDate " +
                        "GROUP BY e.category.name")
        List<Object[]> sumAmountByCategoryBetween(
                        @Param("fromDate") LocalDate fromDate,
                        @Param("toDate") LocalDate toDate);

        @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e")
        BigDecimal sumAllAmounts();

        @Query("SELECT e.category.name, COALESCE(SUM(e.amount), 0) FROM Expense e " +
                        "GROUP BY e.category.name")
        List<Object[]> sumAmountByCategoryAll();

        @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE " +
                        "(:fromDate IS NULL OR e.expenseDate >= :fromDate) AND " +
                        "(:toDate IS NULL OR e.expenseDate <= :toDate) AND " +
                        "(:categoryId IS NULL OR e.category.id = :categoryId) AND " +
                        "(:paymentMode IS NULL OR e.paymentMode = :paymentMode) AND " +
                        "(:bankAccountId IS NULL OR e.bankAccount.id = :bankAccountId) AND " +
                        "(:minAmount IS NULL OR e.amount >= :minAmount) AND " +
                        "(:maxAmount IS NULL OR e.amount <= :maxAmount) AND " +
                        "(:createdBy IS NULL OR e.createdBy = :createdBy)")
        BigDecimal sumAmountByFilters(
                        @Param("fromDate") LocalDate fromDate,
                        @Param("toDate") LocalDate toDate,
                        @Param("categoryId") Long categoryId,
                        @Param("paymentMode") String paymentMode,
                        @Param("bankAccountId") Long bankAccountId,
                        @Param("minAmount") BigDecimal minAmount,
                        @Param("maxAmount") BigDecimal maxAmount,
                        @Param("createdBy") String createdBy);

        @Query("SELECT COUNT(e) FROM Expense e WHERE " +
                        "(:fromDate IS NULL OR e.expenseDate >= :fromDate) AND " +
                        "(:toDate IS NULL OR e.expenseDate <= :toDate) AND " +
                        "(:categoryId IS NULL OR e.category.id = :categoryId) AND " +
                        "(:paymentMode IS NULL OR e.paymentMode = :paymentMode) AND " +
                        "(:bankAccountId IS NULL OR e.bankAccount.id = :bankAccountId) AND " +
                        "(:minAmount IS NULL OR e.amount >= :minAmount) AND " +
                        "(:maxAmount IS NULL OR e.amount <= :maxAmount) AND " +
                        "(:createdBy IS NULL OR e.createdBy = :createdBy)")
        Long countByFilters(
                        @Param("fromDate") LocalDate fromDate,
                        @Param("toDate") LocalDate toDate,
                        @Param("categoryId") Long categoryId,
                        @Param("paymentMode") String paymentMode,
                        @Param("bankAccountId") Long bankAccountId,
                        @Param("minAmount") BigDecimal minAmount,
                        @Param("maxAmount") BigDecimal maxAmount,
                        @Param("createdBy") String createdBy);

        @Query("SELECT e.category.name, COALESCE(SUM(e.amount), 0) FROM Expense e WHERE " +
                        "(:fromDate IS NULL OR e.expenseDate >= :fromDate) AND " +
                        "(:toDate IS NULL OR e.expenseDate <= :toDate) AND " +
                        "(:categoryId IS NULL OR e.category.id = :categoryId) AND " +
                        "(:paymentMode IS NULL OR e.paymentMode = :paymentMode) AND " +
                        "(:bankAccountId IS NULL OR e.bankAccount.id = :bankAccountId) AND " +
                        "(:minAmount IS NULL OR e.amount >= :minAmount) AND " +
                        "(:maxAmount IS NULL OR e.amount <= :maxAmount) AND " +
                        "(:createdBy IS NULL OR e.createdBy = :createdBy) " +
                        "GROUP BY e.category.name")
        List<Object[]> sumAmountByCategoryFilters(
                        @Param("fromDate") LocalDate fromDate,
                        @Param("toDate") LocalDate toDate,
                        @Param("categoryId") Long categoryId,
                        @Param("paymentMode") String paymentMode,
                        @Param("bankAccountId") Long bankAccountId,
                        @Param("minAmount") BigDecimal minAmount,
                        @Param("maxAmount") BigDecimal maxAmount,
                        @Param("createdBy") String createdBy);

        @Query("SELECT COUNT(e) FROM Expense e WHERE e.expenseDate BETWEEN :fromDate AND :toDate")
        Long getCountBetweenDates(
                        @Param("fromDate") LocalDate fromDate,
                        @Param("toDate") LocalDate toDate);

        List<Expense> findByExpenseDateBetween(LocalDate fromDate, LocalDate toDate);

        List<Expense> findByCategory(ExpenseCategory category);

        @Query("SELECT e FROM Expense e WHERE e.expenseDate BETWEEN :fromDate AND :toDate " +
                        "AND e.category = :category")
        List<Expense> findByExpenseDateBetweenAndCategory(
                        @Param("fromDate") LocalDate fromDate,
                        @Param("toDate") LocalDate toDate,
                        @Param("category") ExpenseCategory category);

        @Query("SELECT e FROM Expense e WHERE " +
                        "(:fromDate IS NULL OR e.expenseDate >= :fromDate) AND " +
                        "(:toDate IS NULL OR e.expenseDate <= :toDate) AND " +
                        "(:categoryId IS NULL OR e.category.id = :categoryId) AND " +
                        "(:paymentMode IS NULL OR e.paymentMode = :paymentMode) AND " +
                        "(:bankAccountId IS NULL OR e.bankAccount.id = :bankAccountId) AND " +
                        "(:minAmount IS NULL OR e.amount >= :minAmount) AND " +
                        "(:maxAmount IS NULL OR e.amount <= :maxAmount) AND " +
                        "(:createdBy IS NULL OR e.createdBy = :createdBy)")
        Page<Expense> findByFilters(
                        @Param("fromDate") LocalDate fromDate,
                        @Param("toDate") LocalDate toDate,
                        @Param("categoryId") Long categoryId,
                        @Param("paymentMode") String paymentMode,
                        @Param("bankAccountId") Long bankAccountId,
                        @Param("minAmount") BigDecimal minAmount,
                        @Param("maxAmount") BigDecimal maxAmount,
                        @Param("createdBy") String createdBy,
                        Pageable pageable);
}
