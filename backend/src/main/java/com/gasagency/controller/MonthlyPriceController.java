package com.gasagency.controller;

import com.gasagency.dto.MonthlyPriceDTO;
import com.gasagency.service.MonthlyPriceService;
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
    public ResponseEntity<MonthlyPriceDTO> createPrice(@Valid @RequestBody MonthlyPriceDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createPrice(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MonthlyPriceDTO> getPrice(@PathVariable Long id) {
        return ResponseEntity.ok(service.getPriceById(id));
    }

    @GetMapping
    public ResponseEntity<Page<MonthlyPriceDTO>> getAllPrices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "ASC") String direction) {
        Sort.Direction sortDirection = Sort.Direction.fromString(direction.toUpperCase());
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        return ResponseEntity.ok(service.getAllPrices(pageable));
    }

    @GetMapping("/variant/{variantId}")
    public ResponseEntity<List<MonthlyPriceDTO>> getPricesByVariant(@PathVariable Long variantId) {
        return ResponseEntity.ok(service.getPricesByVariant(variantId));
    }

    @GetMapping("/variant/{variantId}/month/{monthYear}")
    public ResponseEntity<MonthlyPriceDTO> getPriceForMonth(@PathVariable Long variantId,
            @PathVariable String monthYear) {
        LocalDate date = LocalDate.parse(monthYear + "-01");
        return ResponseEntity.ok(service.getPriceForVariantAndMonth(variantId, date));
    }

    @GetMapping("/variant/{variantId}/latest/{monthYear}")
    public ResponseEntity<MonthlyPriceDTO> getLatestPriceForMonth(@PathVariable Long variantId,
            @PathVariable String monthYear) {
        LocalDate date = LocalDate.parse(monthYear + "-01");
        return ResponseEntity.ok(service.getLatestPriceForVariant(variantId, date));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MonthlyPriceDTO> updatePrice(@PathVariable Long id, @Valid @RequestBody MonthlyPriceDTO dto) {
        return ResponseEntity.ok(service.updatePrice(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePrice(@PathVariable Long id) {
        service.deletePrice(id);
        return ResponseEntity.noContent().build();
    }
}
