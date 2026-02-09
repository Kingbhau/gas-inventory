package com.gasagency.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "payment_modes")
public class PaymentMode extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private Boolean isActive = true;

    @Column(nullable = false)
    private Boolean isBankAccountRequired = false;

    // Constructors
    public PaymentMode() {
    }

    public PaymentMode(String name, String code, String description, Boolean isActive, Boolean isBankAccountRequired) {
        this.name = name;
        this.code = code;
        this.description = description;
        this.isActive = isActive;
        this.isBankAccountRequired = isBankAccountRequired;
    }

    public PaymentMode(Long id, String name, String code, String description, Boolean isActive,
            Boolean isBankAccountRequired) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.description = description;
        this.isActive = isActive;
        this.isBankAccountRequired = isBankAccountRequired;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Boolean getIsBankAccountRequired() {
        return isBankAccountRequired;
    }

    public void setIsBankAccountRequired(Boolean isBankAccountRequired) {
        this.isBankAccountRequired = isBankAccountRequired;
    }
}
