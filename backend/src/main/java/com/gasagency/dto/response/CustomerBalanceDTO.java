package com.gasagency.dto.response;

import java.util.List;

public class CustomerBalanceDTO {
    private Long customerId;
    private String customerName;
    private List<VariantBalance> variantBalances;

    public static class VariantBalance {
        private Long variantId;
        private String variantName;
        private Long balance;

        public VariantBalance() {
        }

        public VariantBalance(Long variantId, String variantName, Long balance) {
            this.variantId = variantId;
            this.variantName = variantName;
            this.balance = balance;
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

        public Long getBalance() {
            return balance;
        }

        public void setBalance(Long balance) {
            this.balance = balance;
        }
    }

    public CustomerBalanceDTO() {
    }

    public CustomerBalanceDTO(Long customerId, String customerName, List<VariantBalance> variantBalances) {
        this.customerId = customerId;
        this.customerName = customerName;
        this.variantBalances = variantBalances;
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

    public List<VariantBalance> getVariantBalances() {
        return variantBalances;
    }

    public void setVariantBalances(List<VariantBalance> variantBalances) {
        this.variantBalances = variantBalances;
    }
}



