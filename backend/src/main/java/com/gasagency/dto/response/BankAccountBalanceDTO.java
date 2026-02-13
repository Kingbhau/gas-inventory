package com.gasagency.dto.response;

import java.math.BigDecimal;

public class BankAccountBalanceDTO {
    private String bankName;
    private BigDecimal balance;

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
}



