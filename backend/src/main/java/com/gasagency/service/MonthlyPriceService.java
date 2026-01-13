package com.gasagency.service;

import com.gasagency.dto.MonthlyPriceDTO;
import com.gasagency.entity.MonthlyPrice;
import com.gasagency.entity.CylinderVariant;
import com.gasagency.repository.MonthlyPriceRepository;
import com.gasagency.repository.CylinderVariantRepository;
import com.gasagency.exception.ResourceNotFoundException;
import com.gasagency.util.LoggerUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MonthlyPriceService {
        private static final Logger logger = LoggerFactory.getLogger(MonthlyPriceService.class);

        private final MonthlyPriceRepository repository;
        private final CylinderVariantRepository variantRepository;

        public MonthlyPriceService(MonthlyPriceRepository repository,
                        CylinderVariantRepository variantRepository) {
                this.repository = repository;
                this.variantRepository = variantRepository;
        }

        @Transactional
        public MonthlyPriceDTO createPrice(MonthlyPriceDTO dto) {
                LoggerUtil.logBusinessEntry(logger, "CREATE_PRICE", "variantId",
                                dto != null ? dto.getVariantId() : "null", "month",
                                dto != null ? dto.getMonthYear() : "null");

                // Validate input
                if (dto == null || dto.getVariantId() == null) {
                        LoggerUtil.logBusinessError(logger, "CREATE_PRICE", "Invalid price request", "reason",
                                        "variant ID is null");
                        throw new IllegalArgumentException("Variant ID cannot be null");
                }
                if (dto.getMonthYear() == null) {
                        LoggerUtil.logBusinessError(logger, "CREATE_PRICE", "Invalid price request", "reason",
                                        "month year is null");
                        throw new IllegalArgumentException("Month year cannot be null");
                }
                if (dto.getBasePrice() == null || dto.getBasePrice().signum() <= 0) {
                        LoggerUtil.logBusinessError(logger, "CREATE_PRICE", "Invalid base price", "price",
                                        dto != null ? dto.getBasePrice() : "null");
                        throw new IllegalArgumentException("Price must be greater than 0");
                }

                CylinderVariant variant = variantRepository.findById(dto.getVariantId())
                                .orElseThrow(() -> {
                                        LoggerUtil.logBusinessError(logger, "CREATE_PRICE", "Variant not found",
                                                        "variantId", dto.getVariantId());
                                        return new ResourceNotFoundException(
                                                        "Variant not found with id: " + dto.getVariantId());
                                });

                // Prevent duplicate price for same variant and month
                if (repository.findByVariantAndMonthYear(variant, dto.getMonthYear()).isPresent()) {
                        LoggerUtil.logBusinessError(logger, "CREATE_PRICE", "Duplicate price for variant/month",
                                        "variantId", dto.getVariantId(), "monthYear", dto.getMonthYear());
                        throw new IllegalArgumentException("A price for this variant and month already exists.");
                }

                MonthlyPrice price = new MonthlyPrice(variant, dto.getMonthYear(), dto.getBasePrice());
                price = repository.save(price);

                LoggerUtil.logBusinessSuccess(logger, "CREATE_PRICE", "id", price.getId(), "variant", variant.getName(),
                                "price", dto.getBasePrice());
                LoggerUtil.logAudit("CREATE", "MONTHLY_PRICE", "priceId", price.getId(), "variantId", variant.getId());

                return toDTO(price);
        }

        public MonthlyPriceDTO getPriceById(Long id) {
                LoggerUtil.logDatabaseOperation(logger, "SELECT", "MONTHLY_PRICE", "id", id);

                MonthlyPrice price = repository.findById(id)
                                .orElseThrow(() -> {
                                        LoggerUtil.logBusinessError(logger, "GET_PRICE", "Price not found", "id", id);
                                        return new ResourceNotFoundException("Price not found with id: " + id);
                                });
                return toDTO(price);
        }

        public List<MonthlyPriceDTO> getAllPrices() {
                LoggerUtil.logDatabaseOperation(logger, "SELECT_ALL", "MONTHLY_PRICE");

                return repository.findAll().stream()
                                .map(this::toDTO)
                                .collect(Collectors.toList());
        }

        public Page<MonthlyPriceDTO> getAllPrices(Pageable pageable) {
                LoggerUtil.logDatabaseOperation(logger, "SELECT_PAGINATED", "MONTHLY_PRICE", "page",
                                pageable.getPageNumber(), "size", pageable.getPageSize());

                return repository.findAll(pageable)
                                .map(this::toDTO);
        }

        public List<MonthlyPriceDTO> getPricesByVariant(Long variantId) {
                LoggerUtil.logDatabaseOperation(logger, "SELECT", "MONTHLY_PRICE", "variantId", variantId);

                CylinderVariant variant = variantRepository.findById(variantId)
                                .orElseThrow(() -> {
                                        LoggerUtil.logBusinessError(logger, "GET_PRICES_BY_VARIANT",
                                                        "Variant not found", "variantId", variantId);
                                        return new ResourceNotFoundException(
                                                        "Variant not found with id: " + variantId);
                                });
                return repository.findByVariant(variant).stream()
                                .map(this::toDTO)
                                .collect(Collectors.toList());
        }

        public MonthlyPriceDTO getPriceForVariantAndMonth(Long variantId, LocalDate monthYear) {
                LoggerUtil.logDatabaseOperation(logger, "SELECT", "MONTHLY_PRICE", "variantId", variantId, "month",
                                monthYear);

                CylinderVariant variant = variantRepository.findById(variantId)
                                .orElseThrow(() -> {
                                        LoggerUtil.logBusinessError(logger, "GET_PRICE_FOR_VARIANT_MONTH",
                                                        "Variant not found", "variantId", variantId);
                                        return new ResourceNotFoundException(
                                                        "Variant not found with id: " + variantId);
                                });
                MonthlyPrice price = repository.findByVariantAndMonthYear(variant, monthYear)
                                .orElseThrow(() -> {
                                        LoggerUtil.logBusinessError(logger, "GET_PRICE_FOR_VARIANT_MONTH",
                                                        "Price not found", "variantId", variantId, "month", monthYear);
                                        return new ResourceNotFoundException(
                                                        "Price not found for variant and month");
                                });
                return toDTO(price);
        }

        public MonthlyPriceDTO getLatestPriceForVariant(Long variantId, LocalDate monthYear) {
                LoggerUtil.logDatabaseOperation(logger, "SELECT", "MONTHLY_PRICE", "variantId", variantId, "month",
                                monthYear);
                CylinderVariant variant = variantRepository.findById(variantId)
                                .orElseThrow(() -> {
                                        LoggerUtil.logBusinessError(logger, "GET_LATEST_PRICE_FOR_VARIANT_MONTH",
                                                        "Variant not found", "variantId", variantId);
                                        return new ResourceNotFoundException(
                                                        "Variant not found with id: " + variantId);
                                });
                MonthlyPrice price = repository
                                .findTopByVariantAndMonthYearLessThanEqualOrderByMonthYearDesc(variant, monthYear)
                                .orElseThrow(() -> {
                                        LoggerUtil.logBusinessError(logger, "GET_LATEST_PRICE_FOR_VARIANT_MONTH",
                                                        "No price found for variant up to month", "variantId",
                                                        variantId, "month", monthYear);
                                        return new ResourceNotFoundException(
                                                        "No price found for variant up to month: " + monthYear);
                                });
                return toDTO(price);
        }

        @Transactional
        public MonthlyPriceDTO updatePrice(Long id, MonthlyPriceDTO dto) {
                LoggerUtil.logBusinessEntry(logger, "UPDATE_PRICE", "id", id);

                // Validate input
                if (dto == null || dto.getMonthYear() == null) {
                        LoggerUtil.logBusinessError(logger, "UPDATE_PRICE", "Invalid price data", "reason",
                                        "month year is null");
                        throw new IllegalArgumentException("Month year cannot be null");
                }
                if (dto.getBasePrice() == null || dto.getBasePrice().signum() <= 0) {
                        LoggerUtil.logBusinessError(logger, "UPDATE_PRICE", "Invalid base price", "price",
                                        dto.getBasePrice());
                        throw new IllegalArgumentException("Price must be greater than 0");
                }

                MonthlyPrice price = repository.findById(id)
                                .orElseThrow(() -> {
                                        LoggerUtil.logBusinessError(logger, "UPDATE_PRICE", "Price not found", "id",
                                                        id);
                                        return new ResourceNotFoundException("Price not found with id: " + id);
                                });

                // Prevent duplicate price for same variant and month (except self)
                if (repository.findByVariantAndMonthYear(price.getVariant(), dto.getMonthYear())
                                .filter(existing -> !existing.getId().equals(id)).isPresent()) {
                        LoggerUtil.logBusinessError(logger, "UPDATE_PRICE", "Duplicate price for variant/month",
                                        "variantId", price.getVariant().getId(), "monthYear", dto.getMonthYear());
                        throw new IllegalArgumentException("A price for this variant and month already exists.");
                }
                price.setMonthYear(dto.getMonthYear());
                price.setBasePrice(dto.getBasePrice());
                price = repository.save(price);

                LoggerUtil.logBusinessSuccess(logger, "UPDATE_PRICE", "id", price.getId(), "price", dto.getBasePrice());
                LoggerUtil.logAudit("UPDATE", "MONTHLY_PRICE", "priceId", price.getId());

                return toDTO(price);
        }

        @Transactional
        public void deletePrice(Long id) {
                LoggerUtil.logBusinessEntry(logger, "DELETE_PRICE", "id", id);

                MonthlyPrice price = repository.findById(id)
                                .orElseThrow(() -> {
                                        LoggerUtil.logBusinessError(logger, "DELETE_PRICE", "Price not found", "id",
                                                        id);
                                        return new ResourceNotFoundException("Price not found with id: " + id);
                                });
                repository.delete(price);

                LoggerUtil.logBusinessSuccess(logger, "DELETE_PRICE", "id", id);
                LoggerUtil.logAudit("DELETE", "MONTHLY_PRICE", "priceId", id);
        }

        private MonthlyPriceDTO toDTO(MonthlyPrice price) {
                return new MonthlyPriceDTO(price.getId(), price.getVariant().getId(),
                                price.getVariant().getName(), price.getMonthYear(), price.getBasePrice(),
                                price.getCreatedAt());
        }
}
