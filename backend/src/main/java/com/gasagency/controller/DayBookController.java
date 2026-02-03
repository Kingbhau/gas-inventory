package com.gasagency.controller;

import com.gasagency.dto.DayBookDTO;
import com.gasagency.dto.DayBookSummaryDTO;
import com.gasagency.service.DayBookService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<Page<DayBookDTO>> getCurrentDayTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "transactionDate") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        return ResponseEntity.ok(dayBookService.getCurrentDayTransactions(pageable));
    }

    /**
     * Get all transactions for a specific date with pagination
     * Query param format: YYYY-MM-DD (e.g.,
     * /api/daybook/by-date?date=2026-02-02&page=0&size=10)
     */
    @GetMapping("/by-date")
    public ResponseEntity<Page<DayBookDTO>> getTransactionsByDate(
            @RequestParam(required = false) String date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "transactionDate") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {

        LocalDate transactionDate;

        if (date == null || date.isEmpty()) {
            transactionDate = LocalDate.now();
        } else {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                transactionDate = LocalDate.parse(date, formatter);
            } catch (Exception e) {
                return ResponseEntity.badRequest().build();
            }
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        return ResponseEntity.ok(dayBookService.getTransactionsByDate(transactionDate, pageable));
    }

    /**
     * Get summary for a specific date (all transactions, no pagination)
     */
    @GetMapping("/summary")
    public ResponseEntity<DayBookSummaryDTO> getSummaryByDate(
            @RequestParam(required = false) String date) {

        LocalDate transactionDate;

        if (date == null || date.isEmpty()) {
            transactionDate = LocalDate.now();
        } else {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                transactionDate = LocalDate.parse(date, formatter);
            } catch (Exception e) {
                return ResponseEntity.badRequest().build();
            }
        }

        return ResponseEntity.ok(dayBookService.getTransactionsByDateSummary(transactionDate));
    }
}
