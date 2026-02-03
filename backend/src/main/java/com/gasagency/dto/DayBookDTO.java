package com.gasagency.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class DayBookDTO {
    private Long id;
    private Long customerId;
    private LocalDate transactionDate;
    private String customerName;
    private String warehouseName;
    private String variantName;
    private Long filledCount;
    private Long emptyCount;
    private BigDecimal totalAmount;
    private BigDecimal amountReceived;
    private BigDecimal dueAmount;
    private String paymentMode;
    private String transactionType; // "SALE" or "EMPTY_RETURN"

    public DayBookDTO() {
    }

    public DayBookDTO(Long id, Long customerId, LocalDate transactionDate, String customerName, String warehouseName,
            String variantName, Long filledCount, Long emptyCount, BigDecimal totalAmount,
            BigDecimal amountReceived, BigDecimal dueAmount, String paymentMode,
            String transactionType) {
        this.id = id;
        this.customerId = customerId;
        this.transactionDate = transactionDate;
        this.customerName = customerName;
        this.warehouseName = warehouseName;
        this.variantName = variantName;
        this.filledCount = filledCount;
        this.emptyCount = emptyCount;
        this.totalAmount = totalAmount;
        this.amountReceived = amountReceived;
        this.dueAmount = dueAmount;
        this.paymentMode = paymentMode;
        this.transactionType = transactionType;
    }

    // Getters and Setters
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

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getWarehouseName() {
        return warehouseName;
    }

    public void setWarehouseName(String warehouseName) {
        this.warehouseName = warehouseName;
    }

    public String getVariantName() {
        return variantName;
    }

    public void setVariantName(String variantName) {
        this.variantName = variantName;
    }

    public Long getFilledCount() {
        return filledCount;
    }

    public void setFilledCount(Long filledCount) {
        this.filledCount = filledCount;
    }

    public Long getEmptyCount() {
        return emptyCount;
    }

    public void setEmptyCount(Long emptyCount) {
        this.emptyCount = emptyCount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getAmountReceived() {
        return amountReceived;
    }

    public void setAmountReceived(BigDecimal amountReceived) {
        this.amountReceived = amountReceived;
    }

    public BigDecimal getDueAmount() {
        return dueAmount;
    }

    public void setDueAmount(BigDecimal dueAmount) {
        this.dueAmount = dueAmount;
    }

    public String getPaymentMode() {
        return paymentMode;
    }

    public void setPaymentMode(String paymentMode) {
        this.paymentMode = paymentMode;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }
}
