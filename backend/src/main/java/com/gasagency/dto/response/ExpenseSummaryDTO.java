package com.gasagency.dto.response;

import java.math.BigDecimal;

public class ExpenseSummaryDTO {
    private BigDecimal totalAmount;
    private Integer transactionCount;
    private BigDecimal avgExpenseValue;
    private String topCategory;

    public ExpenseSummaryDTO() {
    }

    public ExpenseSummaryDTO(BigDecimal totalAmount, Integer transactionCount, BigDecimal avgExpenseValue,
            String topCategory) {
        this.totalAmount = totalAmount;
        this.transactionCount = transactionCount;
        this.avgExpenseValue = avgExpenseValue;
        this.topCategory = topCategory;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Integer getTransactionCount() {
        return transactionCount;
    }

    public void setTransactionCount(Integer transactionCount) {
        this.transactionCount = transactionCount;
    }

    public BigDecimal getAvgExpenseValue() {
        return avgExpenseValue;
    }

    public void setAvgExpenseValue(BigDecimal avgExpenseValue) {
        this.avgExpenseValue = avgExpenseValue;
    }

    public String getTopCategory() {
        return topCategory;
    }

    public void setTopCategory(String topCategory) {
        this.topCategory = topCategory;
    }
}



