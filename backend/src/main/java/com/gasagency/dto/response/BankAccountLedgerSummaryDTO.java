package com.gasagency.dto.response;

import java.math.BigDecimal;
import java.util.List;

public class BankAccountLedgerSummaryDTO {
    private BigDecimal totalDeposits;
    private BigDecimal totalWithdrawals;
    private BigDecimal netBalance;
    private BigDecimal balanceAfter;
    private Integer transactionCount;
    private List<BankAccountBalanceDTO> bankwiseBalances;

    public BigDecimal getTotalDeposits() {
        return totalDeposits;
    }

    public void setTotalDeposits(BigDecimal totalDeposits) {
        this.totalDeposits = totalDeposits;
    }

    public BigDecimal getTotalWithdrawals() {
        return totalWithdrawals;
    }

    public void setTotalWithdrawals(BigDecimal totalWithdrawals) {
        this.totalWithdrawals = totalWithdrawals;
    }

    public BigDecimal getNetBalance() {
        return netBalance;
    }

    public void setNetBalance(BigDecimal netBalance) {
        this.netBalance = netBalance;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(BigDecimal balanceAfter) {
        this.balanceAfter = balanceAfter;
    }

    public Integer getTransactionCount() {
        return transactionCount;
    }

    public void setTransactionCount(Integer transactionCount) {
        this.transactionCount = transactionCount;
    }

    public List<BankAccountBalanceDTO> getBankwiseBalances() {
        return bankwiseBalances;
    }

    public void setBankwiseBalances(List<BankAccountBalanceDTO> bankwiseBalances) {
        this.bankwiseBalances = bankwiseBalances;
    }
}



