
package com.gasagency.service;

import com.gasagency.dto.BusinessInfoDto;
import com.gasagency.entity.BusinessInfo;
import com.gasagency.repository.BusinessInfoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BusinessInfoService {
    private final BusinessInfoRepository businessInfoRepository;

    @Autowired
    public BusinessInfoService(BusinessInfoRepository businessInfoRepository) {
        this.businessInfoRepository = businessInfoRepository;
    }
    // Removed stray closing brace

    public BusinessInfoDto getBusinessInfo() {
        List<BusinessInfo> all = businessInfoRepository.findAll();
        if (all.isEmpty()) {
            return new BusinessInfoDto();
        }
        return toDtoSafe(all.get(0));
    }

    public BusinessInfoDto getBusinessInfoById(Long id) {
        try {
            return businessInfoRepository.findById(id)
                    .map(this::toDtoSafe)
                    .orElse(new BusinessInfoDto());
        } catch (Exception e) {
            // Log error and return a default DTO
            e.printStackTrace();
            return new BusinessInfoDto();
        }
    }


    public BusinessInfoDto saveBusinessInfo(BusinessInfoDto dto) {
        List<BusinessInfo> all = businessInfoRepository.findAll();
        BusinessInfo entity = all.isEmpty() ? new BusinessInfo() : all.get(0);
        entity.setAgencyName(dto.getAgencyName());
        entity.setRegistrationNumber(dto.getRegistrationNumber());
        entity.setGstNumber(dto.getGstNumber());
        entity.setAddress(dto.getAddress());
        entity.setContactNumber(dto.getContactNumber());
        entity.setEmail(dto.getEmail());
        BusinessInfo saved = businessInfoRepository.save(entity);
        return toDtoSafe(saved);
    }

    private BusinessInfoDto toDtoSafe(BusinessInfo entity) {
        BusinessInfoDto dto = new BusinessInfoDto();
        if (entity == null)
            return dto;
        dto.setAgencyName(entity.getAgencyName() != null ? entity.getAgencyName() : "");
        dto.setRegistrationNumber(entity.getRegistrationNumber() != null ? entity.getRegistrationNumber() : "");
        dto.setGstNumber(entity.getGstNumber() != null ? entity.getGstNumber() : "");
        dto.setAddress(entity.getAddress() != null ? entity.getAddress() : "");
        dto.setContactNumber(entity.getContactNumber() != null ? entity.getContactNumber() : "");
        dto.setEmail(entity.getEmail() != null ? entity.getEmail() : "");
        dto.setId(entity.getId() != null ? entity.getId() : null);
        return dto;
    }

}


