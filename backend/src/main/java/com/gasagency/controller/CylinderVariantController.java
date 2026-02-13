package com.gasagency.controller;

import com.gasagency.dto.response.CylinderVariantDTO;
import com.gasagency.dto.response.PagedResponseDTO;
import com.gasagency.dto.response.SimpleStatusDTO;
import com.gasagency.service.CylinderVariantService;
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
@RequestMapping("/api/variants")
public class CylinderVariantController {
    private final CylinderVariantService service;

    public CylinderVariantController(CylinderVariantService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CylinderVariantDTO>> createVariant(@Valid @RequestBody CylinderVariantDTO dto) {
        CylinderVariantDTO created = service.createVariant(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseUtil.success("Variant created successfully", created));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CylinderVariantDTO>> getVariant(@PathVariable Long id) {
        CylinderVariantDTO variant = service.getVariantById(id);
        return ResponseEntity.ok(ApiResponseUtil.success("Variant retrieved successfully", variant));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponseDTO<CylinderVariantDTO>>> getAllVariants(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "ASC") String direction) {
        Sort.Direction sortDirection = Sort.Direction.fromString(direction.toUpperCase());
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        Page<CylinderVariantDTO> variants = service.getAllVariants(pageable);
        return ResponseEntity.ok(ApiResponseUtil.success("Variants retrieved successfully", variants));
    }

    @GetMapping("/active/list")
    public ResponseEntity<ApiResponse<List<CylinderVariantDTO>>> getActiveVariants() {
        List<CylinderVariantDTO> variants = service.getActiveVariants();
        return ResponseEntity.ok(ApiResponseUtil.success("Active variants retrieved successfully", variants));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CylinderVariantDTO>> updateVariant(@PathVariable Long id,
            @Valid @RequestBody CylinderVariantDTO dto) {
        CylinderVariantDTO updated = service.updateVariant(id, dto);
        return ResponseEntity.ok(ApiResponseUtil.success("Variant updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<SimpleStatusDTO>> deleteVariant(@PathVariable Long id) {
        service.deleteVariant(id);
        return ResponseEntity.ok(ApiResponseUtil.success("Variant deleted successfully",
                new SimpleStatusDTO("SUCCESS")));
    }

    @PostMapping("/{id}/reactivate")
    public ResponseEntity<ApiResponse<CylinderVariantDTO>> reactivateVariant(@PathVariable Long id) {
        CylinderVariantDTO variant = service.reactivateVariant(id);
        return ResponseEntity.ok(ApiResponseUtil.success("Variant reactivated successfully", variant));
    }
}

