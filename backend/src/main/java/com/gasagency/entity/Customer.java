package com.gasagency.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "customer", uniqueConstraints = { @UniqueConstraint(columnNames = { "mobile" }) }, indexes = {
        @Index(name = "idx_customer_mobile", columnList = "mobile"),
        @Index(name = "idx_customer_active", columnList = "active")
})
public class Customer extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @NotBlank(message = "Customer name is required.")
    @Size(max = 100, message = "Customer name must be at most 100 characters.")
    @Column(nullable = false)
    private String name;

    @NotBlank(message = "Mobile number is required.")
    @Pattern(regexp = "^[6-9][0-9]{9}$", message = "Mobile number must be a valid 10-digit Indian number.")
    @Column(nullable = false, unique = true)
    private String mobile;

    @Size(max = 255, message = "Address must be at most 255 characters.")
    @Column(columnDefinition = "TEXT")
    private String address;

    @NotNull(message = "Active status is required.")
    @Column(nullable = false)
    private Boolean active = true;

    public Customer() {
    }

    public Customer(String name, String mobile, String address) {
        this.name = name;
        this.mobile = mobile;
        this.address = address;
        this.active = true;
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
}
