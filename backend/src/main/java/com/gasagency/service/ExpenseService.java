package com.gasagency.service;

import com.gasagency.dto.response.ExpenseDTO;
import com.gasagency.dto.response.ExpenseSummaryDTO;
import com.gasagency.entity.Expense;
import com.gasagency.entity.ExpenseCategory;
import com.gasagency.repository.ExpenseRepository;
import com.gasagency.repository.ExpenseCategoryRepository;
import com.gasagency.repository.BankAccountRepository;
import com.gasagency.repository.PaymentModeRepository;
import com.gasagency.exception.ConcurrencyConflictException;
import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class ExpenseService {

        private final ExpenseRepository repository;
        private final ExpenseCategoryRepository categoryRepository;
        private final BankAccountRepository bankAccountRepository;
        private final PaymentModeRepository paymentModeRepository;
        private final ModelMapper modelMapper;

        public ExpenseService(ExpenseRepository repository, ExpenseCategoryRepository categoryRepository,
                        BankAccountRepository bankAccountRepository, PaymentModeRepository paymentModeRepository,
                        ModelMapper modelMapper) {
                this.repository = repository;
                this.categoryRepository = categoryRepository;
                this.bankAccountRepository = bankAccountRepository;
                this.paymentModeRepository = paymentModeRepository;
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
                        Long categoryId, String paymentMode, Long bankAccountId, Double minAmount, Double maxAmount,
                        String createdBy) {
                LocalDate effectiveFromDate = fromDate != null ? fromDate : LocalDate.of(1970, 1, 1);
                LocalDate effectiveToDate = toDate != null ? toDate : LocalDate.of(2100, 12, 31);
                Pageable pageableWithSort = PageRequest.of(
                                pageable.getPageNumber(),
                                pageable.getPageSize(),
                                Sort.by(Sort.Order.desc("expenseDate"), Sort.Order.desc("id")));

                BigDecimal minAmountBD = minAmount != null ? BigDecimal.valueOf(minAmount) : null;
                BigDecimal maxAmountBD = maxAmount != null ? BigDecimal.valueOf(maxAmount) : null;

                return repository.findByFilters(effectiveFromDate, effectiveToDate, categoryId, paymentMode, bankAccountId,
                                minAmountBD, maxAmountBD,
                                (createdBy != null && !createdBy.isEmpty()) ? createdBy : null,
                                pageableWithSort)
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
        public BigDecimal getTotalAmountBetweenDates(LocalDate fromDate, LocalDate toDate) {
                BigDecimal total = repository.getTotalAmountBetweenDates(fromDate, toDate);
                return total != null ? total : BigDecimal.ZERO;
        }

        @Transactional(readOnly = true)
        public Map<String, BigDecimal> getAmountByCategoryBetween(LocalDate fromDate, LocalDate toDate) {
                Map<String, BigDecimal> result = new HashMap<>();
                for (Object[] row : repository.sumAmountByCategoryBetween(fromDate, toDate)) {
                        if (row == null || row.length < 2) {
                                continue;
                        }
                        String category = row[0] != null ? row[0].toString() : "Other";
                        BigDecimal amount = row[1] instanceof BigDecimal
                                        ? (BigDecimal) row[1]
                                        : BigDecimal.ZERO;
                        result.put(category, amount);
                }
                return result;
        }

        @Transactional(readOnly = true)
        public Map<LocalDate, BigDecimal> getAmountByDateBetween(LocalDate fromDate, LocalDate toDate) {
                Map<LocalDate, BigDecimal> result = new HashMap<>();
                for (Object[] row : repository.sumAmountByDateBetween(fromDate, toDate)) {
                        if (row == null || row.length < 2) {
                                continue;
                        }
                        LocalDate date = (LocalDate) row[0];
                        BigDecimal amount = row[1] instanceof BigDecimal
                                        ? (BigDecimal) row[1]
                                        : BigDecimal.ZERO;
                        result.put(date, amount);
                }
                return result;
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
                String paymentMode = dto.getPaymentMode() != null ? dto.getPaymentMode().trim() : null;
                if (paymentMode == null || paymentMode.isEmpty()) {
                        throw new RuntimeException("Payment mode is required");
                }
                boolean requiresBankAccount = paymentModeRepository.findByName(paymentMode)
                                .map(pm -> Boolean.TRUE.equals(pm.getIsBankAccountRequired()))
                                .orElseThrow(() -> new RuntimeException("Invalid payment mode"));
                ExpenseCategory category = categoryRepository.findById(dto.getCategoryId())
                                .orElseThrow(() -> new RuntimeException("Category not found"));

                Expense expense = new Expense();
                expense.setDescription(dto.getDescription());
                expense.setAmount(dto.getAmount());
                expense.setCategory(category);
                expense.setExpenseDate(dto.getExpenseDate());
                expense.setNotes(dto.getNotes());
                expense.setPaymentMode(paymentMode);
                if (requiresBankAccount) {
                        if (dto.getBankAccountId() == null) {
                                throw new RuntimeException("Bank account is required for selected payment mode");
                        }
                        expense.setBankAccount(bankAccountRepository.findById(dto.getBankAccountId())
                                        .orElseThrow(() -> new RuntimeException("Bank account not found")));
                } else {
                        expense.setBankAccount(null);
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
                String paymentMode = dto.getPaymentMode() != null ? dto.getPaymentMode().trim() : null;
                if (paymentMode == null || paymentMode.isEmpty()) {
                        throw new RuntimeException("Payment mode is required");
                }
                boolean requiresBankAccount = paymentModeRepository.findByName(paymentMode)
                                .map(pm -> Boolean.TRUE.equals(pm.getIsBankAccountRequired()))
                                .orElseThrow(() -> new RuntimeException("Invalid payment mode"));
                Expense expense = repository.findById(id)
                                .orElseThrow(() -> new RuntimeException("Expense not found with id: " + id));

                ExpenseCategory category = categoryRepository.findById(dto.getCategoryId())
                                .orElseThrow(() -> new RuntimeException("Category not found"));

                expense.setDescription(dto.getDescription());
                expense.setAmount(dto.getAmount());
                expense.setCategory(category);
                expense.setExpenseDate(dto.getExpenseDate());
                expense.setNotes(dto.getNotes());
                expense.setPaymentMode(paymentMode);
                if (requiresBankAccount) {
                        if (dto.getBankAccountId() == null) {
                                throw new RuntimeException("Bank account is required for selected payment mode");
                        }
                        expense.setBankAccount(bankAccountRepository.findById(dto.getBankAccountId())
                                        .orElseThrow(() -> new RuntimeException("Bank account not found")));
                } else {
                        expense.setBankAccount(null);
                }
                Expense updated;
                try {
                        updated = repository.save(expense);
                } catch (ObjectOptimisticLockingFailureException e) {
                        throw new ConcurrencyConflictException("Expense", id);
                }
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
                        String paymentMode, Long bankAccountId, Double minAmount, Double maxAmount, String createdBy) {
                LocalDate effectiveFromDate = fromDate != null ? fromDate : LocalDate.of(1970, 1, 1);
                LocalDate effectiveToDate = toDate != null ? toDate : LocalDate.of(2100, 12, 31);
                BigDecimal minAmountBD = minAmount != null ? BigDecimal.valueOf(minAmount) : null;
                BigDecimal maxAmountBD = maxAmount != null ? BigDecimal.valueOf(maxAmount) : null;

                BigDecimal totalAmount = repository.sumAmountByFilters(effectiveFromDate, effectiveToDate, categoryId, paymentMode,
                                bankAccountId, minAmountBD, maxAmountBD,
                                (createdBy != null && !createdBy.isEmpty()) ? createdBy : null);
                Long transactionCount = repository.countByFilters(effectiveFromDate, effectiveToDate, categoryId, paymentMode,
                                bankAccountId, minAmountBD, maxAmountBD,
                                (createdBy != null && !createdBy.isEmpty()) ? createdBy : null);

                BigDecimal avgExpenseValue = transactionCount != null && transactionCount > 0
                                ? totalAmount.divide(BigDecimal.valueOf(transactionCount), 2,
                                                java.math.RoundingMode.HALF_UP)
                                : BigDecimal.ZERO;

                String topCategory = "N/A";
                List<Object[]> categorySums = repository.sumAmountByCategoryFilters(effectiveFromDate, effectiveToDate, categoryId,
                                paymentMode, bankAccountId, minAmountBD, maxAmountBD,
                                (createdBy != null && !createdBy.isEmpty()) ? createdBy : null);
                if (categorySums != null && !categorySums.isEmpty()) {
                        Object[] top = categorySums.stream()
                                        .max((a, b) -> {
                                                BigDecimal av = a[1] instanceof BigDecimal ? (BigDecimal) a[1]
                                                                : BigDecimal.ZERO;
                                                BigDecimal bv = b[1] instanceof BigDecimal ? (BigDecimal) b[1]
                                                                : BigDecimal.ZERO;
                                                return av.compareTo(bv);
                                        })
                                        .orElse(null);
                        if (top != null && top.length > 0 && top[0] != null) {
                                topCategory = top[0].toString();
                        }
                }

                return new ExpenseSummaryDTO(
                                totalAmount != null ? totalAmount : BigDecimal.ZERO,
                                transactionCount != null ? transactionCount.intValue() : 0,
                                avgExpenseValue,
                                topCategory);
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

