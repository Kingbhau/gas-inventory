package com.gasagency.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class CylinderVariantDTO {
    private Long id;

    @NotBlank(message = "Variant name cannot be null or blank")
    @Size(min = 2, max = 100, message = "Variant name must be between 2 and 100 characters")
    private String name;

    @Positive(message = "Weight must be greater than 0")
    private Double weightKg;

    private Boolean active;

    public CylinderVariantDTO() {
    }

    public CylinderVariantDTO(Long id, String name, Double weightKg, Boolean active) {
        this.id = id;
        this.name = name;
        this.weightKg = weightKg;
        this.active = active;
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
