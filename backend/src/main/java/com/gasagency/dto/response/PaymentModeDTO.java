package com.gasagency.dto.response;

import java.time.LocalDateTime;

public class PaymentModeDTO {

    private Long id;
    private String name;
    private String code;
    private String description;
    private Boolean isActive;
    private Boolean isBankAccountRequired;
    private LocalDateTime createdDate;
    private String createdBy;
    private LocalDateTime updatedDate;
    private String updatedBy;

    // Constructors
    public PaymentModeDTO() {
    }

    public PaymentModeDTO(Long id, String name, String code, String description, Boolean isActive,
            Boolean isBankAccountRequired,
            LocalDateTime createdDate, String createdBy,
            LocalDateTime updatedDate, String updatedBy) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.description = description;
        this.isActive = isActive;
        this.isBankAccountRequired = isBankAccountRequired;
        this.createdDate = createdDate;
        this.createdBy = createdBy;
        this.updatedDate = updatedDate;
        this.updatedBy = updatedBy;
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

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(LocalDateTime updatedDate) {
        this.updatedDate = updatedDate;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
}



