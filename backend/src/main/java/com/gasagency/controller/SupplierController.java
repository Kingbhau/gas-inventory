package com.gasagency.controller;

import com.gasagency.dto.SupplierDTO;
import com.gasagency.service.SupplierService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/suppliers")
public class SupplierController {
    private final SupplierService service;

    public SupplierController(SupplierService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<SupplierDTO> createSupplier(@Valid @RequestBody Map<String, Object> request) {
        String name = (String) request.get("name");
        String contact = (String) request.get("contact");
        Long businessId = null;

        if (request.get("businessId") instanceof Number) {
            businessId = ((Number) request.get("businessId")).longValue();
        }

        if (businessId == null) {
            return ResponseEntity.badRequest().build();
        }

        SupplierDTO dto = new SupplierDTO();
        dto.setName(name);
        dto.setContact(contact);

        return ResponseEntity.status(HttpStatus.CREATED).body(service.createSupplier(dto, businessId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SupplierDTO> getSupplier(@PathVariable Long id) {
        return ResponseEntity.ok(service.getSupplierById(id));
    }

    @GetMapping
    public ResponseEntity<Page<SupplierDTO>> getAllSuppliers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "ASC") String direction) {
        Sort.Direction sortDirection = Sort.Direction.fromString(direction.toUpperCase());
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        return ResponseEntity.ok(service.getAllSuppliers(pageable));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SupplierDTO> updateSupplier(@PathVariable Long id, @Valid @RequestBody SupplierDTO dto) {
        return ResponseEntity.ok(service.updateSupplier(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSupplier(@PathVariable Long id) {
        service.deleteSupplier(id);
        return ResponseEntity.noContent().build();
    }
}
