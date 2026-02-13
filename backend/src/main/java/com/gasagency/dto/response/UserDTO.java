package com.gasagency.dto.response;

public class UserDTO {
    private Long businessId;
    private Long id;
    private String name;
    private String mobileNo;
    private String role;
    private boolean active;
    // address field removed
    private String username; // Added username field
    private java.time.LocalDateTime createdDate;
    private String createdBy;
    private java.time.LocalDateTime updatedDate;
    private String updatedBy;
    // Getters and setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    } // Added getter for username

    public void setUsername(String username) {
        this.username = username;
    } // Added setter for username

    public String getMobileNo() {
        return mobileNo;
    }

    public void setMobileNo(String mobileNo) {
        this.mobileNo = mobileNo;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Long getBusinessId() {
        return businessId;
    }

    public void setBusinessId(Long businessId) {
        this.businessId = businessId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public java.time.LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(java.time.LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public java.time.LocalDateTime getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(java.time.LocalDateTime updatedDate) {
        this.updatedDate = updatedDate;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    // address getter/setter removed
}



