package com.gasagency.controller;

import com.gasagency.dto.response.MonthlyPriceDTO;
import com.gasagency.dto.response.PagedResponseDTO;
import com.gasagency.dto.response.SimpleStatusDTO;
import com.gasagency.service.MonthlyPriceService;
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
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/monthly-prices")
public class MonthlyPriceController {
    private final MonthlyPriceService service;

    public MonthlyPriceController(MonthlyPriceService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MonthlyPriceDTO>> createPrice(@Valid @RequestBody MonthlyPriceDTO dto) {
        MonthlyPriceDTO created = service.createPrice(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseUtil.success("Monthly price created successfully", created));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MonthlyPriceDTO>> getPrice(@PathVariable Long id) {
        MonthlyPriceDTO price = service.getPriceById(id);
        return ResponseEntity.ok(ApiResponseUtil.success("Monthly price retrieved successfully", price));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponseDTO<MonthlyPriceDTO>>> getAllPrices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "ASC") String direction) {
        Sort.Direction sortDirection = Sort.Direction.fromString(direction.toUpperCase());
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        Page<MonthlyPriceDTO> prices = service.getAllPrices(pageable);
        return ResponseEntity.ok(ApiResponseUtil.success("Monthly prices retrieved successfully", prices));
    }

    @GetMapping("/variant/{variantId}")
    public ResponseEntity<ApiResponse<List<MonthlyPriceDTO>>> getPricesByVariant(@PathVariable Long variantId) {
        List<MonthlyPriceDTO> prices = service.getPricesByVariant(variantId);
        return ResponseEntity.ok(ApiResponseUtil.success("Monthly prices retrieved successfully", prices));
    }

    @GetMapping("/variant/{variantId}/month/{monthYear}")
    public ResponseEntity<ApiResponse<MonthlyPriceDTO>> getPriceForMonth(@PathVariable Long variantId,
            @PathVariable String monthYear) {
        LocalDate date = LocalDate.parse(monthYear + "-01");
        MonthlyPriceDTO price = service.getPriceForVariantAndMonth(variantId, date);
        return ResponseEntity.ok(ApiResponseUtil.success("Monthly price retrieved successfully", price));
    }

    @GetMapping("/variant/{variantId}/latest/{monthYear}")
    public ResponseEntity<ApiResponse<MonthlyPriceDTO>> getLatestPriceForMonth(@PathVariable Long variantId,
            @PathVariable String monthYear) {
        LocalDate date = LocalDate.parse(monthYear + "-01");
        MonthlyPriceDTO price = service.getLatestPriceForVariant(variantId, date);
        return ResponseEntity.ok(ApiResponseUtil.success("Latest monthly price retrieved successfully", price));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MonthlyPriceDTO>> updatePrice(@PathVariable Long id,
            @Valid @RequestBody MonthlyPriceDTO dto) {
        MonthlyPriceDTO updated = service.updatePrice(id, dto);
        return ResponseEntity.ok(ApiResponseUtil.success("Monthly price updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<SimpleStatusDTO>> deletePrice(@PathVariable Long id) {
        service.deletePrice(id);
        return ResponseEntity.ok(ApiResponseUtil.success("Monthly price deleted successfully",
                new SimpleStatusDTO("SUCCESS")));
    }
}

