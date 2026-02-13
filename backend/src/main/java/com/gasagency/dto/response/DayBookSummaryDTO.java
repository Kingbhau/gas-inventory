package com.gasagency.dto.response;

import java.math.BigDecimal;
import java.util.List;

public class DayBookSummaryDTO {
    private List<DayBookDTO> transactions;
    private Long totalFilledCount;
    private Long totalEmptyCount;
    private BigDecimal totalAmount;
    private BigDecimal totalAmountReceived;
    private BigDecimal totalDueAmount;
    private Integer totalTransactions;

    public DayBookSummaryDTO() {
    }

    public DayBookSummaryDTO(List<DayBookDTO> transactions, Long totalFilledCount, Long totalEmptyCount,
            BigDecimal totalAmount, BigDecimal totalAmountReceived, BigDecimal totalDueAmount,
            Integer totalTransactions) {
        this.transactions = transactions;
        this.totalFilledCount = totalFilledCount;
        this.totalEmptyCount = totalEmptyCount;
        this.totalAmount = totalAmount;
        this.totalAmountReceived = totalAmountReceived;
        this.totalDueAmount = totalDueAmount;
        this.totalTransactions = totalTransactions;
    }

    // Getters and Setters
    public List<DayBookDTO> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<DayBookDTO> transactions) {
        this.transactions = transactions;
    }

    public Long getTotalFilledCount() {
        return totalFilledCount;
    }

    public void setTotalFilledCount(Long totalFilledCount) {
        this.totalFilledCount = totalFilledCount;
    }

    public Long getTotalEmptyCount() {
        return totalEmptyCount;
    }

    public void setTotalEmptyCount(Long totalEmptyCount) {
        this.totalEmptyCount = totalEmptyCount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getTotalAmountReceived() {
        return totalAmountReceived;
    }

    public void setTotalAmountReceived(BigDecimal totalAmountReceived) {
        this.totalAmountReceived = totalAmountReceived;
    }

    public BigDecimal getTotalDueAmount() {
        return totalDueAmount;
    }

    public void setTotalDueAmount(BigDecimal totalDueAmount) {
        this.totalDueAmount = totalDueAmount;
    }

    public Integer getTotalTransactions() {
        return totalTransactions;
    }

    public void setTotalTransactions(Integer totalTransactions) {
        this.totalTransactions = totalTransactions;
    }
}



