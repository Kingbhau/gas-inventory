package com.gasagency.service;

import com.gasagency.dto.SupplierDTO;
import com.gasagency.entity.Supplier;
import com.gasagency.entity.BusinessInfo;
import com.gasagency.repository.SupplierRepository;
import com.gasagency.repository.BusinessInfoRepository;
import com.gasagency.exception.ResourceNotFoundException;
import com.gasagency.repository.SupplierTransactionRepository;
import com.gasagency.util.LoggerUtil;
import com.gasagency.util.CodeGenerator;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SupplierService {
    private static final Logger logger = LoggerFactory.getLogger(SupplierService.class);
    private final SupplierRepository repository;
    private final com.gasagency.repository.SupplierTransactionRepository supplierTransactionRepository;
    private final CodeGenerator codeGenerator;
    private final BusinessInfoRepository businessInfoRepository;

    public SupplierService(SupplierRepository repository,
            SupplierTransactionRepository supplierTransactionRepository,
            CodeGenerator codeGenerator,
            BusinessInfoRepository businessInfoRepository) {
        this.repository = repository;
        this.supplierTransactionRepository = supplierTransactionRepository;
        this.codeGenerator = codeGenerator;
        this.businessInfoRepository = businessInfoRepository;
    }

    @CacheEvict(value = { "suppliersAll" }, allEntries = true)
    public SupplierDTO createSupplier(SupplierDTO dto, Long businessId) {
        LoggerUtil.logBusinessEntry(logger, "CREATE_SUPPLIER", "name", dto != null ? dto.getName() : "null");

        // Validate businessId
        if (businessId == null) {
            throw new IllegalArgumentException("Business ID is required");
        }

        // Load business entity
        BusinessInfo business = businessInfoRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found with id: " + businessId));

        Supplier supplier = new Supplier(dto.getName(), dto.getContact());
        // Auto-generate unique supplier code
        String supplierCode = codeGenerator.generateSupplierCode();
        supplier.setCode(supplierCode);
        supplier.setBusiness(business);

        supplier = repository.save(supplier);

        LoggerUtil.logBusinessSuccess(logger, "CREATE_SUPPLIER", "id", supplier.getId(), "name", supplier.getName());
        LoggerUtil.logAudit("CREATE", "SUPPLIER", "supplierId", supplier.getId(), "name", supplier.getName());

        return toDTO(supplier);
    }

    public SupplierDTO getSupplierById(Long id) {
        LoggerUtil.logDatabaseOperation(logger, "SELECT", "SUPPLIER", "id", id);

        Supplier supplier = repository.findById(id)
                .orElseThrow(() -> {
                    LoggerUtil.logBusinessError(logger, "GET_SUPPLIER", "Supplier not found", "id", id);
                    return new ResourceNotFoundException("Supplier not found with id: " + id);
                });
        return toDTO(supplier);
    }

    @Cacheable("suppliersAll")
    public List<SupplierDTO> getAllSuppliers() {
        LoggerUtil.logDatabaseOperation(logger, "SELECT_ALL", "SUPPLIER");

        return repository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public Page<SupplierDTO> getAllSuppliers(Pageable pageable) {
        LoggerUtil.logDatabaseOperation(logger, "SELECT_PAGINATED", "SUPPLIER", "page", pageable.getPageNumber(),
                "size", pageable.getPageSize());

        return repository.findAll(pageable)
                .map(this::toDTO);
    }

    @CacheEvict(value = { "suppliersAll" }, allEntries = true)
    public SupplierDTO updateSupplier(Long id, SupplierDTO dto) {
        LoggerUtil.logBusinessEntry(logger, "UPDATE_SUPPLIER", "id", id, "name", dto != null ? dto.getName() : "null");

        Supplier supplier = repository.findById(id)
                .orElseThrow(() -> {
                    LoggerUtil.logBusinessError(logger, "UPDATE_SUPPLIER", "Supplier not found", "id", id);
                    return new ResourceNotFoundException("Supplier not found with id: " + id);
                });

        supplier.setName(dto.getName());
        supplier.setContact(dto.getContact());
        supplier = repository.save(supplier);

        LoggerUtil.logBusinessSuccess(logger, "UPDATE_SUPPLIER", "id", supplier.getId(), "name", supplier.getName());
        LoggerUtil.logAudit("UPDATE", "SUPPLIER", "supplierId", supplier.getId(), "name", supplier.getName());

        return toDTO(supplier);
    }

    @Transactional
    @CacheEvict(value = { "suppliersAll" }, allEntries = true)
    public void deleteSupplier(Long id) {
        LoggerUtil.logBusinessEntry(logger, "DELETE_SUPPLIER", "id", id);

        Supplier supplier = repository.findById(id)
                .orElseThrow(() -> {
                    LoggerUtil.logBusinessError(logger, "DELETE_SUPPLIER", "Supplier not found", "id", id);
                    return new ResourceNotFoundException("Supplier not found with id: " + id);
                });

        // Prevent deletion if supplier has dependent transactions
        // (Assume supplierTransactionRepository is available via DI)
        if (supplierTransactionRepository.findBySupplier(supplier).size() > 0) {
            LoggerUtil.logBusinessError(logger, "DELETE_SUPPLIER", "Cannot delete - has transactions", "id", id);
            throw new IllegalArgumentException("Cannot delete supplier with existing transactions.");
        }

        repository.delete(supplier);

        LoggerUtil.logBusinessSuccess(logger, "DELETE_SUPPLIER", "id", id);
        LoggerUtil.logAudit("DELETE", "SUPPLIER", "supplierId", id);
    }

    private SupplierDTO toDTO(Supplier supplier) {
        SupplierDTO dto = new SupplierDTO(supplier.getId(), supplier.getName(), supplier.getContact());
        dto.setCode(supplier.getCode());
        return dto;
    }
}
