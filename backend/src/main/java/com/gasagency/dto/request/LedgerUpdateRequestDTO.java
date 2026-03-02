package com.gasagency.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class LedgerUpdateRequestDTO {
    private Long filledOut;
    private Long emptyIn;
    private BigDecimal totalAmount;
    private BigDecimal amountReceived;
    private LocalDate transactionDate;
    private String updateReason;
    private String paymentMode;
    private Long bankAccountId;
    private List<PaymentSplitUpdateDTO> paymentSplits;

    public static class PaymentSplitUpdateDTO {
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

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
    }

    public String getUpdateReason() {
        return updateReason;
    }

    public void setUpdateReason(String updateReason) {
        this.updateReason = updateReason;
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

    public List<PaymentSplitUpdateDTO> getPaymentSplits() {
        return paymentSplits;
    }

    public void setPaymentSplits(List<PaymentSplitUpdateDTO> paymentSplits) {
        this.paymentSplits = paymentSplits;
    }
}



