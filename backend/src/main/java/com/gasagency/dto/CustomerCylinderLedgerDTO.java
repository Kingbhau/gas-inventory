package com.gasagency.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class CustomerCylinderLedgerDTO {
    private Long id;
    private Long customerId;
    private String customerName;
    private Long variantId;
    private String variantName;
    private LocalDate transactionDate;
    private LocalDateTime createdAt;
    private String refType;
    private Long refId;
    private Long filledOut;
    private Long emptyIn;
    private Long balance;
    private Long fromWarehouseId;
    private String fromWarehouseName;
    private Long toWarehouseId;
    private String toWarehouseName;

    public CustomerCylinderLedgerDTO() {
    }

    public CustomerCylinderLedgerDTO(Long id, Long customerId, String customerName, Long variantId, String variantName,
            LocalDate transactionDate, String refType, Long refId, Long filledOut, Long emptyIn, Long balance) {
        this.id = id;
        this.customerId = customerId;
        this.customerName = customerName;
        this.variantId = variantId;
        this.variantName = variantName;
        this.transactionDate = transactionDate;
        this.refType = refType;
        this.refId = refId;
        this.filledOut = filledOut;
        this.emptyIn = emptyIn;
        this.balance = balance;
    }

    public CustomerCylinderLedgerDTO(Long id, Long customerId, String customerName, Long variantId, String variantName,
            LocalDate transactionDate, String refType, Long refId, Long filledOut, Long emptyIn, Long balance,
            Long fromWarehouseId, String fromWarehouseName, Long toWarehouseId, String toWarehouseName) {
        this.id = id;
        this.customerId = customerId;
        this.customerName = customerName;
        this.variantId = variantId;
        this.variantName = variantName;
        this.transactionDate = transactionDate;
        this.refType = refType;
        this.refId = refId;
        this.filledOut = filledOut;
        this.emptyIn = emptyIn;
        this.balance = balance;
        this.fromWarehouseId = fromWarehouseId;
        this.fromWarehouseName = fromWarehouseName;
        this.toWarehouseId = toWarehouseId;
        this.toWarehouseName = toWarehouseName;
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

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getRefType() {
        return refType;
    }

    public void setRefType(String refType) {
        this.refType = refType;
    }

    public Long getRefId() {
        return refId;
    }

    public void setRefId(Long refId) {
        this.refId = refId;
    }

    public Long getFilledOut() {
        return filledOut;
    }

    public void setFilledOut(Long filledOut) {
        this.filledOut = filledOut;
    }

    public Long getEmptyIn() {
        return emptyIn;
    }

    public void setEmptyIn(Long emptyIn) {
        this.emptyIn = emptyIn;
    }

    public Long getBalance() {
        return balance;
    }

    public void setBalance(Long balance) {
        this.balance = balance;
    }

    public Long getFromWarehouseId() {
        return fromWarehouseId;
    }

    public void setFromWarehouseId(Long fromWarehouseId) {
        this.fromWarehouseId = fromWarehouseId;
    }

    public String getFromWarehouseName() {
        return fromWarehouseName;
    }

    public void setFromWarehouseName(String fromWarehouseName) {
        this.fromWarehouseName = fromWarehouseName;
    }

    public Long getToWarehouseId() {
        return toWarehouseId;
    }

    public void setToWarehouseId(Long toWarehouseId) {
        this.toWarehouseId = toWarehouseId;
    }

    public String getToWarehouseName() {
        return toWarehouseName;
    }

    public void setToWarehouseName(String toWarehouseName) {
        this.toWarehouseName = toWarehouseName;
    }
}
