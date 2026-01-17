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
}
