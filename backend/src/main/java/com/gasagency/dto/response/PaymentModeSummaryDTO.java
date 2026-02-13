package com.gasagency.dto.response;

import java.util.HashMap;
import java.util.Map;

public class PaymentModeSummaryDTO {
    private Map<String, PaymentModeStats> paymentModeStats = new HashMap<>();
    private double totalAmount;
    private int totalTransactions;

    public PaymentModeSummaryDTO() {
    }

    // Inner class for individual payment mode statistics
    public static class PaymentModeStats {
        private String paymentModeName;
        private String paymentModeCode;
        private double totalAmount;
        private int transactionCount;

        public PaymentModeStats() {
        }

        public PaymentModeStats(String paymentModeName, String paymentModeCode, double totalAmount,
                int transactionCount) {
            this.paymentModeName = paymentModeName;
            this.paymentModeCode = paymentModeCode;
            this.totalAmount = totalAmount;
            this.transactionCount = transactionCount;
        }

        public String getPaymentModeName() {
            return paymentModeName;
        }

        public void setPaymentModeName(String paymentModeName) {
            this.paymentModeName = paymentModeName;
        }

        public String getPaymentModeCode() {
            return paymentModeCode;
        }

        public void setPaymentModeCode(String paymentModeCode) {
            this.paymentModeCode = paymentModeCode;
        }

        public double getTotalAmount() {
            return totalAmount;
        }

        public void setTotalAmount(double totalAmount) {
            this.totalAmount = totalAmount;
        }

        public int getTransactionCount() {
            return transactionCount;
        }

        public void setTransactionCount(int transactionCount) {
            this.transactionCount = transactionCount;
        }
    }

    public Map<String, PaymentModeStats> getPaymentModeStats() {
        return paymentModeStats;
    }

    public void setPaymentModeStats(Map<String, PaymentModeStats> paymentModeStats) {
        this.paymentModeStats = paymentModeStats;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public int getTotalTransactions() {
        return totalTransactions;
    }

    public void setTotalTransactions(int totalTransactions) {
        this.totalTransactions = totalTransactions;
    }
}



