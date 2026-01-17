package com.gasagency.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class SaleDTO {
    private Long id;
    private Long customerId;
    private String customerName;
    private LocalDate saleDate;
    private BigDecimal totalAmount;
    private String paymentMode;
    private Long bankAccountId;
    private String bankAccountName;
    private List<SaleItemDTO> saleItems;

    public SaleDTO() {
    }

    public SaleDTO(Long id, Long customerId, String customerName, LocalDate saleDate, BigDecimal totalAmount,
            String paymentMode, Long bankAccountId, String bankAccountName, List<SaleItemDTO> saleItems) {
        this.id = id;
        this.customerId = customerId;
        this.customerName = customerName;
        this.saleDate = saleDate;
        this.totalAmount = totalAmount;
        this.paymentMode = paymentMode;
        this.bankAccountId = bankAccountId;
        this.bankAccountName = bankAccountName;
        this.saleItems = saleItems;
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

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public LocalDate getSaleDate() {
        return saleDate;
    }

    public void setSaleDate(LocalDate saleDate) {
        this.saleDate = saleDate;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public List<SaleItemDTO> getSaleItems() {
        return saleItems;
    }

    public void setSaleItems(List<SaleItemDTO> saleItems) {
        this.saleItems = saleItems;
    }

    public String getPaymentMode() {
        return paymentMode;
    }

    public void setPaymentMode(String paymentMode) {
        this.paymentMode = paymentMode;
    }

    public Long getBankAccountId() {
        return bankAccountId;
    }

    public void setBankAccountId(Long bankAccountId) {
        this.bankAccountId = bankAccountId;
    }

    public String getBankAccountName() {
        return bankAccountName;
    }

    public void setBankAccountName(String bankAccountName) {
        this.bankAccountName = bankAccountName;
    }
}
