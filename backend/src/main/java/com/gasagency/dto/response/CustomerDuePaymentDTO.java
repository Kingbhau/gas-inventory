package com.gasagency.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public class CustomerDuePaymentDTO {
    private Long customerId;
    private String customerName;
    private String customerPhone;
    private String customerAddress;
    private BigDecimal totalSalesAmount;
    private BigDecimal amountReceived;
    private BigDecimal dueAmount;
    private LocalDate lastTransactionDate;
    private Long transactionCount;

    public CustomerDuePaymentDTO() {
    }

    public CustomerDuePaymentDTO(Long customerId, String customerName, String customerPhone,
            String customerAddress, BigDecimal totalSalesAmount, BigDecimal amountReceived,
            BigDecimal dueAmount, LocalDate lastTransactionDate, Long transactionCount) {
        this.customerId = customerId;
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.customerAddress = customerAddress;
        this.totalSalesAmount = totalSalesAmount;
        this.amountReceived = amountReceived;
        this.dueAmount = dueAmount;
        this.lastTransactionDate = lastTransactionDate;
        this.transactionCount = transactionCount;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public String getCustomerAddress() {
        return customerAddress;
    }

    public void setCustomerAddress(String customerAddress) {
        this.customerAddress = customerAddress;
    }

    public BigDecimal getTotalSalesAmount() {
        return totalSalesAmount;
    }

    public void setTotalSalesAmount(BigDecimal totalSalesAmount) {
        this.totalSalesAmount = totalSalesAmount;
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

    public LocalDate getLastTransactionDate() {
        return lastTransactionDate;
    }

    public void setLastTransactionDate(LocalDate lastTransactionDate) {
        this.lastTransactionDate = lastTransactionDate;
    }

    public Long getTransactionCount() {
        return transactionCount;
    }

    public void setTransactionCount(Long transactionCount) {
        this.transactionCount = transactionCount;
    }
}



