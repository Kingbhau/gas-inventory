package com.gasagency.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonBackReference;

@Entity
@Table(name = "supplier", uniqueConstraints = { @UniqueConstraint(columnNames = { "name" }) }, indexes = {
        @Index(name = "idx_supplier_name", columnList = "name"),
        @Index(name = "idx_supplier_contact", columnList = "contact")
})
public class Supplier extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Supplier name is required.")
    @Size(max = 100, message = "Supplier name must be at most 100 characters.")
    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false, unique = true, updatable = false, length = 20)
    private String code; // Auto-generated: SUP001, SUP002, etc. (Read-only)

    @NotBlank(message = "Contact is required.")
    @Size(max = 20, message = "Contact must be at most 20 characters.")
    @Column(nullable = false)
    private String contact;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false)
    @JsonBackReference("business-suppliers")
    private BusinessInfo business;

    public Supplier() {
    }

    public Supplier(String name, String contact) {
        this.name = name;
        this.contact = contact;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
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

    public BusinessInfo getBusiness() {
        return business;
    }

    public void setBusiness(BusinessInfo business) {
        this.business = business;
    }
}

