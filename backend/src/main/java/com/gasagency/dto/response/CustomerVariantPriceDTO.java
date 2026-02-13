package com.gasagency.dto.response;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import com.gasagency.validation.ValidPrices;
import java.math.BigDecimal;

@ValidPrices
public class CustomerVariantPriceDTO {
    private Long id;

    @NotNull(message = "Customer ID is required")
    private Long customerId;

    @NotNull(message = "Variant ID is required")
    private Long variantId;

    private String variantName;

    @NotNull(message = "Sale price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Sale price must be positive")
    private BigDecimal salePrice;

    @NotNull(message = "Discount price is required")
    @DecimalMin(value = "0.0", message = "Discount price must be non-negative")
    private BigDecimal discountPrice;

    public CustomerVariantPriceDTO() {
    }

    public CustomerVariantPriceDTO(Long customerId, Long variantId, String variantName,
            BigDecimal salePrice, BigDecimal discountPrice) {
        this.customerId = customerId;
        this.variantId = variantId;
        this.variantName = variantName;
        this.salePrice = salePrice;
        this.discountPrice = discountPrice;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
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

    public BigDecimal getSalePrice() {
        return salePrice;
    }

    public void setSalePrice(BigDecimal salePrice) {
        this.salePrice = salePrice;
    }

    public BigDecimal getDiscountPrice() {
        return discountPrice;
    }

    public void setDiscountPrice(BigDecimal discountPrice) {
        this.discountPrice = discountPrice;
    }
}



