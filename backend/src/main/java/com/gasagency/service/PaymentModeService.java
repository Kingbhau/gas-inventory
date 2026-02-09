package com.gasagency.service;

import com.gasagency.dto.PaymentModeDTO;
import com.gasagency.entity.PaymentMode;
import com.gasagency.repository.PaymentModeRepository;
import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PaymentModeService {

    private final PaymentModeRepository repository;
    private final ModelMapper modelMapper;

    public PaymentModeService(PaymentModeRepository repository, ModelMapper modelMapper) {
        this.repository = repository;
        this.modelMapper = modelMapper;
    }

    public Page<PaymentModeDTO> getAllPaymentModes(Pageable pageable) {
        return repository.findAll(pageable)
                .map(mode -> modelMapper.map(mode, PaymentModeDTO.class));
    }

    @Cacheable("paymentModesActive")
    public List<PaymentModeDTO> getActivePaymentModes() {
        return repository.findByIsActiveTrue()
                .stream()
                .map(mode -> modelMapper.map(mode, PaymentModeDTO.class))
                .collect(Collectors.toList());
    }

    @Cacheable("paymentModeNames")
    public List<String> getActivePaymentModeNames() {
        return repository.findActiveNames();
    }

    public PaymentModeDTO getPaymentModeById(Long id) {
        PaymentMode mode = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment mode not found with id: " + id));
        return modelMapper.map(mode, PaymentModeDTO.class);
    }

    @CacheEvict(value = { "paymentModesActive", "paymentModeNames" }, allEntries = true)
    public PaymentModeDTO createPaymentMode(PaymentModeDTO dto) {
        // Check if payment mode with same name already exists
        if (repository.findByName(dto.getName()).isPresent()) {
            throw new RuntimeException("Payment mode with name '" + dto.getName() + "' already exists");
        }

        // Check if payment mode with same code already exists
        if (repository.findByCode(dto.getCode()).isPresent()) {
            throw new RuntimeException("Payment mode with code '" + dto.getCode() + "' already exists");
        }

        PaymentMode mode = modelMapper.map(dto, PaymentMode.class);
        mode.setIsActive(true);
        // Ensure isBankAccountRequired has a default value if null
        if (mode.getIsBankAccountRequired() == null) {
            mode.setIsBankAccountRequired(false);
        }

        PaymentMode saved = repository.save(mode);
        return modelMapper.map(saved, PaymentModeDTO.class);
    }

    @CacheEvict(value = { "paymentModesActive", "paymentModeNames" }, allEntries = true)
    public PaymentModeDTO updatePaymentMode(Long id, PaymentModeDTO dto) {
        PaymentMode mode = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment mode not found with id: " + id));

        // Check if new name is already taken by another payment mode
        if (!mode.getName().equals(dto.getName()) &&
                repository.findByName(dto.getName()).isPresent()) {
            throw new RuntimeException("Payment mode with name '" + dto.getName() + "' already exists");
        }

        // Check if new code is already taken by another payment mode
        if (!mode.getCode().equals(dto.getCode()) &&
                repository.findByCode(dto.getCode()).isPresent()) {
            throw new RuntimeException("Payment mode with code '" + dto.getCode() + "' already exists");
        }

        mode.setName(dto.getName());
        mode.setCode(dto.getCode());
        mode.setDescription(dto.getDescription());
        // Ensure isBankAccountRequired has a default value if null
        Boolean bankAccountRequired = dto.getIsBankAccountRequired();
        mode.setIsBankAccountRequired(bankAccountRequired != null ? bankAccountRequired : false);
        if (dto.getIsActive() != null) {
            mode.setIsActive(dto.getIsActive());
        }

        PaymentMode updated = repository.save(mode);
        return modelMapper.map(updated, PaymentModeDTO.class);
    }

    @CacheEvict(value = { "paymentModesActive", "paymentModeNames" }, allEntries = true)
    public void deletePaymentMode(Long id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("Payment mode not found with id: " + id);
        }
        repository.deleteById(id);
    }

    @CacheEvict(value = { "paymentModesActive", "paymentModeNames" }, allEntries = true)
    public PaymentModeDTO togglePaymentModeStatus(Long id, Boolean isActive) {
        PaymentMode mode = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment mode not found with id: " + id));

        mode.setIsActive(isActive);

        PaymentMode updated = repository.save(mode);
        return modelMapper.map(updated, PaymentModeDTO.class);
    }
}
