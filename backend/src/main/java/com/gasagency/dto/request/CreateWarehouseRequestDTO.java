package com.gasagency.dto.request;

public class CreateWarehouseRequestDTO {
    private String name;
    private Long businessId;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getBusinessId() {
        return businessId;
    }

    public void setBusinessId(Long businessId) {
        this.businessId = businessId;
    }
}



