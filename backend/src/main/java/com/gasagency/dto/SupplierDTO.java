package com.gasagency.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

public class SupplierDTO {
    private Long id;

    @NotBlank(message = "Supplier name cannot be null or blank")
    @Size(min = 2, max = 100, message = "Supplier name must be between 2 and 100 characters")
    private String name;

    @NotBlank(message = "Contact cannot be null or blank")
    @Size(min = 5, max = 50, message = "Contact must be between 5 and 50 characters")
    @Pattern(regexp = "^[+0-9\\-\\s()]+$", message = "Contact must contain only valid phone number characters")
    private String contact;

    public SupplierDTO() {
    }

    public SupplierDTO(Long id, String name, String contact) {
        this.id = id;
        this.name = name;
        this.contact = contact;
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

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }
}
