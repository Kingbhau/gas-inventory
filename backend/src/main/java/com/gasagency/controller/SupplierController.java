package com.gasagency.controller;

import com.gasagency.dto.request.CreateSupplierRequestDTO;
import com.gasagency.dto.response.SupplierDTO;
import com.gasagency.dto.response.PagedResponseDTO;
import com.gasagency.dto.response.SimpleStatusDTO;
import com.gasagency.service.SupplierService;
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

@RestController
@RequestMapping("/api/suppliers")
public class SupplierController {
    private final SupplierService service;

    public SupplierController(SupplierService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SupplierDTO>> createSupplier(
            @Valid @RequestBody CreateSupplierRequestDTO request) {
        String name = request.getName();
        String contact = request.getContact();
        Long businessId = request.getBusinessId();
        if (businessId == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponseUtil.error("businessId is required", "INVALID_ARGUMENT"));
        }

        SupplierDTO dto = new SupplierDTO();
        dto.setName(name);
        dto.setContact(contact);

        SupplierDTO created = service.createSupplier(dto, businessId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseUtil.success("Supplier created successfully", created));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SupplierDTO>> getSupplier(@PathVariable Long id) {
        SupplierDTO supplier = service.getSupplierById(id);
        return ResponseEntity.ok(ApiResponseUtil.success("Supplier retrieved successfully", supplier));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponseDTO<SupplierDTO>>> getAllSuppliers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "ASC") String direction) {
        Sort.Direction sortDirection = Sort.Direction.fromString(direction.toUpperCase());
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        Page<SupplierDTO> suppliers = service.getAllSuppliers(pageable);
        return ResponseEntity.ok(ApiResponseUtil.success("Suppliers retrieved successfully", suppliers));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SupplierDTO>> updateSupplier(@PathVariable Long id,
            @Valid @RequestBody SupplierDTO dto) {
        SupplierDTO updated = service.updateSupplier(id, dto);
        return ResponseEntity.ok(ApiResponseUtil.success("Supplier updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<SimpleStatusDTO>> deleteSupplier(@PathVariable Long id) {
        service.deleteSupplier(id);
        return ResponseEntity.ok(ApiResponseUtil.success("Supplier deleted successfully",
                new SimpleStatusDTO("SUCCESS")));
    }
}

