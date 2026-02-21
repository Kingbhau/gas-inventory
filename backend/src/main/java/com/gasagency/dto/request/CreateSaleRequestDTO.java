package com.gasagency.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;
import java.time.LocalDate;

public class CreateSaleRequestDTO {
    @NotNull(message = "Customer ID cannot be null")
    @Positive(message = "Customer ID must be a positive number")
    private Long customerId;

    @NotNull(message = "Warehouse ID cannot be null")
    @Positive(message = "Warehouse ID must be a positive number")
    private Long warehouseId;

    @PositiveOrZero(message = "Amount received cannot be negative")
    private BigDecimal amountReceived;

    private String modeOfPayment;

    private Long bankAccountId;

    private LocalDate saleDate;

    @NotEmpty(message = "Sale must contain at least one item")
    @Size(min = 1, max = 100, message = "Sale cannot have more than 100 items")
    @Valid
    private List<SaleItemRequestDTO> items;

    public CreateSaleRequestDTO() {
    }

    public CreateSaleRequestDTO(Long customerId, Long warehouseId, BigDecimal amountReceived, String modeOfPayment,
            Long bankAccountId, LocalDate saleDate, List<SaleItemRequestDTO> items) {
        this.customerId = customerId;
        this.warehouseId = warehouseId;
        this.amountReceived = amountReceived;
        this.modeOfPayment = modeOfPayment;
        this.bankAccountId = bankAccountId;
        this.saleDate = saleDate;
        this.items = items;
    }

    public static class SaleItemRequestDTO {
        @NotNull(message = "Variant ID cannot be null")
        @Positive(message = "Variant ID must be a positive number")
        private Long variantId;

        @NotNull(message = "Quantity issued cannot be null")
        @Positive(message = "Quantity issued must be greater than 0")
        private Long qtyIssued;

        @NotNull(message = "Quantity empty received cannot be null")
        @PositiveOrZero(message = "Quantity empty received cannot be negative")
        private Long qtyEmptyReceived;

        @PositiveOrZero(message = "Discount cannot be negative")
        @DecimalMin(value = "0.00", message = "Discount must be >= 0")
        private BigDecimal discount;

        public SaleItemRequestDTO() {
        }

        public SaleItemRequestDTO(Long variantId, Long qtyIssued, Long qtyEmptyReceived, BigDecimal discount) {
            this.variantId = variantId;
            this.qtyIssued = qtyIssued;
            this.qtyEmptyReceived = qtyEmptyReceived;
            this.discount = discount;
        }

        public Long getVariantId() {
            return variantId;
        }

        public void setVariantId(Long variantId) {
            this.variantId = variantId;
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

        public BigDecimal getDiscount() {
            return discount;
        }

        public void setDiscount(BigDecimal discount) {
            this.discount = discount;
        }
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public BigDecimal getAmountReceived() {
        return amountReceived;
    }

    public void setAmountReceived(BigDecimal amountReceived) {
        this.amountReceived = amountReceived;
    }

    public String getModeOfPayment() {
        return modeOfPayment;
    }

    public void setModeOfPayment(String modeOfPayment) {
        this.modeOfPayment = modeOfPayment;
    }

    public List<SaleItemRequestDTO> getItems() {
        return items;
    }

    public void setItems(List<SaleItemRequestDTO> items) {
        this.items = items;
    }

    public Long getBankAccountId() {
        return bankAccountId;
    }

    public void setBankAccountId(Long bankAccountId) {
        this.bankAccountId = bankAccountId;
    }

    public LocalDate getSaleDate() {
        return saleDate;
    }

    public void setSaleDate(LocalDate saleDate) {
        this.saleDate = saleDate;
    }
}



