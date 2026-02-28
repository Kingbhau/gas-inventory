package com.gasagency.dto.request;

import java.util.List;

public class LedgerBulkVerificationRequestDTO {
    private List<Long> ledgerIds;
    private String remark;

    public List<Long> getLedgerIds() {
        return ledgerIds;
    }

    public void setLedgerIds(List<Long> ledgerIds) {
        this.ledgerIds = ledgerIds;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
