package com.gasagency.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;

public class CustomerDTO {
    private Long id;

    @NotBlank(message = "Customer name cannot be blank")
    @Size(min = 2, max = 100, message = "Customer name must be between 2 and 100 characters")
    private String name;

    @NotBlank(message = "Mobile number cannot be blank")
    @Pattern(regexp = "^[+]?[(]?[0-9]{1,3}[)]?[-\\s.]?[(]?[0-9]{1,4}[)]?[-\\s.]?[0-9]{1,4}[-\\s.]?[0-9]{1,9}$", message = "Invalid mobile number format")
    private String mobile;

    @Size(max = 500, message = "Address must not exceed 500 characters")
    private String address;

    private Boolean active;

    @DecimalMin(value = "0.0", message = "Sale price must be non-negative")
    private BigDecimal salePrice;

    @DecimalMin(value = "0.0", message = "Discount price must be non-negative")
    private BigDecimal discountPrice;

    private List<Long> configuredVariants; // Array of variant IDs configured for this customer

    private LocalDate lastSaleDate;

    private Long totalPending;

    public CustomerDTO() {
    }

    public CustomerDTO(Long id, String name, String mobile, String address, Boolean active) {
        this.id = id;
        this.name = name;
        this.mobile = mobile;
        this.address = address;
        this.active = active;
        this.lastSaleDate = null;
        this.totalPending = 0L;
    }

    public CustomerDTO(Long id, String name, String mobile, String address, Boolean active, LocalDate lastSaleDate,
            Long totalPending) {
        this.id = id;
        this.name = name;
        this.mobile = mobile;
        this.address = address;
        this.active = active;
        this.lastSaleDate = lastSaleDate;
        this.totalPending = totalPending;
    }

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

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public LocalDate getLastSaleDate() {
        return lastSaleDate;
    }

    public void setLastSaleDate(LocalDate lastSaleDate) {
        this.lastSaleDate = lastSaleDate;
    }

    public Long getTotalPending() {
        return totalPending;
    }

    public void setTotalPending(Long totalPending) {
        this.totalPending = totalPending;
    }

    public BigDecimal getSalePrice() {
        return salePrice;
    }

    public void setSalePrice(BigDecimal salePrice) {
        this.salePrice = salePrice;
    }

    public BigDecimal getDiscountPrice() {
        return discountPrice;
    }

    public void setDiscountPrice(BigDecimal discountPrice) {
        this.discountPrice = discountPrice;
    }

    public List<Long> getConfiguredVariants() {
        return configuredVariants;
    }

    public void setConfiguredVariants(List<Long> configuredVariants) {
        this.configuredVariants = configuredVariants;
    }
}
