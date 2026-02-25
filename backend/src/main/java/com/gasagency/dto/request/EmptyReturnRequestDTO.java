package com.gasagency.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class EmptyReturnRequestDTO {
    private Long customerId;
    private Long warehouseId;
    private Long variantId;
    private LocalDate transactionDate;
    private Long emptyIn;
    private BigDecimal amountReceived;
    private String paymentType;
    private String paymentMode;
    private Long bankAccountId;
    private List<PaymentSplitRequestDTO> paymentSplits;

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public Long getVariantId() {
        return variantId;
    }

    public void setVariantId(Long variantId) {
        this.variantId = variantId;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
    }

    public Long getEmptyIn() {
        return emptyIn;
    }

    public void setEmptyIn(Long emptyIn) {
        this.emptyIn = emptyIn;
    }

    public BigDecimal getAmountReceived() {
        return amountReceived;
    }

    public void setAmountReceived(BigDecimal amountReceived) {
        this.amountReceived = amountReceived;
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

    public String getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(String paymentType) {
        this.paymentType = paymentType;
    }

    public List<PaymentSplitRequestDTO> getPaymentSplits() {
        return paymentSplits;
    }

    public void setPaymentSplits(List<PaymentSplitRequestDTO> paymentSplits) {
        this.paymentSplits = paymentSplits;
    }

    public static class PaymentSplitRequestDTO {
        private String modeOfPayment;
        private BigDecimal amount;
        private Long bankAccountId;
        private String note;

        public String getModeOfPayment() {
            return modeOfPayment;
        }

        public void setModeOfPayment(String modeOfPayment) {
            this.modeOfPayment = modeOfPayment;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public Long getBankAccountId() {
            return bankAccountId;
        }

        public void setBankAccountId(Long bankAccountId) {
            this.bankAccountId = bankAccountId;
        }

        public String getNote() {
            return note;
        }

        public void setNote(String note) {
            this.note = note;
        }
    }
}



