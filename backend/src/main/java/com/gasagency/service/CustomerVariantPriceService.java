package com.gasagency.service;

import com.gasagency.dto.CustomerVariantPriceDTO;
import com.gasagency.entity.CustomerVariantPrice;
import com.gasagency.entity.Customer;
import com.gasagency.entity.CylinderVariant;
import com.gasagency.repository.CustomerVariantPriceRepository;
import com.gasagency.repository.CustomerRepository;
import com.gasagency.repository.CylinderVariantRepository;
import com.gasagency.exception.ResourceNotFoundException;
import com.gasagency.util.LoggerUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CustomerVariantPriceService {
    private static final Logger logger = LoggerFactory.getLogger(CustomerVariantPriceService.class);

    private final CustomerVariantPriceRepository repository;
    private final CustomerRepository customerRepository;
    private final CylinderVariantRepository variantRepository;

    public CustomerVariantPriceService(CustomerVariantPriceRepository repository,
            CustomerRepository customerRepository,
            CylinderVariantRepository variantRepository) {
        this.repository = repository;
        this.customerRepository = customerRepository;
        this.variantRepository = variantRepository;
    }

    @Transactional
    public CustomerVariantPriceDTO createPrice(CustomerVariantPriceDTO dto) {
        LoggerUtil.logBusinessEntry(logger, "CREATE_CUSTOMER_VARIANT_PRICE",
                "customerId", dto.getCustomerId(), "variantId", dto.getVariantId());

        // Validate customer exists
        Customer customer = customerRepository.findById(dto.getCustomerId())
                .orElseThrow(() -> {
                    LoggerUtil.logBusinessError(logger, "CREATE_CUSTOMER_VARIANT_PRICE",
                            "Customer not found", "customerId", dto.getCustomerId());
                    return new ResourceNotFoundException("Customer not found with id: " + dto.getCustomerId());
                });

        // Validate variant exists
        CylinderVariant variant = variantRepository.findById(dto.getVariantId())
                .orElseThrow(() -> {
                    LoggerUtil.logBusinessError(logger, "CREATE_CUSTOMER_VARIANT_PRICE",
                            "Variant not found", "variantId", dto.getVariantId());
                    return new ResourceNotFoundException("Variant not found with id: " + dto.getVariantId());
                });

        // Check if pricing already exists
        if (repository.existsByCustomerIdAndVariantId(dto.getCustomerId(), dto.getVariantId())) {
            LoggerUtil.logBusinessError(logger, "CREATE_CUSTOMER_VARIANT_PRICE",
                    "Pricing already exists", "customerId", dto.getCustomerId(), "variantId", dto.getVariantId());
            throw new IllegalArgumentException("Pricing already exists for this customer-variant combination");
        }

        CustomerVariantPrice price = new CustomerVariantPrice(customer, variant,
                dto.getSalePrice(), dto.getDiscountPrice());
        price = repository.save(price);

        LoggerUtil.logBusinessSuccess(logger, "CREATE_CUSTOMER_VARIANT_PRICE",
                "id", price.getId(), "customerId", customer.getId(), "variantId", variant.getId());

        return toDTO(price);
    }

    public CustomerVariantPriceDTO getPriceByCustomerAndVariant(Long customerId, Long variantId) {
        LoggerUtil.logDatabaseOperation(logger, "SELECT", "CUSTOMER_VARIANT_PRICE",
                "customerId", customerId, "variantId", variantId);

        return repository.findByCustomerIdAndVariantId(customerId, variantId)
                .map(this::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Pricing not found for customer: " + customerId + " and variant: " + variantId));
    }

    public List<CustomerVariantPriceDTO> getPricesByCustomer(Long customerId) {
        LoggerUtil.logDatabaseOperation(logger, "SELECT_ALL", "CUSTOMER_VARIANT_PRICE",
                "customerId", customerId);

        return repository.findByCustomerId(customerId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<CustomerVariantPriceDTO> getPricesByVariant(Long variantId) {
        LoggerUtil.logDatabaseOperation(logger, "SELECT_ALL", "CUSTOMER_VARIANT_PRICE",
                "variantId", variantId);

        return repository.findByVariantId(variantId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public CustomerVariantPriceDTO updatePrice(Long id, CustomerVariantPriceDTO dto) {
        LoggerUtil.logBusinessEntry(logger, "UPDATE_CUSTOMER_VARIANT_PRICE", "id", id);

        CustomerVariantPrice price = repository.findById(id)
                .orElseThrow(() -> {
                    LoggerUtil.logBusinessError(logger, "UPDATE_CUSTOMER_VARIANT_PRICE",
                            "Pricing not found", "id", id);
                    return new ResourceNotFoundException("Pricing not found with id: " + id);
                });

        price.setSalePrice(dto.getSalePrice());
        price.setDiscountPrice(dto.getDiscountPrice());
        price = repository.save(price);

        LoggerUtil.logBusinessSuccess(logger, "UPDATE_CUSTOMER_VARIANT_PRICE",
                "id", price.getId(), "customerId", price.getCustomer().getId());

        return toDTO(price);
    }

    @Transactional
    public void deletePrice(Long id) {
        LoggerUtil.logBusinessEntry(logger, "DELETE_CUSTOMER_VARIANT_PRICE", "id", id);

        if (!repository.existsById(id)) {
            LoggerUtil.logBusinessError(logger, "DELETE_CUSTOMER_VARIANT_PRICE",
                    "Pricing not found", "id", id);
            throw new ResourceNotFoundException("Pricing not found with id: " + id);
        }

        repository.deleteById(id);
        LoggerUtil.logBusinessSuccess(logger, "DELETE_CUSTOMER_VARIANT_PRICE", "id", id);
    }

    @Transactional
    public void deletePriceByCustomerAndVariant(Long customerId, Long variantId) {
        LoggerUtil.logBusinessEntry(logger, "DELETE_CUSTOMER_VARIANT_PRICE_BY_COMBO",
                "customerId", customerId, "variantId", variantId);

        repository.deleteByCustomerIdAndVariantId(customerId, variantId);
        LoggerUtil.logBusinessSuccess(logger, "DELETE_CUSTOMER_VARIANT_PRICE_BY_COMBO",
                "customerId", customerId, "variantId", variantId);
    }

    private CustomerVariantPriceDTO toDTO(CustomerVariantPrice price) {
        CustomerVariantPriceDTO dto = new CustomerVariantPriceDTO(
                price.getCustomer().getId(),
                price.getVariant().getId(),
                price.getVariant().getName(),
                price.getSalePrice(),
                price.getDiscountPrice());
        dto.setId(price.getId());
        return dto;
    }
}
