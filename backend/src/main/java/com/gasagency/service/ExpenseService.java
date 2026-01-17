package com.gasagency.service;

import com.gasagency.dto.ExpenseDTO;
import com.gasagency.dto.ExpenseSummaryDTO;
import com.gasagency.entity.Expense;
import com.gasagency.entity.ExpenseCategory;
import com.gasagency.repository.ExpenseRepository;
import com.gasagency.repository.ExpenseCategoryRepository;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class ExpenseService {

        private final ExpenseRepository repository;
        private final ExpenseCategoryRepository categoryRepository;
        private final ModelMapper modelMapper;

        public ExpenseService(ExpenseRepository repository, ExpenseCategoryRepository categoryRepository,
                        ModelMapper modelMapper) {
                this.repository = repository;
                this.categoryRepository = categoryRepository;
                this.modelMapper = modelMapper;
        }

        public Page<ExpenseDTO> getAllExpenses(Pageable pageable) {
                Pageable pageableWithSort = PageRequest.of(
                                pageable.getPageNumber(),
                                pageable.getPageSize(),
                                Sort.by(Sort.Order.desc("expenseDate"), Sort.Order.desc("id")));
                return repository.findAll(pageableWithSort)
                                .map(this::convertToDTO);
        }

        public Page<ExpenseDTO> getExpensesByDateRange(LocalDate fromDate, LocalDate toDate, Pageable pageable) {
                Pageable pageableWithSort = PageRequest.of(
                                pageable.getPageNumber(),
                                pageable.getPageSize(),
                                Sort.by(Sort.Order.desc("expenseDate"), Sort.Order.desc("id")));
                return repository.findByExpenseDateBetween(fromDate, toDate, pageableWithSort)
                                .map(this::convertToDTO);
        }

        public Page<ExpenseDTO> getExpensesByCategory(Long categoryId, Pageable pageable) {
                ExpenseCategory category = categoryRepository.findById(categoryId)
                                .orElseThrow(() -> new RuntimeException("Category not found"));

                Pageable pageableWithSort = PageRequest.of(
                                pageable.getPageNumber(),
                                pageable.getPageSize(),
                                Sort.by(Sort.Order.desc("expenseDate"), Sort.Order.desc("id")));

                return repository.findByCategory(category, pageableWithSort)
                                .map(this::convertToDTO);
        }

        public ExpenseDTO getExpenseById(Long id) {
                Expense expense = repository.findById(id)
                                .orElseThrow(() -> new RuntimeException("Expense not found with id: " + id));
                return convertToDTO(expense);
        }

        public ExpenseDTO createExpense(ExpenseDTO dto) {
                ExpenseCategory category = categoryRepository.findById(dto.getCategoryId())
                                .orElseThrow(() -> new RuntimeException("Category not found"));

                Expense expense = new Expense();
                expense.setDescription(dto.getDescription());
                expense.setAmount(dto.getAmount());
                expense.setCategory(category);
                expense.setExpenseDate(dto.getExpenseDate());
                expense.setNotes(dto.getNotes());

                Expense saved = repository.save(expense);
                return convertToDTO(saved);
        }

        public ExpenseDTO updateExpense(Long id, ExpenseDTO dto) {
                Expense expense = repository.findById(id)
                                .orElseThrow(() -> new RuntimeException("Expense not found with id: " + id));

                ExpenseCategory category = categoryRepository.findById(dto.getCategoryId())
                                .orElseThrow(() -> new RuntimeException("Category not found"));

                expense.setDescription(dto.getDescription());
                expense.setAmount(dto.getAmount());
                expense.setCategory(category);
                expense.setExpenseDate(dto.getExpenseDate());
                expense.setNotes(dto.getNotes());

                Expense updated = repository.save(expense);
                return convertToDTO(updated);
        }

        public void deleteExpense(Long id) {
                if (!repository.existsById(id)) {
                        throw new RuntimeException("Expense not found with id: " + id);
                }
                repository.deleteById(id);
        }

        public ExpenseSummaryDTO getExpensesSummary(LocalDate fromDate, LocalDate toDate, Long categoryId) {
                java.util.List<Expense> expenses;

                if (fromDate != null && toDate != null && categoryId != null) {
                        ExpenseCategory category = categoryRepository.findById(categoryId)
                                        .orElseThrow(() -> new RuntimeException("Category not found"));
                        expenses = repository.findByExpenseDateBetweenAndCategory(fromDate, toDate, category);
                } else if (fromDate != null && toDate != null) {
                        expenses = repository.findByExpenseDateBetween(fromDate, toDate);
                } else if (categoryId != null) {
                        ExpenseCategory category = categoryRepository.findById(categoryId)
                                        .orElseThrow(() -> new RuntimeException("Category not found"));
                        expenses = repository.findByCategory(category);
                } else {
                        expenses = repository.findAll();
                }

                BigDecimal totalAmount = BigDecimal.ZERO;
                for (Expense expense : expenses) {
                        totalAmount = totalAmount.add(expense.getAmount());
                }

                int transactionCount = expenses.size();
                BigDecimal avgExpenseValue = transactionCount > 0
                                ? totalAmount.divide(BigDecimal.valueOf(transactionCount))
                                : BigDecimal.ZERO;

                // Find top category by total amount
                String topCategory = "N/A";
                if (!expenses.isEmpty()) {
                        java.util.Map<String, BigDecimal> categorySums = new java.util.HashMap<>();
                        for (Expense expense : expenses) {
                                String categoryName = expense.getCategory().getName();
                                categorySums.put(categoryName, categorySums.getOrDefault(categoryName, BigDecimal.ZERO)
                                                .add(expense.getAmount()));
                        }
                        topCategory = categorySums.entrySet().stream()
                                        .max(java.util.Map.Entry.comparingByValue())
                                        .map(java.util.Map.Entry::getKey)
                                        .orElse("N/A");
                }

                return new ExpenseSummaryDTO(totalAmount, transactionCount, avgExpenseValue, topCategory);
        }

        private ExpenseDTO convertToDTO(Expense expense) {
                ExpenseDTO dto = modelMapper.map(expense, ExpenseDTO.class);
                dto.setCategory(expense.getCategory().getName());
                dto.setCategoryId(expense.getCategory().getId());
                return dto;
        }
}
