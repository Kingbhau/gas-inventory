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
    private List<SaleItemDTO> saleItems;

    public SaleDTO() {
    }

    public SaleDTO(Long id, Long customerId, String customerName, LocalDate saleDate, BigDecimal totalAmount,
            List<SaleItemDTO> saleItems) {
        this.id = id;
        this.customerId = customerId;
        this.customerName = customerName;
        this.saleDate = saleDate;
        this.totalAmount = totalAmount;
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
}
