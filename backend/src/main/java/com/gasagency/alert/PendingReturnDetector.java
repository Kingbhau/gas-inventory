package com.gasagency.alert;

import com.gasagency.entity.Customer;
import com.gasagency.repository.CustomerRepository;
import com.gasagency.service.AlertConfigurationService;
import com.gasagency.service.AlertNotificationService;
import com.gasagency.service.CustomerCylinderLedgerService;
import com.gasagency.entity.AlertConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Alert detector for PENDING RETURN CYLINDERS
 * Monitors cylinders pending return from customers
 */
@Component
public class PendingReturnDetector implements AlertDetector {

    private static final Logger logger = LoggerFactory.getLogger(PendingReturnDetector.class);
    private static final String ALERT_TYPE = "PENDING_RETURN_CYLINDERS";

    private final CustomerRepository customerRepository;
    private final CustomerCylinderLedgerService ledgerService;
    private final AlertConfigurationService configService;
    private final AlertNotificationService notificationService;

    public PendingReturnDetector(CustomerRepository customerRepository,
            CustomerCylinderLedgerService ledgerService,
            AlertConfigurationService configService,
            AlertNotificationService notificationService) {
        this.customerRepository = customerRepository;
        this.ledgerService = ledgerService;
        this.configService = configService;
        this.notificationService = notificationService;
    }

    @Override
    public String getAlertType() {
        return ALERT_TYPE;
    }

    @Override
    public void detectAndCreateAlerts() {
        try {
            // Get alert configuration
            Optional<AlertConfiguration> configOpt = configService.getConfigOptional(ALERT_TYPE);

            if (configOpt.isEmpty() || !configOpt.get().getEnabled()) {
                return; // Alert disabled
            }

            AlertConfiguration config = configOpt.get();
            int pendingThreshold = config.getPendingReturnThreshold() != null ? config.getPendingReturnThreshold() : 10;

            // Get all customers
            List<Customer> allCustomers = customerRepository.findAll();

            for (Customer customer : allCustomers) {
                try {
                    // Get count of pending return cylinders for this customer
                    long pendingCount = ledgerService.getPendingReturnCountForCustomer(customer.getId());

                    if (pendingCount >= pendingThreshold) {
                        String alertKey = "PENDING_RETURN_CUST_" + customer.getId();
                        String message = customer.getName() +
                                ": " + pendingCount + " cylinders pending return (threshold: " + pendingThreshold + ")";

                        notificationService.createOrUpdateAlert(
                                ALERT_TYPE,
                                alertKey,
                                null,
                                customer.getId(),
                                message,
                                "warning");
                        logger.info("Pending return alert created for customer: {}", customer.getName());
                    }
                } catch (Exception e) {
                    logger.warn("Error checking pending returns for customer {}: {}", customer.getId(), e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.error("Error detecting pending return alerts", e);
        }
    }
}
