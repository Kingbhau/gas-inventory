package com.gasagency.dto.request;

import jakarta.validation.constraints.NotBlank;

public class CreateBankAccountRequestDTO {
    @NotBlank(message = "Bank name is required.")
    private String bankName;

    @NotBlank(message = "Account number is required.")
    private String accountNumber;

    @NotBlank(message = "Account holder name is required.")
    private String accountHolderName;

    private String accountName;

    private String accountType;

    private Boolean isActive;

    public CreateBankAccountRequestDTO() {
    }

    public CreateBankAccountRequestDTO(String bankName, String accountNumber, String accountHolderName,
            String accountName, String accountType) {
        this.bankName = bankName;
        this.accountNumber = accountNumber;
        this.accountHolderName = accountHolderName;
        this.accountName = accountName;
        this.accountType = accountType;
    }

    public CreateBankAccountRequestDTO(String bankName, String accountNumber, String accountHolderName,
            String accountName, String accountType, Boolean isActive) {
        this.bankName = bankName;
        this.accountNumber = accountNumber;
        this.accountHolderName = accountHolderName;
        this.accountName = accountName;
        this.accountType = accountType;
        this.isActive = isActive;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getAccountHolderName() {
        return accountHolderName;
    }

    public void setAccountHolderName(String accountHolderName) {
        this.accountHolderName = accountHolderName;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}



