package com.gasagency.service;

import com.gasagency.dto.CylinderVariantDTO;
import com.gasagency.entity.CylinderVariant;
import com.gasagency.entity.InventoryStock;
import com.gasagency.entity.MonthlyPrice;
import com.gasagency.entity.SaleItem;
import com.gasagency.entity.CustomerCylinderLedger;
import com.gasagency.entity.CustomerVariantPrice;
import com.gasagency.repository.CylinderVariantRepository;
import com.gasagency.repository.InventoryStockRepository;
import com.gasagency.repository.MonthlyPriceRepository;
import com.gasagency.repository.SaleItemRepository;
import com.gasagency.repository.CustomerCylinderLedgerRepository;
import com.gasagency.repository.CustomerVariantPriceRepository;
import com.gasagency.exception.ResourceNotFoundException;
import com.gasagency.exception.InvalidOperationException;
import com.gasagency.util.LoggerUtil;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CylinderVariantService {
    private static final Logger logger = LoggerFactory.getLogger(CylinderVariantService.class);
    private final CylinderVariantRepository repository;
    private final InventoryStockRepository inventoryStockRepository;
    private final MonthlyPriceRepository monthlyPriceRepository;
    private final SaleItemRepository saleItemRepository;
    private final CustomerCylinderLedgerRepository ledgerRepository;
    private final CustomerVariantPriceRepository customerVariantPriceRepository;

    public CylinderVariantService(CylinderVariantRepository repository,
            InventoryStockRepository inventoryStockRepository,
            MonthlyPriceRepository monthlyPriceRepository,
            SaleItemRepository saleItemRepository,
            CustomerCylinderLedgerRepository ledgerRepository,
            CustomerVariantPriceRepository customerVariantPriceRepository) {
        this.repository = repository;
        this.inventoryStockRepository = inventoryStockRepository;
        this.monthlyPriceRepository = monthlyPriceRepository;
        this.saleItemRepository = saleItemRepository;
        this.ledgerRepository = ledgerRepository;
        this.customerVariantPriceRepository = customerVariantPriceRepository;
    }

    @CacheEvict(value = { "cylinderVariantsAll", "cylinderVariantsActive", "variantById" }, allEntries = true)
    public CylinderVariantDTO createVariant(CylinderVariantDTO dto) {
        LoggerUtil.logBusinessEntry(logger, "CREATE_VARIANT", "name", dto != null ? dto.getName() : "null");

        // Validate input
        if (dto == null || dto.getName() == null || dto.getName().trim().isEmpty()) {
            LoggerUtil.logBusinessError(logger, "CREATE_VARIANT", "Invalid variant data", "reason", "name is empty");
            throw new IllegalArgumentException("Variant name cannot be null or empty");
        }
        if (dto.getWeightKg() == null || dto.getWeightKg() <= 0) {
            LoggerUtil.logBusinessError(logger, "CREATE_VARIANT", "Invalid weight", "weight", dto.getWeightKg());
            throw new IllegalArgumentException("Weight must be greater than 0");
        }

        CylinderVariant variant = new CylinderVariant(dto.getName(), dto.getWeightKg());
        variant.setBasePrice(dto.getBasePrice());
        variant = repository.save(variant);

        LoggerUtil.logBusinessSuccess(logger, "CREATE_VARIANT", "id", variant.getId(), "name", variant.getName());
        LoggerUtil.logAudit("CREATE", "CYLINDER_VARIANT", "variantId", variant.getId(), "name", variant.getName());

        return toDTO(variant);
    }

    @Cacheable(value = "variantById", key = "#id")
    public CylinderVariantDTO getVariantById(Long id) {
        LoggerUtil.logDatabaseOperation(logger, "SELECT", "CYLINDER_VARIANT", "id", id);

        CylinderVariant variant = repository.findById(id)
                .orElseThrow(() -> {
                    LoggerUtil.logBusinessError(logger, "GET_VARIANT", "Variant not found", "id", id);
                    return new ResourceNotFoundException("Variant not found with id: " + id);
                });
        return toDTO(variant);
    }

    @Cacheable("cylinderVariantsAll")
    public List<CylinderVariantDTO> getAllVariants() {
        LoggerUtil.logDatabaseOperation(logger, "SELECT_ALL", "CYLINDER_VARIANT");

        return repository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public Page<CylinderVariantDTO> getAllVariants(Pageable pageable) {
        LoggerUtil.logDatabaseOperation(logger, "SELECT_PAGINATED", "CYLINDER_VARIANT", "page",
                pageable.getPageNumber(), "size", pageable.getPageSize());

        return repository.findAll(pageable)
                .map(this::toDTO);
    }

    @Cacheable("cylinderVariantsActive")
    public List<CylinderVariantDTO> getActiveVariants() {
        LoggerUtil.logDatabaseOperation(logger, "SELECT", "CYLINDER_VARIANT", "filter", "active=true");

        return repository.findAllByActive(true).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get CylinderVariant entity by ID (internal use)
     */
    public CylinderVariant getCylinderVariantEntity(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Variant ID must be positive");
        }

        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found with ID: " + id));
    }

    @CacheEvict(value = { "cylinderVariantsAll", "cylinderVariantsActive", "variantById" }, allEntries = true)
    public CylinderVariantDTO updateVariant(Long id, CylinderVariantDTO dto) {
        LoggerUtil.logBusinessEntry(logger, "UPDATE_VARIANT", "id", id, "name", dto != null ? dto.getName() : "null");

        // Validate input
        if (dto == null || dto.getName() == null || dto.getName().trim().isEmpty()) {
            LoggerUtil.logBusinessError(logger, "UPDATE_VARIANT", "Invalid variant data", "reason", "name is empty");
            throw new IllegalArgumentException("Variant name cannot be null or empty");
        }
        if (dto.getWeightKg() == null || dto.getWeightKg() <= 0) {
            LoggerUtil.logBusinessError(logger, "UPDATE_VARIANT", "Invalid weight", "weight", dto.getWeightKg());
            throw new IllegalArgumentException("Weight must be greater than 0");
        }

        CylinderVariant variant = repository.findById(id)
                .orElseThrow(() -> {
                    LoggerUtil.logBusinessError(logger, "UPDATE_VARIANT", "Variant not found", "id", id);
                    return new ResourceNotFoundException("Variant not found with id: " + id);
                });

        variant.setName(dto.getName());
        variant.setWeightKg(dto.getWeightKg());
        variant.setActive(dto.getActive());

        // Check if basePrice has changed and cascade update to all customer variant
        // prices
        if (dto.getBasePrice() != null && !dto.getBasePrice().equals(variant.getBasePrice())) {
            java.math.BigDecimal oldBasePrice = variant.getBasePrice();
            variant.setBasePrice(dto.getBasePrice());

            // Update all CustomerVariantPrice records where salePrice equals old basePrice
            List<CustomerVariantPrice> pricesWithOldBase = customerVariantPriceRepository.findByVariantId(id);
            for (CustomerVariantPrice cvp : pricesWithOldBase) {
                // Only update if the current sale price matches the old base price (user hasn't
                // customized)
                if (oldBasePrice != null && oldBasePrice.equals(cvp.getSalePrice())) {
                    cvp.setSalePrice(dto.getBasePrice());
                    customerVariantPriceRepository.save(cvp);
                    LoggerUtil.logBusinessSuccess(logger, "CASCADE_UPDATE_PRICE", "customerId",
                            cvp.getCustomer().getId(),
                            "variantId", id, "oldPrice", oldBasePrice, "newPrice", dto.getBasePrice());
                }
            }
        } else if (dto.getBasePrice() != null) {
            variant.setBasePrice(dto.getBasePrice());
        }

        variant = repository.save(variant);

        LoggerUtil.logBusinessSuccess(logger, "UPDATE_VARIANT", "id", variant.getId(), "name", variant.getName());
        LoggerUtil.logAudit("UPDATE", "CYLINDER_VARIANT", "variantId", variant.getId(), "name", variant.getName());

        return toDTO(variant);
    }

    @CacheEvict(value = { "cylinderVariantsAll", "cylinderVariantsActive", "variantById" }, allEntries = true)
    public void deleteVariant(Long id) {
        LoggerUtil.logBusinessEntry(logger, "DELETE_VARIANT", "id", id);

        CylinderVariant variant = repository.findById(id)
                .orElseThrow(() -> {
                    LoggerUtil.logBusinessError(logger, "DELETE_VARIANT", "Variant not found", "id", id);
                    return new ResourceNotFoundException("Variant not found with id: " + id);
                });

        // Check for inventory stock with quantities
        Optional<InventoryStock> stock = inventoryStockRepository.findByVariant(variant);
        if (stock.isPresent()) {
            InventoryStock inv = stock.get();
            if (inv.getFilledQty() > 0 || inv.getEmptyQty() > 0) {
                LoggerUtil.logBusinessError(logger, "DELETE_VARIANT", "Cannot delete - has inventory", "id", id,
                        "filledQty", inv.getFilledQty(), "emptyQty", inv.getEmptyQty());
                throw new InvalidOperationException(
                        "Cannot delete variant with existing inventory. " +
                                "Current: " + inv.getFilledQty() + " filled, " + inv.getEmptyQty() + " empty");
            }
        }

        // Check for price history
        List<MonthlyPrice> prices = monthlyPriceRepository.findByVariant(variant);
        if (!prices.isEmpty()) {
            LoggerUtil.logBusinessError(logger, "DELETE_VARIANT", "Cannot delete - has price history", "id", id,
                    "priceCount", prices.size());
            throw new InvalidOperationException(
                    "Cannot delete variant with price history. " +
                            "Variant has been used in " + prices.size() + " pricing records.");
        }

        // Check for sale items
        List<SaleItem> saleItems = saleItemRepository.findByVariant(variant);
        if (!saleItems.isEmpty()) {
            LoggerUtil.logBusinessError(logger, "DELETE_VARIANT", "Cannot delete - used in sales", "id", id,
                    "saleCount", saleItems.size());
            throw new InvalidOperationException(
                    "Cannot delete variant used in " + saleItems.size() + " sales");
        }

        // Check for ledger entries
        List<CustomerCylinderLedger> ledgers = ledgerRepository.findByVariant(variant);
        if (!ledgers.isEmpty()) {
            LoggerUtil.logBusinessError(logger, "DELETE_VARIANT", "Cannot delete - has ledger entries", "id", id,
                    "ledgerCount", ledgers.size());
            throw new InvalidOperationException(
                    "Cannot delete variant with " + ledgers.size() + " ledger entries");
        }

        repository.delete(variant);

        LoggerUtil.logBusinessSuccess(logger, "DELETE_VARIANT", "id", id);
        LoggerUtil.logAudit("DELETE", "CYLINDER_VARIANT", "variantId", id);
    }

    private CylinderVariantDTO toDTO(CylinderVariant variant) {
        return new CylinderVariantDTO(variant.getId(), variant.getName(),
                variant.getWeightKg(), variant.getActive(), variant.getBasePrice());
    }

    @CacheEvict(value = { "cylinderVariantsAll", "cylinderVariantsActive", "variantById" }, allEntries = true)
    public CylinderVariantDTO reactivateVariant(Long id) {
        logger.info("Reactivating cylinder variant with ID: {}", id);
        CylinderVariant variant = repository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Cylinder variant not found with ID: {}", id);
                    return new ResourceNotFoundException("Cylinder variant not found with id: " + id);
                });

        if (variant.getActive()) {
            logger.warn("Cylinder variant with ID: {} is already active", id);
            throw new InvalidOperationException("Cylinder variant is already active");
        }

        variant.setActive(true);
        LoggerUtil.logBusinessSuccess(logger, "REACTIVATE_VARIANT", "id", id);
        return toDTO(repository.save(variant));
    }
}
