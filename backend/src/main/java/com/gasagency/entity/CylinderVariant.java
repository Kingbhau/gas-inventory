package com.gasagency.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;

@Entity
@Table(name = "cylinder_variant", uniqueConstraints = { @UniqueConstraint(columnNames = { "name" }) }, indexes = {
        @Index(name = "idx_variant_name", columnList = "name"),
        @Index(name = "idx_variant_active", columnList = "active")
})
public class CylinderVariant extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Variant name is required.")
    @Size(max = 100, message = "Variant name must be at most 100 characters.")
    @Column(nullable = false, unique = true)
    private String name;

    @NotNull(message = "Weight is required.")
    @DecimalMin(value = "0.1", inclusive = true, message = "Weight must be positive.")
    @Column(nullable = false)
    private Double weightKg;

    @NotNull(message = "Active status is required.")
    @Column(nullable = false)
    private Boolean active = true;

    public CylinderVariant() {
    }

    public CylinderVariant(String name, Double weightKg) {
        this.name = name;
        this.weightKg = weightKg;
        this.active = true;
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

    public Double getWeightKg() {
        return weightKg;
    }

    public void setWeightKg(Double weightKg) {
        this.weightKg = weightKg;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
