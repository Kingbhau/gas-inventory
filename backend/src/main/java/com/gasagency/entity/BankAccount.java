package com.gasagency.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonManagedReference;

@Entity
@Table(name = "bank_account", indexes = {
        @Index(name = "idx_bank_account_active", columnList = "is_active")
})
public class BankAccount extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @NotBlank(message = "Bank code is required.")
    @Column(nullable = false, length = 20, unique = true, updatable = false)
    private String code;

    @NotBlank(message = "Bank name is required.")
    @Column(nullable = false, length = 100)
    private String bankName;

    @NotBlank(message = "Account number is required.")
    @Column(nullable = false, length = 50, unique = true)
    private String accountNumber;

    @NotBlank(message = "Account holder name is required.")
    @Column(nullable = false, length = 100)
    private String accountHolderName;

    @Column(length = 100)
    private String accountName;

    @Column(length = 50)
    private String accountType;

    @NotNull(message = "Current balance is required.")
    @DecimalMin(value = "0.0", inclusive = true, message = "Current balance cannot be negative.")
    @Column(nullable = false)
    private BigDecimal currentBalance;

    @Column(nullable = false)
    private Boolean isActive = true;

    public BankAccount() {
    }

    public BankAccount(String code, String bankName, String accountNumber, String accountHolderName,
            BigDecimal currentBalance) {
        this.code = Objects.requireNonNull(code, "Bank code cannot be null");
        this.bankName = Objects.requireNonNull(bankName, "Bank name cannot be null");
        this.accountNumber = Objects.requireNonNull(accountNumber, "Account number cannot be null");
        this.accountHolderName = Objects.requireNonNull(accountHolderName, "Account holder name cannot be null");
        this.currentBalance = Objects.requireNonNull(currentBalance, "Current balance cannot be null");

        if (currentBalance.signum() < 0) {
            throw new IllegalArgumentException("Current balance cannot be negative");
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
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

    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    public void setCurrentBalance(BigDecimal currentBalance) {
        this.currentBalance = currentBalance;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        BankAccount that = (BankAccount) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(accountNumber, that.accountNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, accountNumber);
    }

    @Override
    public String toString() {
        return "BankAccount{" +
                "id=" + id +
                ", code='" + code + '\'' +
                ", bankName='" + bankName + '\'' +
                ", accountNumber='" + accountNumber + '\'' +
                ", accountHolderName='" + accountHolderName + '\'' +
                ", currentBalance=" + currentBalance +
                ", isActive=" + isActive +
                ", createdDate=" + createdDate +
                '}';
    }

    @OneToMany(mappedBy = "bankAccount", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JsonManagedReference("bankAccount-sales")
    private List<Sale> sales = new ArrayList<>();

    @OneToMany(mappedBy = "bankAccount", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JsonManagedReference("bankAccount-ledgers")
    private List<CustomerCylinderLedger> ledgers = new ArrayList<>();

    @OneToMany(mappedBy = "bankAccount", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JsonManagedReference("bankAccount-accountLedgers")
    private List<BankAccountLedger> accountLedgers = new ArrayList<>();

    public List<Sale> getSales() {
        return sales;
    }

    public void setSales(List<Sale> sales) {
        this.sales = sales;
    }

    public List<CustomerCylinderLedger> getLedgers() {
        return ledgers;
    }

    public void setLedgers(List<CustomerCylinderLedger> ledgers) {
        this.ledgers = ledgers;
    }

    public List<BankAccountLedger> getAccountLedgers() {
        return accountLedgers;
    }

    public void setAccountLedgers(List<BankAccountLedger> accountLedgers) {
        this.accountLedgers = accountLedgers;
    }
}
