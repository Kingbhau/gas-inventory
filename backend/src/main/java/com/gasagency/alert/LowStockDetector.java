package com.gasagency.alert;

import com.gasagency.dto.response.InventoryStockDTO;
import com.gasagency.service.AlertConfigurationService;
import com.gasagency.service.AlertNotificationService;
import com.gasagency.service.InventoryStockService;
import com.gasagency.entity.AlertConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Alert detector for LOW STOCK
 * Monitors filled and empty cylinder levels per warehouse
 */
@Component
public class LowStockDetector implements AlertDetector {

    private static final Logger logger = LoggerFactory.getLogger(LowStockDetector.class);
    private static final String ALERT_TYPE = "LOW_STOCK_WAREHOUSE";

    private final InventoryStockService inventoryStockService;
    private final AlertConfigurationService configService;
    private final AlertNotificationService notificationService;

    public LowStockDetector(InventoryStockService inventoryStockService,
            AlertConfigurationService configService,
            AlertNotificationService notificationService) {
        this.inventoryStockService = inventoryStockService;
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
            int filledThreshold = config.getFilledCylinderThreshold() != null ? config.getFilledCylinderThreshold()
                    : 50;
            int emptyThreshold = config.getEmptyCylinderThreshold() != null ? config.getEmptyCylinderThreshold() : 50;

            // Get all warehouse inventory
            List<InventoryStockDTO> allStock = inventoryStockService.getAllStock();

            for (InventoryStockDTO stock : allStock) {
                if (stock.getWarehouseId() == null)
                    continue;

                Long warehouseId = stock.getWarehouseId();
                String warehouseName = stock.getWarehouseName() != null ? stock.getWarehouseName()
                        : "Warehouse " + warehouseId;

                // Check filled cylinders
                long filledQty = stock.getFilledQty() != null ? stock.getFilledQty() : 0;
                if (filledQty < filledThreshold) {
                    String alertKey = "LOW_STOCK_FILLED_WH_" + warehouseId;
                    String message = warehouseName + ": Only " + filledQty +
                            " filled cylinders (threshold: " + filledThreshold + ")";

                    notificationService.createOrUpdateAlert(
                            ALERT_TYPE,
                            alertKey,
                            warehouseId,
                            null,
                            message,
                            "warning");
                    logger.info("Low filled stock alert created for {}", warehouseName);
                }

                // Check empty cylinders
                long emptyQty = stock.getEmptyQty() != null ? stock.getEmptyQty() : 0;
                if (emptyQty < emptyThreshold) {
                    String alertKey = "LOW_STOCK_EMPTY_WH_" + warehouseId;
                    String message = warehouseName + ": Only " + emptyQty +
                            " empty cylinders (threshold: " + emptyThreshold + ")";

                    notificationService.createOrUpdateAlert(
                            ALERT_TYPE,
                            alertKey,
                            warehouseId,
                            null,
                            message,
                            "warning");
                    logger.info("Low empty stock alert created for {}", warehouseName);
                }
            }
        } catch (Exception e) {
            logger.error("Error detecting low stock alerts", e);
        }
    }
}

