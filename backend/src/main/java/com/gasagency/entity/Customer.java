package com.gasagency.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import java.util.ArrayList;
import java.util.List;

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

    @DecimalMin(value = "0.0", message = "Sale price must be non-negative.")
    @Column(nullable = true)
    private BigDecimal salePrice;

    @DecimalMin(value = "0.0", message = "Discount price must be non-negative.")
    @Column(nullable = true)
    private BigDecimal discountPrice;

    @Size(max = 50, message = "GST number must be at most 50 characters.")
    @Pattern(regexp = "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[0-9]{1}[0-9A-Z]{2}$", message = "GST number must be in valid Indian GST format (15 characters).")
    @Column(nullable = true)
    private String gstNo;

    @Column(columnDefinition = "TEXT", nullable = true)
    private String configuredVariants; // JSON array of variant IDs configured for this customer

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

    public String getConfiguredVariants() {
        return configuredVariants;
    }

    public void setConfiguredVariants(String configuredVariants) {
        this.configuredVariants = configuredVariants;
    }

    public String getGstNo() {
        return gstNo;
    }

    public void setGstNo(String gstNo) {
        this.gstNo = gstNo;
    }

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JsonManagedReference("customer-sales")
    private List<Sale> sales = new ArrayList<>();

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JsonManagedReference("customer-variantPrices")
    private List<CustomerVariantPrice> variantPrices = new ArrayList<>();

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JsonManagedReference("customer-ledgers")
    private List<CustomerCylinderLedger> ledgerEntries = new ArrayList<>();

    public List<Sale> getSales() {
        return sales;
    }

    public void setSales(List<Sale> sales) {
        this.sales = sales;
    }

    public List<CustomerVariantPrice> getVariantPrices() {
        return variantPrices;
    }

    public void setVariantPrices(List<CustomerVariantPrice> variantPrices) {
        this.variantPrices = variantPrices;
    }

    public List<CustomerCylinderLedger> getLedgerEntries() {
        return ledgerEntries;
    }

    public void setLedgerEntries(List<CustomerCylinderLedger> ledgerEntries) {
        this.ledgerEntries = ledgerEntries;
    }
}
