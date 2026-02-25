package com.gasagency.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class SaleDTO {
    private Long id;
    private String referenceNumber;
    private Long customerId;
    private String customerName;
    private Long warehouseId;
    private LocalDate saleDate;
    private BigDecimal totalAmount;
    private BigDecimal amountReceived;
    private String paymentMode;
    private Long bankAccountId;
    private String bankAccountName;
    private List<SalePaymentSplitDTO> paymentSplits;
    private List<SaleItemDTO> saleItems;
    private String createdBy;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
    private String updatedBy;

    public SaleDTO() {
    }

    public SaleDTO(Long id, String referenceNumber, Long customerId, String customerName, Long warehouseId,
            LocalDate saleDate, BigDecimal totalAmount, String paymentMode, Long bankAccountId,
            String bankAccountName, List<SaleItemDTO> saleItems, List<SalePaymentSplitDTO> paymentSplits) {
        this.id = id;
        this.referenceNumber = referenceNumber;
        this.customerId = customerId;
        this.customerName = customerName;
        this.warehouseId = warehouseId;
        this.saleDate = saleDate;
        this.totalAmount = totalAmount;
        this.paymentMode = paymentMode;
        this.bankAccountId = bankAccountId;
        this.bankAccountName = bankAccountName;
        this.saleItems = saleItems;
        this.paymentSplits = paymentSplits;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
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

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
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

    public BigDecimal getAmountReceived() {
        return amountReceived;
    }

    public void setAmountReceived(BigDecimal amountReceived) {
        this.amountReceived = amountReceived;
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

    public List<SalePaymentSplitDTO> getPaymentSplits() {
        return paymentSplits;
    }

    public void setPaymentSplits(List<SalePaymentSplitDTO> paymentSplits) {
        this.paymentSplits = paymentSplits;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDateTime getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(LocalDateTime updatedDate) {
        this.updatedDate = updatedDate;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

}



