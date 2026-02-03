package com.gasagency.service;

import com.gasagency.dto.ExpenseDTO;
import com.gasagency.dto.ExpenseSummaryDTO;
import com.gasagency.entity.Expense;
import com.gasagency.entity.ExpenseCategory;
import com.gasagency.repository.ExpenseRepository;
import com.gasagency.repository.ExpenseCategoryRepository;
import com.gasagency.repository.BankAccountRepository;
import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class ExpenseService {

        private final ExpenseRepository repository;
        private final ExpenseCategoryRepository categoryRepository;
        private final BankAccountRepository bankAccountRepository;
        private final ModelMapper modelMapper;

        public ExpenseService(ExpenseRepository repository, ExpenseCategoryRepository categoryRepository,
                        BankAccountRepository bankAccountRepository, ModelMapper modelMapper) {
                this.repository = repository;
                this.categoryRepository = categoryRepository;
                this.bankAccountRepository = bankAccountRepository;
                this.modelMapper = modelMapper;
        }

        @Transactional(readOnly = true)
        public Page<ExpenseDTO> getAllExpenses(Pageable pageable) {
                Pageable pageableWithSort = PageRequest.of(
                                pageable.getPageNumber(),
                                pageable.getPageSize(),
                                Sort.by(Sort.Order.desc("expenseDate"), Sort.Order.desc("id")));
                return repository.findAll(pageableWithSort)
                                .map(this::convertToDTO);
        }

        @Transactional(readOnly = true)
        public Page<ExpenseDTO> getAllExpenses(Pageable pageable, LocalDate fromDate, LocalDate toDate,
                        Long categoryId, String paymentMode, Long bankAccountId, Double minAmount, Double maxAmount) {
                Pageable pageableWithSort = PageRequest.of(
                                pageable.getPageNumber(),
                                pageable.getPageSize(),
                                Sort.by(Sort.Order.desc("expenseDate"), Sort.Order.desc("id")));

                BigDecimal minAmountBD = minAmount != null ? BigDecimal.valueOf(minAmount) : null;
                BigDecimal maxAmountBD = maxAmount != null ? BigDecimal.valueOf(maxAmount) : null;

                return repository.findByFilters(fromDate, toDate, categoryId, paymentMode, bankAccountId,
                                minAmountBD, maxAmountBD, pageableWithSort)
                                .map(this::convertToDTO);
        }

        @Transactional(readOnly = true)
        public Page<ExpenseDTO> getExpensesByDateRange(LocalDate fromDate, LocalDate toDate, Pageable pageable) {
                Pageable pageableWithSort = PageRequest.of(
                                pageable.getPageNumber(),
                                pageable.getPageSize(),
                                Sort.by(Sort.Order.desc("expenseDate"), Sort.Order.desc("id")));
                return repository.findByExpenseDateBetween(fromDate, toDate, pageableWithSort)
                                .map(this::convertToDTO);
        }

        @Transactional(readOnly = true)
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

        @Transactional(readOnly = true)
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
                expense.setPaymentMode(dto.getPaymentMode());

                if (dto.getBankAccountId() != null && !"Cash".equalsIgnoreCase(dto.getPaymentMode())) {
                        expense.setBankAccount(bankAccountRepository.findById(dto.getBankAccountId())
                                        .orElseThrow(() -> new RuntimeException("Bank account not found")));
                }

                Expense saved = repository.save(expense);
                // Clear dashboard cache since expenses affect daily/monthly metrics
                clearDashboardCache();
                return convertToDTO(saved);
        }

        @CacheEvict(value = "dashboardCache", allEntries = true)
        public void clearDashboardCache() {
                // This method is called whenever data changes to invalidate dashboard cache
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
                expense.setPaymentMode(dto.getPaymentMode());

                if (dto.getBankAccountId() != null && !"Cash".equalsIgnoreCase(dto.getPaymentMode())) {
                        expense.setBankAccount(bankAccountRepository.findById(dto.getBankAccountId())
                                        .orElseThrow(() -> new RuntimeException("Bank account not found")));
                } else {
                        expense.setBankAccount(null);
                }
                Expense updated = repository.save(expense);
                // Clear dashboard cache since expenses affect daily/monthly metrics
                clearDashboardCache();
                return convertToDTO(updated);
        }

        public void deleteExpense(Long id) {
                if (!repository.existsById(id)) {
                        throw new RuntimeException("Expense not found with id: " + id);
                }
                repository.deleteById(id);
                // Clear dashboard cache since expenses affect daily/monthly metrics
                clearDashboardCache();
        }

        @Transactional(readOnly = true)
        public ExpenseSummaryDTO getExpensesSummary(LocalDate fromDate, LocalDate toDate, Long categoryId,
                        String paymentMode, Long bankAccountId, Double minAmount, Double maxAmount) {
                List<Expense> expenses = new ArrayList<>();

                // Build filter criteria dynamically
                if (fromDate != null || toDate != null || categoryId != null || paymentMode != null ||
                                bankAccountId != null || minAmount != null || maxAmount != null) {

                        // Use the same filter logic as the paginated query, but get all results
                        BigDecimal minAmountBD = minAmount != null ? BigDecimal.valueOf(minAmount) : null;
                        BigDecimal maxAmountBD = maxAmount != null ? BigDecimal.valueOf(maxAmount) : null;

                        // Get a large page size to retrieve all matching records
                        Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE,
                                        Sort.by(Sort.Order.desc("expenseDate"), Sort.Order.desc("id")));
                        expenses = repository.findByFilters(fromDate, toDate, categoryId, paymentMode,
                                        bankAccountId, minAmountBD, maxAmountBD, pageable).getContent();
                } else {
                        expenses = repository.findAll();
                }

                BigDecimal totalAmount = BigDecimal.ZERO;
                for (Expense expense : expenses) {
                        totalAmount = totalAmount.add(expense.getAmount());
                }

                int transactionCount = expenses.size();
                BigDecimal avgExpenseValue = transactionCount > 0
                                ? totalAmount.divide(BigDecimal.valueOf(transactionCount), 2,
                                                java.math.RoundingMode.HALF_UP)
                                : BigDecimal.ZERO;

                // Find top category by total amount
                String topCategory = "N/A";
                if (!expenses.isEmpty()) {
                        Map<String, BigDecimal> categorySums = new HashMap<>();
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
                dto.setPaymentMode(expense.getPaymentMode());
                if (expense.getBankAccount() != null) {
                        dto.setBankAccountId(expense.getBankAccount().getId());
                        dto.setBankAccountName(expense.getBankAccount().getBankName());
                        dto.setBankAccountNumber(expense.getBankAccount().getAccountNumber());
                }
                return dto;
        }
}
