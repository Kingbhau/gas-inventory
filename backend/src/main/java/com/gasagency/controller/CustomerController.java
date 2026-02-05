package com.gasagency.controller;

import com.gasagency.dto.CustomerDTO;
import com.gasagency.service.CustomerService;
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
    public ResponseEntity<CustomerDTO> createCustomer(@Valid @RequestBody CustomerDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createCustomer(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerDTO> getCustomer(@PathVariable Long id) {
        return ResponseEntity.ok(service.getCustomerById(id));
    }

    @GetMapping
    public ResponseEntity<Page<CustomerDTO>> getAllCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "ASC") String direction) {
        Sort.Direction sortDirection = Sort.Direction.fromString(direction.toUpperCase());
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        return ResponseEntity.ok(service.getAllCustomers(pageable));
    }

    @GetMapping("/active/list")
    public ResponseEntity<List<CustomerDTO>> getActiveCustomers() {
        return ResponseEntity.ok(service.getActiveCustomers());
    }

    @GetMapping("/active")
    public ResponseEntity<Page<CustomerDTO>> getActiveCustomersPaged(
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
        return ResponseEntity.ok(service.getActiveCustomers(pageable, search, minDueAmount));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustomerDTO> updateCustomer(@PathVariable Long id, @Valid @RequestBody CustomerDTO dto) {
        return ResponseEntity.ok(service.updateCustomer(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Long id) {
        service.deleteCustomer(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reactivate")
    public ResponseEntity<CustomerDTO> reactivateCustomer(@PathVariable Long id) {
        return ResponseEntity.ok(service.reactivateCustomer(id));
    }
}
