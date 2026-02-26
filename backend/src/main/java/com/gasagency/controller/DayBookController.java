package com.gasagency.controller;

import com.gasagency.dto.response.DayBookDTO;
import com.gasagency.dto.response.DayBookSummaryDTO;
import com.gasagency.dto.response.PagedResponseDTO;
import com.gasagency.service.DayBookService;
import com.gasagency.util.ApiResponse;
import com.gasagency.util.ApiResponseUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/daybook")
public class DayBookController {

    private final DayBookService dayBookService;

    public DayBookController(DayBookService dayBookService) {
        this.dayBookService = dayBookService;
    }

    /**
     * Get all transactions for the current day (Sales and Empty Returns) with
     * pagination
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponseDTO<DayBookDTO>>> getCurrentDayTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "transactionDate") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction,
            @RequestParam(required = false) String createdBy,
            @RequestParam(required = false) String transactionType,
            Authentication authentication) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        String effectiveCreatedBy = resolveCreatedBy(authentication, createdBy);
        String effectiveTransactionType = resolveTransactionType(authentication, transactionType);
        Page<DayBookDTO> transactions = dayBookService.getCurrentDayTransactions(pageable, effectiveCreatedBy,
                effectiveTransactionType);
        return ResponseEntity.ok(ApiResponseUtil.success("Daybook transactions retrieved successfully", transactions));
    }

    /**
     * Get all transactions for a specific date with pagination
     * Query param format: YYYY-MM-DD (e.g.,
     * /api/daybook/by-date?date=2026-02-02&page=0&size=10)
     */
    @GetMapping("/by-date")
    public ResponseEntity<ApiResponse<PagedResponseDTO<DayBookDTO>>> getTransactionsByDate(
            @RequestParam(required = false) String date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "transactionDate") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction,
            @RequestParam(required = false) String createdBy,
            @RequestParam(required = false) String transactionType,
            Authentication authentication) {

        LocalDate transactionDate;

        if (date == null || date.isEmpty()) {
            transactionDate = LocalDate.now();
        } else {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                transactionDate = LocalDate.parse(date, formatter);
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponseUtil.error("Invalid date format. Use yyyy-MM-dd", "INVALID_DATE"));
            }
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        String effectiveCreatedBy = resolveCreatedBy(authentication, createdBy);
        String effectiveTransactionType = resolveTransactionType(authentication, transactionType);
        Page<DayBookDTO> transactions = dayBookService.getTransactionsByDate(transactionDate, pageable,
                effectiveCreatedBy, effectiveTransactionType);
        return ResponseEntity.ok(ApiResponseUtil.success("Daybook transactions retrieved successfully", transactions));
    }

    /**
     * Get summary for a specific date (all transactions, no pagination)
     */
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<DayBookSummaryDTO>> getSummaryByDate(
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String createdBy,
            @RequestParam(required = false) String transactionType,
            Authentication authentication) {

        LocalDate transactionDate;

        if (date == null || date.isEmpty()) {
            transactionDate = LocalDate.now();
        } else {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                transactionDate = LocalDate.parse(date, formatter);
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponseUtil.error("Invalid date format. Use yyyy-MM-dd", "INVALID_DATE"));
            }
        }

        String effectiveCreatedBy = resolveCreatedBy(authentication, createdBy);
        String effectiveTransactionType = resolveTransactionType(authentication, transactionType);
        DayBookSummaryDTO summary = dayBookService.getTransactionsByDateSummary(transactionDate, effectiveCreatedBy,
                effectiveTransactionType);
        return ResponseEntity.ok(ApiResponseUtil.success("Daybook summary retrieved successfully", summary));
    }

    private String resolveCreatedBy(Authentication authentication, String requestedCreatedBy) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return requestedCreatedBy;
        }
        boolean isStaff = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_STAFF".equals(authority.getAuthority()));
        return isStaff ? authentication.getName() : requestedCreatedBy;
    }

    private String resolveTransactionType(Authentication authentication, String requestedTransactionType) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return requestedTransactionType;
        }
        boolean isStaff = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_STAFF".equals(authority.getAuthority()));
        if (!isStaff) {
            return requestedTransactionType;
        }
        if (requestedTransactionType == null || requestedTransactionType.trim().isEmpty()) {
            return "STAFF_CORE";
        }
        String normalized = requestedTransactionType.trim().toUpperCase();
        if ("SALE".equals(normalized) || "EMPTY_RETURN".equals(normalized) || "PAYMENT".equals(normalized)) {
            return normalized;
        }
        return "STAFF_CORE";
    }
}

