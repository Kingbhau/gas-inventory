package com.gasagency.controller;

import com.gasagency.dto.response.CustomerDuePaymentDTO;
import com.gasagency.dto.response.PagedResponseDTO;
import com.gasagency.service.CustomerDuePaymentService;
import com.gasagency.util.ApiResponse;
import com.gasagency.util.ApiResponseUtil;
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
    public ResponseEntity<ApiResponse<PagedResponseDTO<CustomerDuePaymentDTO>>> getDuePaymentReport(
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
        Page<CustomerDuePaymentDTO> report = service.getDuePaymentReport(fromDate, toDate, customerId, minAmount,
                maxAmount, pageable);
        return ResponseEntity.ok(ApiResponseUtil.success("Customer due payment report retrieved successfully", report));
    }

    @GetMapping("/report/summary")
    public ResponseEntity<ApiResponse<CustomerDuePaymentService.CustomerDuePaymentReportSummaryDTO>> getDuePaymentReportSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Double minAmount,
            @RequestParam(required = false) Double maxAmount) {

        CustomerDuePaymentService.CustomerDuePaymentReportSummaryDTO summary = service
                .getDuePaymentReportSummary(fromDate, toDate, customerId, minAmount, maxAmount);
        return ResponseEntity.ok(ApiResponseUtil.success("Customer due payment summary retrieved successfully", summary));
    }
}

