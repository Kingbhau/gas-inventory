package com.gasagency.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;

public class SaleItemDTO {
    private Long id;

    @NotNull(message = "Variant ID is required.")
    @Min(value = 1, message = "Variant ID must be positive.")
    private Long variantId;

    private String variantName;

    @NotNull(message = "Quantity issued is required.")
    @Min(value = 1, message = "Quantity issued must be at least 1.")
    private Long qtyIssued;

    @NotNull(message = "Empty cylinders received is required.")
    @Min(value = 0, message = "Empty cylinders received cannot be negative.")
    private Long qtyEmptyReceived;

    @NotNull(message = "Base price is required.")
    @DecimalMin(value = "0.01", message = "Base price must be greater than 0.")
    @Digits(integer = 10, fraction = 2, message = "Base price must have at most 2 decimal places.")
    private BigDecimal basePrice;

    @NotNull(message = "Discount is required.")
    @DecimalMin(value = "0.0", message = "Discount cannot be negative.")
    @Digits(integer = 10, fraction = 2, message = "Discount must have at most 2 decimal places.")
    private BigDecimal discount;

    @NotNull(message = "Final price is required.")
    @DecimalMin(value = "0.0", message = "Final price cannot be negative.")
    @Digits(integer = 10, fraction = 2, message = "Final price must have at most 2 decimal places.")
    private BigDecimal finalPrice;

    public SaleItemDTO() {
    }

    public SaleItemDTO(Long id, Long variantId, String variantName, Long qtyIssued, Long qtyEmptyReceived,
            BigDecimal basePrice, BigDecimal discount, BigDecimal finalPrice) {
        this.id = id;
        this.variantId = variantId;
        this.variantName = variantName;
        this.qtyIssued = qtyIssued;
        this.qtyEmptyReceived = qtyEmptyReceived;
        this.basePrice = basePrice;
        this.discount = discount;
        this.finalPrice = finalPrice;
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

    public Long getQtyIssued() {
        return qtyIssued;
    }

    public void setQtyIssued(Long qtyIssued) {
        this.qtyIssued = qtyIssued;
    }

    public Long getQtyEmptyReceived() {
        return qtyEmptyReceived;
    }

    public void setQtyEmptyReceived(Long qtyEmptyReceived) {
        this.qtyEmptyReceived = qtyEmptyReceived;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public void setDiscount(BigDecimal discount) {
        this.discount = discount;
    }

    public BigDecimal getFinalPrice() {
        return finalPrice;
    }

    public void setFinalPrice(BigDecimal finalPrice) {
        this.finalPrice = finalPrice;
    }
}
