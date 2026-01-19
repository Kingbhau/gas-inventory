package com.gasagency.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonBackReference;

@Entity
@Table(name = "monthly_price", indexes = {
        @Index(name = "idx_monthlyprice_variant_id", columnList = "variant_id"),
        @Index(name = "idx_monthlyprice_month_year", columnList = "monthYear")
})
public class MonthlyPrice extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Variant is required.")
    @ManyToOne
    @JoinColumn(name = "variant_id", nullable = false)
    @JsonBackReference("variant-monthlyPrices")
    private CylinderVariant variant;

    @NotNull(message = "Month/Year is required.")
    @Column(nullable = false)
    private LocalDate monthYear;

    @NotNull(message = "Base price is required.")
    @DecimalMin(value = "0.0", inclusive = false, message = "Base price must be positive.")
    @Column(nullable = false)
    private BigDecimal basePrice;

    @Column(nullable = false, updatable = false)
    private LocalDate createdAt = LocalDate.now();

    public MonthlyPrice() {
    }

    public MonthlyPrice(CylinderVariant variant, LocalDate monthYear, BigDecimal basePrice) {
        this.variant = variant;
        this.monthYear = monthYear;
        this.basePrice = basePrice;
        this.createdAt = LocalDate.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CylinderVariant getVariant() {
        return variant;
    }

    public void setVariant(CylinderVariant variant) {
        this.variant = variant;
    }

    public LocalDate getMonthYear() {
        return monthYear;
    }

    public void setMonthYear(LocalDate monthYear) {
        this.monthYear = monthYear;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }

    public LocalDate getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDate createdAt) {
        this.createdAt = createdAt;
    }
}
