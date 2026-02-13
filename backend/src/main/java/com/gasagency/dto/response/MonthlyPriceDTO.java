package com.gasagency.dto.response;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public class MonthlyPriceDTO {
    private Long id;

    @NotNull(message = "Variant ID cannot be null")
    @Positive(message = "Variant ID must be positive")
    private Long variantId;

    private String variantName;

    @NotNull(message = "Month year cannot be null")
    private LocalDate monthYear;

    @NotNull(message = "Base price cannot be null")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    @Digits(integer = 10, fraction = 2, message = "Price must have at most 2 decimal places")
    private BigDecimal basePrice;

    private LocalDate createdAt;

    public MonthlyPriceDTO() {
    }

    public MonthlyPriceDTO(Long id, Long variantId, String variantName, LocalDate monthYear, BigDecimal basePrice,
            LocalDate createdAt) {
        this.id = id;
        this.variantId = variantId;
        this.variantName = variantName;
        this.monthYear = monthYear;
        this.basePrice = basePrice;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getVariantId() {
        return variantId;
    }

    public void setVariantId(Long variantId) {
        this.variantId = variantId;
    }

    public String getVariantName() {
        return variantName;
    }

    public void setVariantName(String variantName) {
        this.variantName = variantName;
    }

    public LocalDate getMonthYear() {
        return monthYear;
    }

    public void setMonthYear(LocalDate monthYear) {
        this.monthYear = monthYear;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }

    public LocalDate getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDate createdAt) {
        this.createdAt = createdAt;
    }
}



