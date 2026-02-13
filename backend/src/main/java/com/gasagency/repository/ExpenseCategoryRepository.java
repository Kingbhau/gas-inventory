package com.gasagency.repository;

import com.gasagency.entity.ExpenseCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseCategoryRepository extends JpaRepository<ExpenseCategory, Long> {

    Optional<ExpenseCategory> findByName(String name);

    List<ExpenseCategory> findByIsActiveTrue();

    @Query("SELECT c.name FROM ExpenseCategory c WHERE c.isActive = true ORDER BY c.name")
    List<String> findActiveNames();
}

