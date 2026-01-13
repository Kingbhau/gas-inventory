package com.gasagency.controller;

import com.gasagency.dto.CylinderVariantDTO;
import com.gasagency.service.CylinderVariantService;
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
@RequestMapping("/api/variants")
public class CylinderVariantController {
    private final CylinderVariantService service;

    public CylinderVariantController(CylinderVariantService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<CylinderVariantDTO> createVariant(@Valid @RequestBody CylinderVariantDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createVariant(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CylinderVariantDTO> getVariant(@PathVariable Long id) {
        return ResponseEntity.ok(service.getVariantById(id));
    }

    @GetMapping
    public ResponseEntity<Page<CylinderVariantDTO>> getAllVariants(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "ASC") String direction) {
        Sort.Direction sortDirection = Sort.Direction.fromString(direction.toUpperCase());
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        return ResponseEntity.ok(service.getAllVariants(pageable));
    }

    @GetMapping("/active/list")
    public ResponseEntity<List<CylinderVariantDTO>> getActiveVariants() {
        return ResponseEntity.ok(service.getActiveVariants());
    }

    @PutMapping("/{id}")
    public ResponseEntity<CylinderVariantDTO> updateVariant(@PathVariable Long id,
            @Valid @RequestBody CylinderVariantDTO dto) {
        return ResponseEntity.ok(service.updateVariant(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVariant(@PathVariable Long id) {
        service.deleteVariant(id);
        return ResponseEntity.noContent().build();
    }
}
