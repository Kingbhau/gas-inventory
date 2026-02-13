package com.gasagency.dto.response;

public class SaleSummaryDTO {
    private double totalSalesAmount;
    private int transactionCount;
    private double avgSaleValue;
    private String topCustomer;

    public SaleSummaryDTO() {
    }

    public SaleSummaryDTO(double totalSalesAmount, int transactionCount, double avgSaleValue, String topCustomer) {
        this.totalSalesAmount = totalSalesAmount;
        this.transactionCount = transactionCount;
        this.avgSaleValue = avgSaleValue;
        this.topCustomer = topCustomer;
    }

    public double getTotalSalesAmount() {
        return totalSalesAmount;
    }

    public void setTotalSalesAmount(double totalSalesAmount) {
        this.totalSalesAmount = totalSalesAmount;
    }

    public int getTransactionCount() {
        return transactionCount;
    }

    public void setTransactionCount(int transactionCount) {
        this.transactionCount = transactionCount;
    }

    public double getAvgSaleValue() {
        return avgSaleValue;
    }

    public void setAvgSaleValue(double avgSaleValue) {
        this.avgSaleValue = avgSaleValue;
    }

    public String getTopCustomer() {
        return topCustomer;
    }

    public void setTopCustomer(String topCustomer) {
        this.topCustomer = topCustomer;
    }
}



