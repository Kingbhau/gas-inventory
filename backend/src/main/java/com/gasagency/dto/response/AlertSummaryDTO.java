package com.gasagency.dto.response;

import java.util.List;

public class AlertSummaryDTO {
    private int count;
    private List<AlertNotificationDTO> alerts;

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public List<AlertNotificationDTO> getAlerts() {
        return alerts;
    }

    public void setAlerts(List<AlertNotificationDTO> alerts) {
        this.alerts = alerts;
    }
}



