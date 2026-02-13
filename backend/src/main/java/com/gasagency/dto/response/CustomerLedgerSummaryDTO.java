package com.gasagency.dto.response;

import java.util.List;

public class CustomerLedgerSummaryDTO {
    private List<CustomerLedgerVariantSummaryDTO> variants;

    public List<CustomerLedgerVariantSummaryDTO> getVariants() {
        return variants;
    }

    public void setVariants(List<CustomerLedgerVariantSummaryDTO> variants) {
        this.variants = variants;
    }
}



