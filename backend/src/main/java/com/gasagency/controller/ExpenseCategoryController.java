package com.gasagency.controller;

import com.gasagency.dto.response.ExpenseCategoryDTO;
import com.gasagency.dto.response.PagedResponseDTO;
import com.gasagency.dto.response.SimpleStatusDTO;
import com.gasagency.service.ExpenseCategoryService;
import com.gasagency.util.ApiResponse;
import com.gasagency.util.ApiResponseUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/expense-categories")
@PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
public class ExpenseCategoryController {

    private final ExpenseCategoryService service;

    public ExpenseCategoryController(ExpenseCategoryService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponseDTO<ExpenseCategoryDTO>>> getAllCategories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ExpenseCategoryDTO> categories = service.getAllCategories(pageable);
        return ResponseEntity.ok(ApiResponseUtil.success("Expense categories retrieved successfully", categories));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<ExpenseCategoryDTO>>> getActiveCategories() {
        List<ExpenseCategoryDTO> categories = service.getActiveCategories();
        return ResponseEntity.ok(ApiResponseUtil.success("Active expense categories retrieved successfully", categories));
    }

    @GetMapping("/names")
    public ResponseEntity<ApiResponse<List<String>>> getActiveNames() {
        List<String> names = service.getActiveNames();
        return ResponseEntity.ok(ApiResponseUtil.success("Expense category names retrieved successfully", names));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ExpenseCategoryDTO>> getCategoryById(@PathVariable Long id) {
        try {
            ExpenseCategoryDTO category = service.getCategoryById(id);
            return ResponseEntity.ok(ApiResponseUtil.success("Expense category retrieved successfully", category));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponseUtil.error("Expense category not found", "RESOURCE_NOT_FOUND"));
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ExpenseCategoryDTO>> createCategory(@RequestBody ExpenseCategoryDTO dto) {
        try {
            ExpenseCategoryDTO created = service.createCategory(dto);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponseUtil.success("Expense category created successfully", created));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponseUtil.error(e.getMessage(), "EXPENSE_CATEGORY_CREATE_FAILED"));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ExpenseCategoryDTO>> updateCategory(@PathVariable Long id,
            @RequestBody ExpenseCategoryDTO dto) {
        try {
            ExpenseCategoryDTO updated = service.updateCategory(id, dto);
            return ResponseEntity.ok(ApiResponseUtil.success("Expense category updated successfully", updated));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponseUtil.error(e.getMessage(), "EXPENSE_CATEGORY_UPDATE_FAILED"));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<SimpleStatusDTO>> deleteCategory(@PathVariable Long id) {
        try {
            service.deleteCategory(id);
            return ResponseEntity.ok(ApiResponseUtil.success("Expense category deleted successfully",
                    new SimpleStatusDTO("SUCCESS")));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponseUtil.error("Expense category not found", "RESOURCE_NOT_FOUND"));
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<ExpenseCategoryDTO>> toggleCategoryStatus(@PathVariable Long id,
            @RequestParam Boolean isActive) {
        try {
            ExpenseCategoryDTO updated = service.toggleCategoryStatus(id, isActive);
            return ResponseEntity.ok(ApiResponseUtil.success("Expense category status updated successfully", updated));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponseUtil.error("Expense category not found", "RESOURCE_NOT_FOUND"));
        }
    }
}

