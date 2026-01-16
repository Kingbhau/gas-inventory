package com.gasagency.service;

import com.gasagency.dto.CustomerDTO;
import com.gasagency.entity.Customer;
import com.gasagency.entity.CylinderVariant;
import com.gasagency.entity.Sale;
import com.gasagency.entity.CustomerCylinderLedger;
import com.gasagency.repository.CustomerRepository;
import com.gasagency.repository.CylinderVariantRepository;
import com.gasagency.repository.SaleRepository;
import com.gasagency.repository.CustomerCylinderLedgerRepository;
import com.gasagency.exception.ResourceNotFoundException;
import com.gasagency.exception.InvalidOperationException;
import com.gasagency.util.LoggerUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CustomerService {
    private static final Logger logger = LoggerFactory.getLogger(CustomerService.class);

    private final CustomerRepository repository;
    private final SaleRepository saleRepository;
    private final CustomerCylinderLedgerRepository ledgerRepository;
    private final CylinderVariantRepository cylinderVariantRepository;
    private final ObjectMapper objectMapper;

    public CustomerService(CustomerRepository repository,
            SaleRepository saleRepository,
            CustomerCylinderLedgerRepository ledgerRepository, CylinderVariantRepository cylinderVariantRepository) {
        this.repository = repository;
        this.saleRepository = saleRepository;
        this.ledgerRepository = ledgerRepository;
        this.cylinderVariantRepository = cylinderVariantRepository;
        this.objectMapper = new ObjectMapper();
    }

    @Transactional
    public CustomerDTO createCustomer(CustomerDTO dto) {
        LoggerUtil.logBusinessEntry(logger, "CREATE_CUSTOMER", "name", dto != null ? dto.getName() : "null");

        // Validate input
        if (dto == null || dto.getName() == null || dto.getName().trim().isEmpty()) {
            LoggerUtil.logBusinessError(logger, "CREATE_CUSTOMER", "Invalid customer data", "reason", "name is empty");
            throw new IllegalArgumentException("Customer name cannot be null or empty");
        }

        String mobile = dto.getMobile();
        if (mobile == null || mobile.trim().isEmpty()) {
            LoggerUtil.logBusinessError(logger, "CREATE_CUSTOMER", "Invalid customer data", "reason",
                    "mobile is empty");
            throw new IllegalArgumentException("Customer mobile cannot be null or empty");
        }

        mobile = mobile.trim();
        if (!isValidMobileNumber(mobile)) {
            LoggerUtil.logBusinessError(logger, "CREATE_CUSTOMER", "Invalid mobile format", "mobile", mobile);
            throw new IllegalArgumentException(
                    "Invalid mobile number format. Expected 10+ digits. Received: " + mobile);
        }

        // Prevent duplicate mobile
        if (repository.findByMobile(mobile).isPresent()) {
            LoggerUtil.logBusinessError(logger, "CREATE_CUSTOMER", "Duplicate mobile", "mobile", mobile);
            throw new IllegalArgumentException("A customer with this mobile number already exists.");
        }

        Customer customer = new Customer(dto.getName(), mobile, dto.getAddress());
        customer.setSalePrice(dto.getSalePrice());
        customer.setDiscountPrice(dto.getDiscountPrice());
        customer.setConfiguredVariants(convertVariantListToJson(dto.getConfiguredVariants()));
        customer = repository.save(customer);

        LoggerUtil.logBusinessSuccess(logger, "CREATE_CUSTOMER", "id", customer.getId(), "name", customer.getName(),
                "mobile", customer.getMobile());
        LoggerUtil.logAudit("CREATE", "CUSTOMER", "customerId", customer.getId(), "name", customer.getName());

        return toDTO(customer);
    }

    public CustomerDTO getCustomerById(Long id) {
        LoggerUtil.logDatabaseOperation(logger, "SELECT", "CUSTOMER", "id", id);

        Customer customer = repository.findById(id)
                .orElseThrow(() -> {
                    LoggerUtil.logBusinessError(logger, "GET_CUSTOMER", "Customer not found", "id", id);
                    return new ResourceNotFoundException("Customer not found with id: " + id);
                });
        return toDTO(customer);
    }

    public List<CustomerDTO> getAllCustomers() {
        LoggerUtil.logDatabaseOperation(logger, "SELECT_ALL", "CUSTOMER");
        return repository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public Page<CustomerDTO> getAllCustomers(Pageable pageable) {
        LoggerUtil.logDatabaseOperation(logger, "SELECT_PAGINATED", "CUSTOMER", "page", pageable.getPageNumber(),
                "size", pageable.getPageSize());
        return repository.findAll(pageable)
                .map(this::toDTO);
    }

    public List<CustomerDTO> getActiveCustomers() {
        LoggerUtil.logDatabaseOperation(logger, "SELECT", "CUSTOMER", "filter", "active=true");
        return repository.findAllByActive(true).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public CustomerDTO updateCustomer(Long id, CustomerDTO dto) {
        LoggerUtil.logBusinessEntry(logger, "UPDATE_CUSTOMER", "id", id, "name", dto != null ? dto.getName() : "null");

        // Validate input
        if (dto == null || dto.getName() == null || dto.getName().trim().isEmpty()) {
            LoggerUtil.logBusinessError(logger, "UPDATE_CUSTOMER", "Invalid customer data", "reason", "name is empty");
            throw new IllegalArgumentException("Customer name cannot be null or empty");
        }

        final String mobileFinal;
        String mobile = dto.getMobile();
        if (mobile == null || mobile.trim().isEmpty()) {
            LoggerUtil.logBusinessError(logger, "UPDATE_CUSTOMER", "Invalid customer data", "reason",
                    "mobile is empty");
            throw new IllegalArgumentException("Customer mobile cannot be null or empty");
        }

        mobile = mobile.trim();
        if (!isValidMobileNumber(mobile)) {
            LoggerUtil.logBusinessError(logger, "UPDATE_CUSTOMER", "Invalid mobile format", "mobile", mobile);
            throw new IllegalArgumentException(
                    "Invalid mobile number format. Expected 10+ digits. Received: " + mobile);
        }
        mobileFinal = mobile;

        Customer customer = repository.findById(id)
                .orElseThrow(() -> {
                    LoggerUtil.logBusinessError(logger, "UPDATE_CUSTOMER", "Customer not found", "id", id);
                    return new ResourceNotFoundException("Customer not found with id: " + id);
                });

        // Prevent duplicate mobile (except for self)
        repository.findByMobile(mobileFinal).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                LoggerUtil.logBusinessError(logger, "UPDATE_CUSTOMER", "Duplicate mobile", "mobile", mobileFinal);
                throw new IllegalArgumentException("A customer with this mobile number already exists.");
            }
        });
        customer.setName(dto.getName());
        customer.setMobile(mobileFinal);
        customer.setAddress(dto.getAddress());
        customer.setSalePrice(dto.getSalePrice());
        customer.setDiscountPrice(dto.getDiscountPrice());
        customer.setConfiguredVariants(convertVariantListToJson(dto.getConfiguredVariants()));
        customer.setActive(dto.getActive());
        customer = repository.save(customer);

        LoggerUtil.logBusinessSuccess(logger, "UPDATE_CUSTOMER", "id", customer.getId(), "name", customer.getName());
        LoggerUtil.logAudit("UPDATE", "CUSTOMER", "customerId", customer.getId(), "name", customer.getName());

        return toDTO(customer);
    }

    @Transactional
    public void deleteCustomer(Long id) {
        LoggerUtil.logBusinessEntry(logger, "DELETE_CUSTOMER", "id", id);

        Customer customer = repository.findById(id)
                .orElseThrow(() -> {
                    LoggerUtil.logBusinessError(logger, "DELETE_CUSTOMER", "Customer not found", "id", id);
                    return new ResourceNotFoundException("Customer not found with id: " + id);
                });

        // Check for associated sales
        List<Sale> associatedSales = saleRepository.findByCustomer(customer);
        if (!associatedSales.isEmpty()) {
            LoggerUtil.logBusinessError(logger, "DELETE_CUSTOMER", "Cannot delete - has associated sales",
                    "id", id, "salesCount", associatedSales.size());
            throw new InvalidOperationException(
                    "Cannot delete customer with " + associatedSales.size() +
                            " associated sales. Please archive or reassign sales first.");
        }

        // Check for ledger entries
        List<CustomerCylinderLedger> ledgerEntries = ledgerRepository.findByCustomer(customer);
        if (!ledgerEntries.isEmpty()) {
            LoggerUtil.logBusinessError(logger, "DELETE_CUSTOMER", "Cannot delete - has ledger entries",
                    "id", id, "ledgerCount", ledgerEntries.size());
            throw new InvalidOperationException(
                    "Cannot delete customer with active ledger entries. " +
                            "Please settle all transactions first.");
        }

        repository.delete(customer);

        LoggerUtil.logBusinessSuccess(logger, "DELETE_CUSTOMER", "id", id);
        LoggerUtil.logAudit("DELETE", "CUSTOMER", "customerId", id);
    }

    private CustomerDTO toDTO(Customer customer) {
        CustomerDTO dto = new CustomerDTO(customer.getId(), customer.getName(),
                customer.getMobile(), customer.getAddress(), customer.getActive());

        // Set pricing fields
        dto.setSalePrice(customer.getSalePrice());
        dto.setDiscountPrice(customer.getDiscountPrice());
        dto.setConfiguredVariants(convertJsonToVariantList(customer.getConfiguredVariants()));

        // Get last sale date
        List<Sale> sales = saleRepository.findByCustomer(customer);
        if (!sales.isEmpty()) {
            sales.sort((s1, s2) -> s2.getSaleDate().compareTo(s1.getSaleDate()));
            dto.setLastSaleDate(sales.get(0).getSaleDate());
        }

        // Get total pending units: sum the latest balance for each variant
        long totalPending = 0L;
        List<CylinderVariant> variants = cylinderVariantRepository.findAllByActive(true);
        for (CylinderVariant variant : variants) {
            List<CustomerCylinderLedger> latestLedger = ledgerRepository
                    .findLatestLedger(customer.getId(), variant.getId());
            if (!latestLedger.isEmpty() && latestLedger.get(0).getBalance() != null) {
                totalPending += latestLedger.get(0).getBalance();
            }
        }
        dto.setTotalPending(totalPending);

        return dto;
    }

    // Validate mobile number format
    private boolean isValidMobileNumber(String mobile) {
        // Accept: +country codes, spaces, hyphens, parentheses, 10+ digits
        return mobile.matches("^[+]?[(]?[0-9]{1,3}[)]?[-\\s.]?[(]?[0-9]{1,4}[)]?[-\\s.]?[0-9]{1,4}[-\\s.]?[0-9]{1,9}$");
    }

    /**
     * Convert List<Long> to JSON string for storage
     */
    private String convertVariantListToJson(List<Long> variants) {
        if (variants == null || variants.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(variants);
        } catch (Exception e) {
            logger.warn("Failed to convert variant list to JSON", e);
            return null;
        }
    }

    /**
     * Convert JSON string back to List<Long>
     */
    private List<Long> convertJsonToVariantList(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Long.class));
        } catch (Exception e) {
            logger.warn("Failed to convert JSON to variant list", e);
            return null;
        }
    }
}
