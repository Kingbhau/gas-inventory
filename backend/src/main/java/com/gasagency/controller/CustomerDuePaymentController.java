package com.gasagency.controller;

import com.gasagency.dto.CustomerDuePaymentDTO;
import com.gasagency.service.CustomerDuePaymentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/customer-due-payment")
public class CustomerDuePaymentController {

    private final CustomerDuePaymentService service;

    public CustomerDuePaymentController(CustomerDuePaymentService service) {
        this.service = service;
    }

    @GetMapping("/report")
    public ResponseEntity<Page<CustomerDuePaymentDTO>> getDuePaymentReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Double minAmount,
            @RequestParam(required = false) Double maxAmount,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "dueAmount") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        return ResponseEntity
                .ok(service.getDuePaymentReport(fromDate, toDate, customerId, minAmount, maxAmount, pageable));
    }

    @GetMapping("/report/summary")
    public ResponseEntity<CustomerDuePaymentService.CustomerDuePaymentReportSummaryDTO> getDuePaymentReportSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Double minAmount,
            @RequestParam(required = false) Double maxAmount) {

        return ResponseEntity
                .ok(service.getDuePaymentReportSummary(fromDate, toDate, customerId, minAmount, maxAmount));
    }
}
