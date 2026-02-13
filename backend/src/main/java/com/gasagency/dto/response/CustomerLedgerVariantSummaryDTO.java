package com.gasagency.dto.response;

public class CustomerLedgerVariantSummaryDTO {
    private String variantName;
    private Long filledCount;
    private Long returnPending;

    public String getVariantName() {
        return variantName;
    }

    public void setVariantName(String variantName) {
        this.variantName = variantName;
    }

    public Long getFilledCount() {
        return filledCount;
    }

    public void setFilledCount(Long filledCount) {
        this.filledCount = filledCount;
    }

    public Long getReturnPending() {
        return returnPending;
    }

    public void setReturnPending(Long returnPending) {
        this.returnPending = returnPending;
    }
}



