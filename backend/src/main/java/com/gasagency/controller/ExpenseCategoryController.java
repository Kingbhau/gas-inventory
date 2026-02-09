package com.gasagency.controller;

import com.gasagency.dto.ExpenseCategoryDTO;
import com.gasagency.service.ExpenseCategoryService;
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
    public ResponseEntity<Page<ExpenseCategoryDTO>> getAllCategories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(service.getAllCategories(pageable));
    }

    @GetMapping("/active")
    public ResponseEntity<List<ExpenseCategoryDTO>> getActiveCategories() {
        return ResponseEntity.ok(service.getActiveCategories());
    }

    @GetMapping("/names")
    public ResponseEntity<List<String>> getActiveNames() {
        return ResponseEntity.ok(service.getActiveNames());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExpenseCategoryDTO> getCategoryById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(service.getCategoryById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<?> createCategory(@RequestBody ExpenseCategoryDTO dto) {
        try {
            ExpenseCategoryDTO created = service.createCategory(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateCategory(@PathVariable Long id, @RequestBody ExpenseCategoryDTO dto) {
        try {
            ExpenseCategoryDTO updated = service.updateCategory(id, dto);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCategory(@PathVariable Long id) {
        try {
            service.deleteCategory(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> toggleCategoryStatus(@PathVariable Long id, @RequestParam Boolean isActive) {
        try {
            ExpenseCategoryDTO updated = service.toggleCategoryStatus(id, isActive);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
