package com.gasagency.controller;

import com.gasagency.dto.response.CustomerDTO;
import com.gasagency.dto.response.PagedResponseDTO;
import com.gasagency.dto.response.SimpleStatusDTO;
import com.gasagency.service.CustomerService;
import com.gasagency.util.ApiResponse;
import com.gasagency.util.ApiResponseUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {
    private final CustomerService service;

    public CustomerController(CustomerService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CustomerDTO>> createCustomer(@Valid @RequestBody CustomerDTO dto) {
        CustomerDTO created = service.createCustomer(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseUtil.success("Customer created successfully", created));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerDTO>> getCustomer(@PathVariable Long id) {
        CustomerDTO customer = service.getCustomerById(id);
        return ResponseEntity.ok(ApiResponseUtil.success("Customer retrieved successfully", customer));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponseDTO<CustomerDTO>>> getAllCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "ASC") String direction,
            @RequestParam(required = false) String search) {
        Sort.Direction sortDirection = Sort.Direction.fromString(direction.toUpperCase());
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        Page<CustomerDTO> customers = service.getAllCustomers(pageable, search);
        return ResponseEntity.ok(ApiResponseUtil.success("Customers retrieved successfully", customers));
    }

    @GetMapping("/active/list")
    public ResponseEntity<ApiResponse<List<CustomerDTO>>> getActiveCustomers() {
        List<CustomerDTO> customers = service.getActiveCustomers();
        return ResponseEntity.ok(ApiResponseUtil.success("Active customers retrieved successfully", customers));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<PagedResponseDTO<CustomerDTO>>> getActiveCustomersPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "ASC") String direction,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) java.math.BigDecimal minDueAmount) {
        Sort.Direction sortDirection = Sort.Direction.fromString(direction.toUpperCase());
        if (minDueAmount != null) {
            if ("name".equalsIgnoreCase(sortBy)) {
                sortBy = "customer.name";
            } else if ("id".equalsIgnoreCase(sortBy)) {
                sortBy = "customer.id";
            }
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        Page<CustomerDTO> customers = service.getActiveCustomers(pageable, search, minDueAmount);
        return ResponseEntity.ok(ApiResponseUtil.success("Active customers retrieved successfully", customers));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerDTO>> updateCustomer(
            @PathVariable Long id, @Valid @RequestBody CustomerDTO dto) {
        CustomerDTO updated = service.updateCustomer(id, dto);
        return ResponseEntity.ok(ApiResponseUtil.success("Customer updated successfully", updated));
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<SimpleStatusDTO>> deleteCustomer(@PathVariable Long id) {
        service.deleteCustomer(id);
        return ResponseEntity.ok(ApiResponseUtil.success("Customer deleted successfully",
                new SimpleStatusDTO("SUCCESS")));
    }

    @PostMapping("/{id}/reactivate")
    public ResponseEntity<ApiResponse<CustomerDTO>> reactivateCustomer(@PathVariable Long id) {
        CustomerDTO customer = service.reactivateCustomer(id);
        return ResponseEntity.ok(ApiResponseUtil.success("Customer reactivated successfully", customer));
    }
}

