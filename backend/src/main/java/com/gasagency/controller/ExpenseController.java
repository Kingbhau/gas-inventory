package com.gasagency.controller;

import com.gasagency.dto.response.ExpenseDTO;
import com.gasagency.dto.response.ExpenseSummaryDTO;
import com.gasagency.dto.response.PagedResponseDTO;
import com.gasagency.dto.response.SimpleStatusDTO;
import com.gasagency.service.ExpenseService;
import com.gasagency.util.ApiResponse;
import com.gasagency.util.ApiResponseUtil;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/expenses")
@PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
public class ExpenseController {

    private final ExpenseService service;

    public ExpenseController(ExpenseService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponseDTO<ExpenseDTO>>> getAllExpenses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String paymentMode,
            @RequestParam(required = false) Long bankAccountId,
            @RequestParam(required = false) Double minAmount,
            @RequestParam(required = false) Double maxAmount,
            @RequestParam(required = false) String createdBy) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ExpenseDTO> expenses = service.getAllExpenses(pageable, fromDate, toDate, categoryId, paymentMode,
                bankAccountId, minAmount, maxAmount, createdBy);
        return ResponseEntity.ok(ApiResponseUtil.success("Expenses retrieved successfully", expenses));
    }

    @GetMapping("/range")
    public ResponseEntity<ApiResponse<PagedResponseDTO<ExpenseDTO>>> getExpensesByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ExpenseDTO> expenses = service.getExpensesByDateRange(fromDate, toDate, pageable);
        return ResponseEntity.ok(ApiResponseUtil.success("Expenses retrieved successfully", expenses));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<ExpenseSummaryDTO>> getExpensesSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String paymentMode,
            @RequestParam(required = false) Long bankAccountId,
            @RequestParam(required = false) Double minAmount,
            @RequestParam(required = false) Double maxAmount,
            @RequestParam(required = false) String createdBy) {
        ExpenseSummaryDTO summary = service.getExpensesSummary(fromDate, toDate, categoryId, paymentMode, bankAccountId,
                minAmount, maxAmount, createdBy);
        return ResponseEntity.ok(ApiResponseUtil.success("Expenses summary retrieved successfully", summary));
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<ApiResponse<PagedResponseDTO<ExpenseDTO>>> getExpensesByCategory(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        try {
            Page<ExpenseDTO> expenses = service.getExpensesByCategory(categoryId, pageable);
            return ResponseEntity.ok(ApiResponseUtil.success("Expenses retrieved successfully", expenses));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponseUtil.error("Invalid category", "INVALID_ARGUMENT"));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ExpenseDTO>> getExpenseById(@PathVariable Long id) {
        try {
            ExpenseDTO expense = service.getExpenseById(id);
            return ResponseEntity.ok(ApiResponseUtil.success("Expense retrieved successfully", expense));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponseUtil.error("Expense not found", "RESOURCE_NOT_FOUND"));
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ExpenseDTO>> createExpense(@Valid @RequestBody ExpenseDTO dto) {
        try {
            ExpenseDTO created = service.createExpense(dto);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponseUtil.success("Expense created successfully", created));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponseUtil.error(e.getMessage(), "EXPENSE_CREATE_FAILED"));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ExpenseDTO>> updateExpense(@PathVariable Long id,
            @RequestBody ExpenseDTO dto) {
        try {
            ExpenseDTO updated = service.updateExpense(id, dto);
            return ResponseEntity.ok(ApiResponseUtil.success("Expense updated successfully", updated));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponseUtil.error(e.getMessage(), "EXPENSE_UPDATE_FAILED"));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<SimpleStatusDTO>> deleteExpense(@PathVariable Long id) {
        try {
            service.deleteExpense(id);
            return ResponseEntity.ok(ApiResponseUtil.success("Expense deleted successfully",
                    new SimpleStatusDTO("SUCCESS")));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponseUtil.error("Expense not found", "RESOURCE_NOT_FOUND"));
        }
    }
}

