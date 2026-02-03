package com.gasagency.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonBackReference;

@Entity
@Table(name = "expenses", indexes = {
        @Index(name = "idx_expense_date", columnList = "expense_date"),
        @Index(name = "idx_expense_category_date", columnList = "category_id, expense_date")
})
public class Expense extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    @JsonBackReference("category-expenses")
    private ExpenseCategory category;

    @Column(nullable = false)
    private LocalDate expenseDate;

    @Column(length = 500)
    private String notes;

    @Column(length = 50)
    private String paymentMode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_account_id")
    @JsonBackReference("bankAccount-expenses")
    private BankAccount bankAccount;

    // Constructors
    public Expense() {
    }

    public Expense(Long id, String description, BigDecimal amount, ExpenseCategory category,
            LocalDate expenseDate, String notes, String paymentMode, BankAccount bankAccount) {
        this.id = id;
        this.description = description;
        this.amount = amount;
        this.category = category;
        this.expenseDate = expenseDate;
        this.notes = notes;
        this.paymentMode = paymentMode;
        this.bankAccount = bankAccount;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public ExpenseCategory getCategory() {
        return category;
    }

    public void setCategory(ExpenseCategory category) {
        this.category = category;
    }

    public LocalDate getExpenseDate() {
        return expenseDate;
    }

    public void setExpenseDate(LocalDate expenseDate) {
        this.expenseDate = expenseDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getPaymentMode() {
        return paymentMode;
    }

    public void setPaymentMode(String paymentMode) {
        this.paymentMode = paymentMode;
    }

    public BankAccount getBankAccount() {
        return bankAccount;
    }

    public void setBankAccount(BankAccount bankAccount) {
        this.bankAccount = bankAccount;
    }
}
